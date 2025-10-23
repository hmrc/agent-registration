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
import uk.gov.hmrc.agentregistration.shared.AgentApplicationLlp
import uk.gov.hmrc.agentregistration.shared.ApplicationState
import uk.gov.hmrc.agentregistration.shared.BusinessType.Partnership
import uk.gov.hmrc.agentregistration.shared.CompanyProfile
import uk.gov.hmrc.agentregistration.shared.Crn
import uk.gov.hmrc.agentregistration.shared.EmailAddress
import uk.gov.hmrc.agentregistration.shared.GroupId
import uk.gov.hmrc.agentregistration.shared.InternalUserId
import uk.gov.hmrc.agentregistration.shared.LinkId
import uk.gov.hmrc.agentregistration.shared.BusinessDetailsLlp
import uk.gov.hmrc.agentregistration.shared.SafeId
import uk.gov.hmrc.agentregistration.shared.TelephoneNumber
import uk.gov.hmrc.agentregistration.shared.SaUtr
import uk.gov.hmrc.agentregistration.shared.contactdetails.ApplicantContactDetails
import uk.gov.hmrc.agentregistration.shared.contactdetails.ApplicantEmailAddress
import uk.gov.hmrc.agentregistration.shared.contactdetails.ApplicantName
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext

@Singleton()
class TestApplicationController @Inject() (
  cc: ControllerComponents,
  actions: Actions,
  agentApplicationRepo: AgentApplicationRepo
)
extends BackendController(cc):

  given ExecutionContext = controllerComponents.executionContext

  def findApplicationByLinkId(linkId: String): Action[AnyContent] = Action.async: request =>
    agentApplicationRepo
      .findByLinkId(LinkId(linkId))
      .map:
        case Some(agentApplication) => Ok(Json.toJson(agentApplication))
        case None => NoContent

  def createTestApplication: Action[AnyContent] = Action
    .async:
      implicit request =>
        val agentApplication: AgentApplication = makeSubmittedApplication()
        agentApplicationRepo
          .upsert(agentApplication)
          .map(_ => Ok(Json.obj("linkId" -> agentApplication.linkId.value)))

  private def makeSubmittedApplication(): AgentApplication = AgentApplicationLlp(
    linkId = LinkId(value = UUID.randomUUID().toString),
    internalUserId = InternalUserId(value = s"test-${UUID.randomUUID().toString}"),
    groupId = GroupId(value = UUID.randomUUID().toString),
    createdAt = Instant.now(),
    applicationState = ApplicationState.Submitted,
    businessDetails = Some(BusinessDetailsLlp(
      safeId = SafeId("safe-id-12345"),
      saUtr = SaUtr("1234567890"),
      companyProfile = CompanyProfile(
        companyNumber = Crn("12345566"),
        companyName = "Test Partnership LLP",
        dateOfIncorporation = None
      )
    )),
    applicantContactDetails = Some(ApplicantContactDetails(
      applicantName = ApplicantName.NameOfAuthorised(name = Some("Bob Ross")),
      telephoneNumber = Some(TelephoneNumber("1234658979")),
      applicantEmailAddress = Some(ApplicantEmailAddress(
        emailAddress = EmailAddress("user@test.com"),
        isVerified = true
      ))
    )),
    amlsDetails = None
  )
