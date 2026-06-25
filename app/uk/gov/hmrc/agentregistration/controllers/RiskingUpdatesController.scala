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
import uk.gov.hmrc.agentregistration.repository.AgentApplicationRepo
import uk.gov.hmrc.agentregistration.repository.providedetails.llp.IndividualProvidedDetailsRepo
import uk.gov.hmrc.agentregistration.services.RiskingOutcomeEntityHelper.*
import uk.gov.hmrc.agentregistration.services.RiskingOutcomeIndividualHelper.*
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.ApplicationReference
import uk.gov.hmrc.agentregistration.shared.ApplicationState
import uk.gov.hmrc.agentregistration.shared.PersonReference
import uk.gov.hmrc.agentregistration.shared.risking.RiskingOutcomeApplication
import uk.gov.hmrc.agentregistration.shared.risking.RiskingOutcomeEntity
import uk.gov.hmrc.agentregistration.shared.risking.RiskingOutcomeIndividual
import uk.gov.hmrc.agentregistration.shared.risking.RiskingOutcomeRequest
import uk.gov.hmrc.agentregistration.util.ProcessInSequence
import uk.gov.hmrc.http.InternalServerException

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@Singleton()
class RiskingUpdatesController @Inject() (
  cc: ControllerComponents,
  actions: Actions,
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
    val riskingOutcomeEntity: RiskingOutcomeEntity = riskingOutcomeRequest.entityFailures.riskingOutcomeEntity
    val individualOutcomes: Seq[(PersonReference, RiskingOutcomeIndividual)] = riskingOutcomeRequest.individualFailures
      .map(individualFailures => individualFailures.personReference -> individualFailures.failures.riskingOutcomeIndividual)
    val riskingOutcomeApplication: RiskingOutcomeApplication = RiskingOutcomeApplication(
      riskingCompletedDate = riskingOutcomeRequest.riskingCompletedDate,
      outcome = riskingOutcomeRequest.applicationOutcome,
      correctiveActionExpiryDate = riskingOutcomeRequest.correctiveActionExpiryDate
    )
    for
      _ <- agentApplicationRepo.upsert(
        agentApplication
          .modify(_.riskingOutcomeApplication).setTo(Some(riskingOutcomeApplication))
          .modify(_.riskingOutcomeEntity).setTo(Some(riskingOutcomeEntity))
          .modify(_.applicationState).setTo(ApplicationState.RiskingCompleted)
      )
      _ <- ProcessInSequence.processInSequence(individualOutcomes)(applyIndividualOutcome)
    yield ()

  private def applyIndividualOutcome(
    personReferenceAndOutcome: (PersonReference, RiskingOutcomeIndividual)
  )(using RequestHeader): Future[Unit] =
    val (personReference, riskingOutcomeIndividual) = personReferenceAndOutcome
    individualProvidedDetailsRepo
      .findByPersonReference(personReference)
      .flatMap:
        case None =>
          val message = s"IndividualProvidedDetails not found for personReference [${personReference.value}]"
          logger.warn(message)
          Future.failed(InternalServerException(message))
        case Some(individualProvidedDetails) =>
          individualProvidedDetailsRepo.upsert(
            individualProvidedDetails
              .modify(_.riskingOutcomeIndividual).setTo(Some(riskingOutcomeIndividual))
          )
