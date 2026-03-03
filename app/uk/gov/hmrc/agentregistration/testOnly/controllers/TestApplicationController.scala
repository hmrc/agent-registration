/*
 * Copyright 2025 HM Revenue & Customs
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

package uk.gov.hmrc.agentregistration.testOnly.controllers

import play.api.libs.json.Json
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.ControllerComponents
import uk.gov.hmrc.agentregistration.action.Actions
import uk.gov.hmrc.agentregistration.repository.AgentApplicationRepo
import uk.gov.hmrc.agentregistration.repository.providedetails.llp.IndividualProvidedDetailsRepo
import uk.gov.hmrc.agentregistration.shared.*
import uk.gov.hmrc.agentregistration.shared.ApplicationState.Started
import uk.gov.hmrc.agentregistration.shared.CheckResult.Pass
import uk.gov.hmrc.agentregistration.shared.businessdetails.BusinessDetailsLlp
import uk.gov.hmrc.agentregistration.shared.businessdetails.CompanyProfile
import uk.gov.hmrc.agentregistration.shared.contactdetails.ApplicantContactDetails
import uk.gov.hmrc.agentregistration.shared.contactdetails.ApplicantEmailAddress
import uk.gov.hmrc.agentregistration.shared.contactdetails.ApplicantName
import uk.gov.hmrc.agentregistration.shared.individual.*
import uk.gov.hmrc.agentregistration.shared.lists.IndividualName
import uk.gov.hmrc.agentregistration.shared.util.Errors.getOrThrowExpectedDataMissing
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext

@Singleton()
class TestApplicationController @Inject() (
  cc: ControllerComponents,
  actions: Actions,
  agentApplicationRepo: AgentApplicationRepo,
  agentApplicationIdGenerator: AgentApplicationIdGenerator,
  individualProvidedDetailsRepo: IndividualProvidedDetailsRepo,
  individualProvidedDetailsIdGenerator: IndividualProvidedDetailsIdGenerator
)
extends BackendController(cc):

  given ExecutionContext = controllerComponents.executionContext

  def createTestSmuIndividual: Action[AnyContent] = Action
    .async:
      implicit request =>
        val agentApplication: AgentApplication = makeApplicationToProvideDetailsFor(applicationState = ApplicationState.SentForRisking)
        val individual: IndividualProvidedDetails = makeIndividualProvidedDetailsFor(agentApplication.agentApplicationId)
        for
          _ <- agentApplicationRepo.upsert(agentApplication)
          _ <- individualProvidedDetailsRepo.upsert(individual)
        yield Created(Json.obj(
          "individualProvidedDetailsId" -> individual.individualProvidedDetailsId.value
        ))

  def deleteTestSmuIndividual(individualId: IndividualProvidedDetailsId): Action[AnyContent] = Action
    .async:
      implicit request =>
        for
          maybeIndividual <- individualProvidedDetailsRepo.findById(individualId)
          applicationId = maybeIndividual.map(_.agentApplicationId).getOrThrowExpectedDataMissing("agentApplicationId")
          maybeAaRes <- agentApplicationRepo.removeById(applicationId)
          maybeIpdRes <- individualProvidedDetailsRepo.removeById(individualId)
        yield (maybeAaRes, maybeIpdRes) match
          case (Some(aaRes), Some(ipdRes)) if aaRes.wasAcknowledged() && ipdRes.wasAcknowledged() => NoContent
          case _ => BadRequest("Something went wrong")

  def createTestApplication: Action[AnyContent] = Action
    .async:
      implicit request =>
        val agentApplication: AgentApplication = makeApplicationToProvideDetailsFor()
        agentApplicationRepo
          .upsert(agentApplication)
          .map(_ => Ok(Json.obj("linkId" -> agentApplication.linkId.value)))

  // TODO: We should revisit the way that we handle the stubbing here after we have brought test data into the shared space
  private def makeApplicationToProvideDetailsFor(applicationState: ApplicationState = Started): AgentApplication = AgentApplicationLlp(
    _id = agentApplicationIdGenerator.nextApplicationId(),
    linkId = LinkId(value = UUID.randomUUID().toString),
    internalUserId = InternalUserId(value = s"test-${UUID.randomUUID().toString}"),
    groupId = GroupId(value = UUID.randomUUID().toString),
    createdAt = Instant.now(),
    applicationState = applicationState, // Provide details journeys now happen before an application is finished
    userRole = Some(UserRole.Authorised),
    businessDetails = Some(BusinessDetailsLlp(
      safeId = SafeId("safe-id-12345"),
      saUtr = SaUtr("1234567890"),
      companyProfile = CompanyProfile(
        companyNumber = Crn("12345566"),
        companyName = "Test Partnership LLP",
        dateOfIncorporation = None,
        unsanitisedCHROAddress = None
      )
    )),
    applicantContactDetails = Some(ApplicantContactDetails(
      applicantName = ApplicantName("Bob Ross"),
      telephoneNumber = Some(TelephoneNumber("1234658979")),
      applicantEmailAddress = Some(ApplicantEmailAddress(
        emailAddress = EmailAddress("user@test.com"),
        isVerified = true
      ))
    )),
    amlsDetails = None,
    agentDetails = None,
    hmrcStandardForAgentsAgreed = StateOfAgreement.Agreed,
    numberOfRequiredKeyIndividuals = None,
    hasOtherRelevantIndividuals = None,
    refusalToDealWithCheckResult = Some(Pass),
    companyStatusCheckResult = Some(Pass),
    vrns = Some(List(Vrn("12341234"), Vrn("43214321"))),
    payeRefs = Some(List(PayeRef("56785678"), PayeRef("87658765")))
  )
  // TODO: Same as makeApplicationToProvideDetailsFor, we should use test data here or create FF links to populate this data
  private def makeIndividualProvidedDetailsFor(agentApplicationId: AgentApplicationId): IndividualProvidedDetails = IndividualProvidedDetails(
    _id = individualProvidedDetailsIdGenerator.nextIndividualProvidedDetailsId(),
    individualName = IndividualName("George Smiley"),
    isPersonOfControl = true,
    internalUserId = Some(InternalUserId(value = s"test-${UUID.randomUUID().toString}")),
    createdAt = Instant.now(),
    providedDetailsState = ProvidedDetailsState.Finished,
    agentApplicationId = agentApplicationId,
    individualDateOfBirth = Some(IndividualDateOfBirth.Provided(LocalDate.of(1980, 1, 1))),
    telephoneNumber = Some(TelephoneNumber("1234658979")),
    emailAddress = Some(IndividualVerifiedEmailAddress(EmailAddress("g.smiley@test.com"), isVerified = true)),
    individualNino = Some(IndividualNino.Provided(Nino("AA123456A"))),
    individualSaUtr = Some(IndividualSaUtr.Provided(SaUtr("1234567890"))),
    hmrcStandardForAgentsAgreed = StateOfAgreement.Agreed,
    hasApprovedApplication = Some(true),
    vrns = Some(List(Vrn("12341234"), Vrn("43214321"))),
    payeRefs = Some(List(PayeRef("56785678"), PayeRef("87658765")))
  )
