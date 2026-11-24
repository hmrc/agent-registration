/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.agentregistration.services.email

import play.api.mvc.RequestHeader
import uk.gov.hmrc.agentregistration.connectors.email.EmailTemplateId
import uk.gov.hmrc.agentregistration.connectors.email.SendEmailRequest
import uk.gov.hmrc.agentregistration.repository.AgentApplicationRepo
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.BusinessType
import uk.gov.hmrc.agentregistration.shared.EmailAddress
import uk.gov.hmrc.agentregistration.shared.testdata.agentapplication.ExpiryTransformations.*
import uk.gov.hmrc.agentregistration.testsupport.ISpec
import uk.gov.hmrc.agentregistration.testsupport.testdata.AgentApplicationStates
import uk.gov.hmrc.agentregistration.testsupport.wiremock.stubs.EmailStubs

class EmailServiceForDeletedApplicationsSpec
extends ISpec:

  private lazy val emailServiceForDeletedApplications: EmailServiceForDeletedApplications = app.injector.instanceOf[EmailServiceForDeletedApplications]
  private lazy val agentApplicationRepo: AgentApplicationRepo = app.injector.instanceOf[AgentApplicationRepo]

  private given RequestHeader = tdAll.fakeBackendRequest

  private def expectedEmail(
    agentApplication: AgentApplication,
    emailAddress: EmailAddress = tdAll.applicantEmailAddress
  ): SendEmailRequest = SendEmailRequest(
    to = Seq(emailAddress),
    templateId = EmailTemplateId.ApplicationDeleted,
    parameters = Map(
      "agentName" -> tdAll.applicantName.value,
      "applicationRef" -> agentApplication.applicationReference.value
    )
  )

  private final case class TestCase(
    description: String,
    agentApplication: AgentApplication,
    emailSendSuccessfully: Boolean,
    expectedSendEmailRequests: Int,
    expectedAgentApplicationAfterRun: AgentApplication
  )

  BusinessType.values.foreach: businessType =>
    s"for a $businessType application" - {

      val agentApplication: AgentApplicationStates = AgentApplicationStates.forBusinessType(businessType)

      Seq(
        TestCase(
          description = "sends the deletion email to the verified applicant email and records it as sent",
          agentApplication = agentApplication.withVerifiedEmail.makeOverdue.makeExpiredEmailSent.makeDueForDeletion,
          emailSendSuccessfully = true,
          expectedSendEmailRequests = 1,
          expectedAgentApplicationAfterRun = agentApplication.withVerifiedEmail.makeOverdue.makeExpiredEmailSent.makeDeletionEmailSent
        ),
        TestCase(
          description = "does not send the deletion email to an application already sent it",
          agentApplication = agentApplication.withVerifiedEmail.makeOverdue.makeExpiredEmailSent.makeDeletionEmailSent,
          emailSendSuccessfully = true,
          expectedSendEmailRequests = 0,
          expectedAgentApplicationAfterRun = agentApplication.withVerifiedEmail.makeOverdue.makeExpiredEmailSent.makeDeletionEmailSent
        ),
        TestCase(
          description = "does not send the deletion email to an application with no verified email address",
          agentApplication = agentApplication.started.makeOverdue.makeExpired.makeDueForDeletion,
          emailSendSuccessfully = true,
          expectedSendEmailRequests = 0,
          expectedAgentApplicationAfterRun = agentApplication.started.makeOverdue.makeExpired.makeDueForDeletion
        ),
        TestCase(
          description = "does not send the deletion email to an application still in the grace period",
          agentApplication = agentApplication.withVerifiedEmail.makeOverdue.makeExpiredEmailSent,
          emailSendSuccessfully = true,
          expectedSendEmailRequests = 0,
          expectedAgentApplicationAfterRun = agentApplication.withVerifiedEmail.makeOverdue.makeExpiredEmailSent
        ),
        TestCase(
          description = "does not record the deletion email as sent when the email service fails, so it is retried on the next run",
          agentApplication = agentApplication.withVerifiedEmail.makeOverdue.makeExpiredEmailSent.makeDueForDeletion,
          emailSendSuccessfully = false,
          expectedSendEmailRequests = 1,
          expectedAgentApplicationAfterRun = agentApplication.withVerifiedEmail.makeOverdue.makeExpiredEmailSent.makeDueForDeletion
        )
      ).foreach: testCase =>
        s"processEmails ${testCase.description}" in:
          if testCase.emailSendSuccessfully then EmailStubs.stubSendEmail(expectedEmail(testCase.agentApplication))
          else EmailStubs.stubSendEmailFailure(expectedEmail(testCase.agentApplication))
          agentApplicationRepo.upsert(testCase.agentApplication).futureValue

          emailServiceForDeletedApplications.processEmails().futureValue

          EmailStubs.verifySendEmail(count = testCase.expectedSendEmailRequests)
          agentApplicationRepo.findById(testCase.agentApplication.agentApplicationId).futureValue.value shouldBe testCase.expectedAgentApplicationAfterRun
    }

  "processEmails sends the deletion email to the verified agent email when agent details are complete" in:
    val dueForDeletion: AgentApplication =
      tdAll.agentApplicationLlp.afterAgentDetailsCompleteWithOtherAgentEmail.makeOverdue.makeExpiredEmailSent.makeDueForDeletion
    val deletionEmailSent: AgentApplication =
      tdAll.agentApplicationLlp.afterAgentDetailsCompleteWithOtherAgentEmail.makeOverdue.makeExpiredEmailSent.makeDeletionEmailSent
    EmailStubs.stubSendEmail(expectedEmail(dueForDeletion, tdAll.otherAgentEmailAddress))
    agentApplicationRepo.upsert(dueForDeletion).futureValue

    emailServiceForDeletedApplications.processEmails().futureValue

    EmailStubs.verifySendEmail(count = 1)
    agentApplicationRepo.findById(dueForDeletion.agentApplicationId).futureValue.value shouldBe deletionEmailSent
