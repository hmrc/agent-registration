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
import play.api.libs.json.Json
import play.api.libs.json.OFormat
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.ControllerComponents
import play.api.mvc.RequestHeader
import uk.gov.hmrc.agentregistration.action.Actions
import uk.gov.hmrc.agentregistration.config.AppConfig
import uk.gov.hmrc.agentregistration.repository.AgentApplicationRepo
import uk.gov.hmrc.agentregistration.repository.providedetails.llp.IndividualProvidedDetailsRepo
import uk.gov.hmrc.agentregistration.services.RiskingOutcomeEntityHelper
import uk.gov.hmrc.agentregistration.services.RiskingOutcomeIndividualHelper
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.ApplicationReference
import uk.gov.hmrc.agentregistration.shared.ApplicationState
import uk.gov.hmrc.agentregistration.shared.ApplicationState.SentToMinerva
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetails
import uk.gov.hmrc.agentregistration.shared.risking.*
import uk.gov.hmrc.agentregistration.shared.risking.updates.UpdateApplicationStateSentToMinervaRequest
import uk.gov.hmrc.agentregistration.shared.util.Errors.getOrThrowExpectedDataMissing
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.=!=
import uk.gov.hmrc.agentregistration.util.ProcessInSequence
import uk.gov.hmrc.agentregistration.shared.ApplicationState.SentToMinerva
import uk.gov.hmrc.agentregistration.shared.risking.updates.UpdateApplicationStateSentToMinervaRequest
import uk.gov.hmrc.internalauth.client.*

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
  individualProvidedDetailsRepo: IndividualProvidedDetailsRepo,
  authComponents: BackendAuthComponents
)(using ExecutionContext)
extends BackendController(cc):

  private val riskingUpdatesWritePermission = Predicate.Permission(
    resource = Resource(
      resourceType = ResourceType("agent-registration"),
      resourceLocation = ResourceLocation("risking-updates")
    ),
    action = IAAction("WRITE")
  )

  private val baseAction =
    if appConfig.InternalAuth.isEnabled
    then authComponents.authorizedAction(riskingUpdatesWritePermission)
    else actions.default

  def receiveRiskingOutcome(applicationReference: ApplicationReference): Action[RiskingOutcomeRequest] =
    baseAction
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
                if agentApplication.applicationState =!= ApplicationState.SentForRisking then
                  logger.warn(
                    s"receiveRiskingOutcome for applicationReference [${applicationReference.value}] in unexpected state [${agentApplication.applicationState}] "
                  )
                markRiskingCompleted(agentApplication, request.body).map(_ => Ok(""))

  def updateApplicationStatusSentToMinerva: Action[UpdateApplicationStateSentToMinervaRequest] =
    baseAction
      .async(parse.json[UpdateApplicationStateSentToMinervaRequest]):
        implicit request =>
          val applicationReferences = request.body.applicationReferences
          agentApplicationRepo
            .updateManyApplicationStateByReference(applicationReferences = applicationReferences, applicationState = SentToMinerva)
            .map(_ => Ok)

  private def markRiskingCompleted(
    agentApplication: AgentApplication,
    riskingOutcomeRequest: RiskingOutcomeRequest
  )(using RequestHeader): Future[Unit] =
    val riskingOutcomeEntity: RiskingOutcomeEntity = RiskingOutcomeEntityHelper.riskingOutcomeEntity(
      riskingOutcomeRequest.entityOutcome,
      riskingOutcomeRequest.entityFailures,
      existingAmlsDetails = agentApplication.amlsDetails
    )

    val riskingOutcomeApplication: RiskingOutcomeApplication = makeRiskingOutcomeApplicationOutcome(riskingOutcomeRequest)

    for
      updatedIndividualProvidedDetailsList <- resolveIndividualOutcomes(agentApplication, riskingOutcomeRequest.individualFailures)
      _ <- ProcessInSequence.processInSequence(updatedIndividualProvidedDetailsList)(individualProvidedDetailsRepo.upsert)
      _ <- agentApplicationRepo.upsert(
        agentApplication
          .modify(_.riskingOutcomeApplication).setTo(Some(riskingOutcomeApplication))
          .modify(_.riskingOutcomeEntity).setTo(Some(riskingOutcomeEntity))
          .modify(_.applicationState).setTo(ApplicationState.RiskingCompleted)
      )
    yield ()

  private def resolveIndividualOutcomes(
    agentApplication: AgentApplication,
    individualFailuresList: Seq[IndividualFailures]
  )(using RequestHeader): Future[Seq[IndividualProvidedDetails]] =
    individualFailuresList.foldLeft(Future.successful(Seq.empty[IndividualProvidedDetails])):
      case (accumulator, individualFailures) =>
        for
          individualProvidedDetailsList: Seq[IndividualProvidedDetails] <- accumulator
          individualProvidedDetails: IndividualProvidedDetails <- individualProvidedDetailsRepo
            .findByPersonReferenceAndAgentApplicationId(individualFailures.personReference, agentApplication._id)
            .map(_.getOrThrowExpectedDataMissing(s"IndividualProvidedDetails for personReference [${individualFailures.personReference.value}]"))
          riskingOutcomeIndividual: RiskingOutcomeIndividual = RiskingOutcomeIndividualHelper.riskingOutcomeIndividual(
            individualFailures.riskingOutcome,
            individualFailures.failures,
            individualProvidedDetails = individualProvidedDetails
          )
        yield individualProvidedDetailsList :+ individualProvidedDetails.modify(_.riskingOutcomeIndividual).setTo(Some(riskingOutcomeIndividual))

  private def makeRiskingOutcomeApplicationOutcome(riskingOutcomeRequest: RiskingOutcomeRequest): RiskingOutcomeApplication =
    val actualDecisionDate: LocalDate =
      riskingOutcomeRequest
        .emailsSentAt
        .atZone(AppConfig.zoneId)
        .toLocalDate

    riskingOutcomeRequest.applicationOutcome match
      case RiskingOutcome.Approved => RiskingOutcomeApplication.Approved(actualDecisionDate)
      case RiskingOutcome.FailedFixable =>
        RiskingOutcomeApplication.FailedFixable(
          actualDecisionDate = actualDecisionDate,
          correctiveActionExpiryDate = actualDecisionDate.plusDays(appConfig.CorrectiveAction.daysToTakeCorrectiveAction.toLong),
          reSubmittedAt = None
        )
      case RiskingOutcome.FailedNonFixable =>
        RiskingOutcomeApplication.FailedNonFixable(
          actualDecisionDate = actualDecisionDate,
          correctiveActionExpiryDate = actualDecisionDate.plusDays(appConfig.CorrectiveAction.daysToTakeCorrectiveAction.toLong)
        )
