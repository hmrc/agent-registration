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

class EmailServiceForExpiredApplicationsSpec
extends ISpec:

  private lazy val emailServiceForExpiredApplications: EmailServiceForExpiredApplications = app.injector.instanceOf[EmailServiceForExpiredApplications]
  private lazy val agentApplicationRepo: AgentApplicationRepo = app.injector.instanceOf[AgentApplicationRepo]

  private given RequestHeader = tdAll.fakeBackendRequest

  private def expectedEmail(
    agentApplication: AgentApplication,
    emailAddress: EmailAddress = tdAll.applicantEmailAddress
  ): SendEmailRequest = SendEmailRequest(
    to = Seq(emailAddress),
    templateId = EmailTemplateId.ApplicationExpired,
    parameters = Map(
      "agentName" -> tdAll.applicantName.value,
      "applicationRef" -> agentApplication.applicationReference.value,
      "deletionDate" -> "9 January 2060"
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

      val agentApplicationStates: AgentApplicationStates = AgentApplicationStates.forBusinessType(businessType)

      Seq(
        TestCase(
          description = "sends the expired email to the verified applicant email and records it as sent",
          agentApplication = agentApplicationStates.withVerifiedEmail.makeOverdue.makeExpired,
          emailSendSuccessfully = true,
          expectedSendEmailRequests = 1,
          expectedAgentApplicationAfterRun = agentApplicationStates.withVerifiedEmail.makeOverdue.makeExpiredEmailSent
        ),
        TestCase(
          description = "does not send the expired email to an application already sent it",
          agentApplication = agentApplicationStates.withVerifiedEmail.makeOverdue.makeExpiredEmailSent,
          emailSendSuccessfully = true,
          expectedSendEmailRequests = 0,
          expectedAgentApplicationAfterRun = agentApplicationStates.withVerifiedEmail.makeOverdue.makeExpiredEmailSent
        ),
        TestCase(
          description = "does not send the expired email to an application with no verified email address",
          agentApplication = agentApplicationStates.started.makeOverdue.makeExpired,
          emailSendSuccessfully = true,
          expectedSendEmailRequests = 0,
          expectedAgentApplicationAfterRun = agentApplicationStates.started.makeOverdue.makeExpired
        ),
        TestCase(
          description = "does not send the expired email to an application that has not expired",
          agentApplication = agentApplicationStates.withVerifiedEmail,
          emailSendSuccessfully = true,
          expectedSendEmailRequests = 0,
          expectedAgentApplicationAfterRun = agentApplicationStates.withVerifiedEmail
        ),
        TestCase(
          description = "does not send the expired email to an application whose grace period has ended",
          agentApplication = agentApplicationStates.withVerifiedEmail.makeOverdue.makeExpired.makeDueForDeletion,
          emailSendSuccessfully = true,
          expectedSendEmailRequests = 0,
          expectedAgentApplicationAfterRun = agentApplicationStates.withVerifiedEmail.makeOverdue.makeExpired.makeDueForDeletion
        ),
        TestCase(
          description = "does not record the expired email as sent when the email service fails, so it is retried on the next run",
          agentApplication = agentApplicationStates.withVerifiedEmail.makeOverdue.makeExpired,
          emailSendSuccessfully = false,
          expectedSendEmailRequests = 1,
          expectedAgentApplicationAfterRun = agentApplicationStates.withVerifiedEmail.makeOverdue.makeExpired
        )
      ).foreach: testCase =>
        s"processEmails ${testCase.description}" in:
          if testCase.emailSendSuccessfully
          then EmailStubs.stubSendEmail(expectedEmail(testCase.agentApplication))
          else EmailStubs.stubSendEmailFailure(expectedEmail(testCase.agentApplication))
          agentApplicationRepo.upsert(testCase.agentApplication).futureValue

          emailServiceForExpiredApplications.processEmails().futureValue

          EmailStubs.verifySendEmail(count = testCase.expectedSendEmailRequests)
          agentApplicationRepo.findById(testCase.agentApplication.agentApplicationId).futureValue.value shouldBe testCase.expectedAgentApplicationAfterRun
    }

  "processEmails sends the expired email to the verified agent email when agent details are complete" in:
    val expired: AgentApplication = tdAll.agentApplicationLlp.afterAgentDetailsCompleteWithOtherAgentEmail.makeOverdue.makeExpired
    val expiredEmailSent: AgentApplication = tdAll.agentApplicationLlp.afterAgentDetailsCompleteWithOtherAgentEmail.makeOverdue.makeExpiredEmailSent
    EmailStubs.stubSendEmail(expectedEmail(expired, tdAll.otherAgentEmailAddress))
    agentApplicationRepo.upsert(expired).futureValue

    emailServiceForExpiredApplications.processEmails().futureValue

    EmailStubs.verifySendEmail(count = 1)
    agentApplicationRepo.findById(expired.agentApplicationId).futureValue.value shouldBe expiredEmailSent
