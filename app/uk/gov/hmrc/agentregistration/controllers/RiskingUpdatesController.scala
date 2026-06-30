/*
 * Copyright 2026 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.agentregistration.controllers

import com.softwaremill.quicklens.*
import play.api.mvc.Action
import play.api.mvc.ControllerComponents
import play.api.mvc.RequestHeader
import uk.gov.hmrc.agentregistration.action.Actions
import uk.gov.hmrc.agentregistration.config.AppConfig
import uk.gov.hmrc.agentregistration.repository.AgentApplicationRepo
import uk.gov.hmrc.agentregistration.repository.providedetails.llp.IndividualProvidedDetailsRepo
import uk.gov.hmrc.agentregistration.services.RiskingOutcomeApplicationHelper
import uk.gov.hmrc.agentregistration.services.RiskingOutcomeEntityHelper
import uk.gov.hmrc.agentregistration.services.RiskingOutcomeIndividualHelper
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.ApplicationReference
import uk.gov.hmrc.agentregistration.shared.ApplicationState
import uk.gov.hmrc.agentregistration.shared.PersonReference
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetails
import uk.gov.hmrc.agentregistration.shared.risking.RiskingOutcomeApplication
import uk.gov.hmrc.agentregistration.shared.risking.RiskingOutcomeEntity
import uk.gov.hmrc.agentregistration.shared.risking.RiskingOutcomeIndividual
import uk.gov.hmrc.agentregistration.shared.risking.RiskingOutcomeRequest
import uk.gov.hmrc.agentregistration.shared.util.Errors.getOrThrowExpectedDataMissing
import uk.gov.hmrc.agentregistration.util.ProcessInSequence

import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@Singleton()
class RiskingUpdatesController @Inject() (
  cc: ControllerComponents,
  actions: Actions,
  appConfig: AppConfig,
  agentApplicationRepo: AgentApplicationRepo,
  individualProvidedDetailsRepo: IndividualProvidedDetailsRepo
)(using ExecutionContext)
extends BackendController(cc):

  def receiveRiskingOutcome(applicationReference: ApplicationReference): Action[RiskingOutcomeRequest] =
    actions
      .default
      .async(parse.json[RiskingOutcomeRequest]):
        implicit request =>
          logger.info(s"Received risking outcome for applicationReference [${applicationReference.value}]")
          agentApplicationRepo
            .findByApplicationReference(applicationReference)
            .flatMap:
              case None =>
                val message = s"Agent application not found for applicationReference [${applicationReference.value}]"
                logger.warn(message)
                Future.successful(NotFound(message))
              case Some(agentApplication) =>
                markRiskingCompleted(agentApplication, request.body)
                  .map(_ => Ok(""))

  private def markRiskingCompleted(
    agentApplication: AgentApplication,
    riskingOutcomeRequest: RiskingOutcomeRequest
  )(using RequestHeader): Future[Unit] =
    val riskingOutcomeEntity: RiskingOutcomeEntity = RiskingOutcomeEntityHelper.riskingOutcomeEntity(
      riskingOutcomeRequest.entityOutcome,
      riskingOutcomeRequest.entityFailures
    )
    val individualOutcomes: Seq[(PersonReference, RiskingOutcomeIndividual)] = riskingOutcomeRequest.individualFailures
      .map(individualFailures =>
        individualFailures.personReference -> RiskingOutcomeIndividualHelper.riskingOutcomeIndividual(
          individualFailures.riskingOutcome,
          individualFailures.failures
        )
      )
    val outcome: RiskingOutcomeApplication.Outcome = RiskingOutcomeApplicationHelper.riskingOutcomeApplicationOutcome(riskingOutcomeRequest.applicationOutcome)
    val riskingOutcomeApplication: RiskingOutcomeApplication = RiskingOutcomeApplication(
      riskingCompletedDate = riskingOutcomeRequest.riskingCompletedDate,
      outcome = outcome,
      correctiveActionExpiryDate = correctiveActionExpiryDateFor(outcome, riskingOutcomeRequest.riskingCompletedDate)
    )
    for

      updatedIndividualProvidedDetailsList <- resolveIndividualOutcomes(individualOutcomes)
      _ <- agentApplicationRepo.upsert(
        agentApplication
          .modify(_.riskingOutcomeApplication).setTo(Some(riskingOutcomeApplication))
          .modify(_.riskingOutcomeEntity).setTo(Some(riskingOutcomeEntity))
          .modify(_.applicationState).setTo(ApplicationState.RiskingCompleted)
      )
      _ <- ProcessInSequence.processInSequence(updatedIndividualProvidedDetailsList)(individualProvidedDetailsRepo.upsert)
    yield ()

  private def correctiveActionExpiryDateFor(
    outcome: RiskingOutcomeApplication.Outcome,
    riskingCompletedDate: LocalDate
  ): Option[LocalDate] =
    outcome match
      case RiskingOutcomeApplication.Outcome.Approved => None
      case RiskingOutcomeApplication.Outcome.FailedFixable | RiskingOutcomeApplication.Outcome.FailedNonFixable =>
        Some(riskingCompletedDate.plusDays(appConfig.CorrectiveAction.daysToTakeCorrectiveAction.toLong))

  private def resolveIndividualOutcomes(
    individualOutcomes: Seq[(PersonReference, RiskingOutcomeIndividual)]
  )(using RequestHeader): Future[Seq[IndividualProvidedDetails]] =
    individualOutcomes.foldLeft(Future.successful(Seq.empty[IndividualProvidedDetails])):
      case (accumulator, (personReference, riskingOutcomeIndividual)) =>
        for
          individualProvidedDetailsList: Seq[IndividualProvidedDetails] <- accumulator
          individualProvidedDetails: IndividualProvidedDetails <- individualProvidedDetailsRepo
            .findByPersonReference(personReference)
            .map(_.getOrThrowExpectedDataMissing(s"IndividualProvidedDetails for personReference [${personReference.value}]"))
        yield individualProvidedDetailsList :+ individualProvidedDetails.modify(_.riskingOutcomeIndividual).setTo(Some(riskingOutcomeIndividual))
