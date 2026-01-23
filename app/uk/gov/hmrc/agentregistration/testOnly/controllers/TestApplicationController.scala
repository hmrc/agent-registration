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
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.AgentApplicationIdGenerator
import uk.gov.hmrc.agentregistration.shared.AgentApplicationLlp
import uk.gov.hmrc.agentregistration.shared.AgentApplicationSoleTrader
import uk.gov.hmrc.agentregistration.shared.ApplicationState
import uk.gov.hmrc.agentregistration.shared.CheckResult
import uk.gov.hmrc.agentregistration.shared.CheckResult.Pass
import uk.gov.hmrc.agentregistration.shared.Crn
import uk.gov.hmrc.agentregistration.shared.EmailAddress
import uk.gov.hmrc.agentregistration.shared.GroupId
import uk.gov.hmrc.agentregistration.shared.InternalUserId
import uk.gov.hmrc.agentregistration.shared.LinkId
import uk.gov.hmrc.agentregistration.shared.Nino
import uk.gov.hmrc.agentregistration.shared.SaUtr
import uk.gov.hmrc.agentregistration.shared.SafeId
import uk.gov.hmrc.agentregistration.shared.StateOfAgreement
import uk.gov.hmrc.agentregistration.shared.StateOfAgreement
import uk.gov.hmrc.agentregistration.shared.TelephoneNumber
import uk.gov.hmrc.agentregistration.shared.UserRole
import uk.gov.hmrc.agentregistration.shared.businessdetails.BusinessDetailsLlp
import uk.gov.hmrc.agentregistration.shared.businessdetails.BusinessDetailsSoleTrader
import uk.gov.hmrc.agentregistration.shared.businessdetails.CompanyProfile
import uk.gov.hmrc.agentregistration.shared.businessdetails.FullName
import uk.gov.hmrc.agentregistration.shared.contactdetails.ApplicantContactDetails
import uk.gov.hmrc.agentregistration.shared.contactdetails.ApplicantEmailAddress
import uk.gov.hmrc.agentregistration.shared.contactdetails.ApplicantName
import uk.gov.hmrc.agentregistration.shared.lists.FromFiveOrFewer
import uk.gov.hmrc.agentregistration.shared.lists.KeyIndividualListSource.FromApplicant
import uk.gov.hmrc.agentregistration.shared.lists.KeyIndividualListSource.FromBusinessTypeSoleTrader
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
  agentApplicationIdGenerator: AgentApplicationIdGenerator
)
extends BackendController(cc):

  given ExecutionContext = controllerComponents.executionContext

  def createTestApplication: Action[AnyContent] = Action
    .async:
      implicit request =>
        val agentApplication: AgentApplication = makeSubmittedApplication()
        agentApplicationRepo
          .upsert(agentApplication)
          .map(_ => Ok(Json.obj("linkId" -> agentApplication.linkId.value)))

  // primarily added to illustrate sole trader modelling of required key individuals
  def createSoleTraderTestApplication: Action[AnyContent] = Action
    .async:
      implicit request =>
        val agentApplication: AgentApplication = makeSubmittedSoleTraderApplication()
        agentApplicationRepo
          .upsert(agentApplication)
          .map(_ => Ok(Json.obj("linkId" -> agentApplication.linkId.value)))

  private def makeSubmittedApplication(): AgentApplication = AgentApplicationLlp(
    _id = agentApplicationIdGenerator.nextApplicationId(),
    linkId = LinkId(value = UUID.randomUUID().toString),
    internalUserId = InternalUserId(value = s"test-${UUID.randomUUID().toString}"),
    groupId = GroupId(value = UUID.randomUUID().toString),
    createdAt = Instant.now(),
    applicationState = ApplicationState.Submitted,
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
    requiredKeyIndividuals = Some(
      FromFiveOrFewer(
        numberToProvideDetails = 3,
        source = FromApplicant
      )
    ),
    refusalToDealWithCheckResult = Some(Pass),
    companyStatusCheckResult = Some(Pass)
  )

  private def makeSubmittedSoleTraderApplication(): AgentApplication = AgentApplicationSoleTrader(
    _id = agentApplicationIdGenerator.nextApplicationId(),
    linkId = LinkId(value = UUID.randomUUID().toString),
    internalUserId = InternalUserId(value = s"test-${UUID.randomUUID().toString}"),
    groupId = GroupId(value = UUID.randomUUID().toString),
    createdAt = Instant.now(),
    applicationState = ApplicationState.Submitted,
    userRole = Some(UserRole.Authorised),
    businessDetails = Some(BusinessDetailsSoleTrader(
      safeId = SafeId("safe-id-12345"),
      saUtr = SaUtr("1234567890"),
      fullName = FullName("Bob", "Smith"),
      dateOfBirth = LocalDate.of(1980, 1, 1),
      nino = Some(Nino("AB123456C")),
      trn = None
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
    requiredKeyIndividuals = Some(
      FromFiveOrFewer(
        numberToProvideDetails = 1, // these amounts are not entered by the user, they are inferred from business type
        source = FromBusinessTypeSoleTrader
      )
    ),
    refusalToDealWithCheckResult = Some(Pass),
    deceasedCheckResult = Some(Pass)
  )
