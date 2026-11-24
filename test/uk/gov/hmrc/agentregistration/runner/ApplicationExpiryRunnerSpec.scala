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

package uk.gov.hmrc.agentregistration.runner

import uk.gov.hmrc.agentregistration.connectors.email.EmailTemplateId
import uk.gov.hmrc.agentregistration.connectors.email.SendEmailRequest
import uk.gov.hmrc.agentregistration.repository.AgentApplicationRepo
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.BusinessType
import uk.gov.hmrc.agentregistration.shared.testdata.agentapplication.ExpiryTransformations.*
import uk.gov.hmrc.agentregistration.testsupport.ISpec
import uk.gov.hmrc.agentregistration.testsupport.testdata.AgentApplicationStates
import uk.gov.hmrc.agentregistration.testsupport.wiremock.stubs.EmailStubs

class ApplicationExpiryRunnerSpec
extends ISpec:

  private lazy val applicationExpiryRunner: ApplicationExpiryRunner = app.injector.instanceOf[ApplicationExpiryRunner]
  private lazy val agentApplicationRepo: AgentApplicationRepo = app.injector.instanceOf[AgentApplicationRepo]

  private def expectedExpiryReminderEmail(agentApplication: AgentApplication): SendEmailRequest = SendEmailRequest(
    to = Seq(tdAll.applicantEmailAddress),
    templateId = EmailTemplateId.ApplicationExpiryReminder,
    parameters = Map(
      "agentName" -> tdAll.applicantName.value,
      "applicationRef" -> agentApplication.applicationReference.value,
      "expiryDate" -> "5 December 2059",
      "deletionDate" -> "19 January 2060"
    )
  )

  private def expectedExpiredEmail(agentApplication: AgentApplication): SendEmailRequest = SendEmailRequest(
    to = Seq(tdAll.applicantEmailAddress),
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
    expectedEmail: Option[SendEmailRequest],
    expectedSendEmailRequests: Int,
    expectedAgentApplicationAfterRun: AgentApplication
  )

  "stages lists the expiry, then the reminder emails, then the expired emails" in:
    applicationExpiryRunner.stages.map(_.name) shouldBe Seq(
      "ApplicationExpiryService.processExpiry",
      "EmailServiceForExpiryReminders.processEmails",
      "EmailServiceForExpiredApplications.processEmails"
    )

  BusinessType.values.foreach: businessType =>
    s"for a $businessType application" - {

      val agentApplicationStates: AgentApplicationStates = AgentApplicationStates.forBusinessType(businessType)
      val expiryReminderEmail: SendEmailRequest = expectedExpiryReminderEmail(agentApplicationStates.withVerifiedEmail)
      val expiredEmail: SendEmailRequest = expectedExpiredEmail(agentApplicationStates.withVerifiedEmail)

      Seq(
        TestCase(
          description = "sends the reminder to an application with 10 days left and does not expire it",
          agentApplication = agentApplicationStates.withVerifiedEmail.makeDueForExpiry,
          emailSendSuccessfully = true,
          expectedEmail = Some(expiryReminderEmail),
          expectedSendEmailRequests = 1,
          expectedAgentApplicationAfterRun = agentApplicationStates.withVerifiedEmail.makeExpiryReminderEmailSent
        ),
        TestCase(
          description = "expires an application whose expiry date has passed and sends the expired email in the same run",
          agentApplication = agentApplicationStates.withVerifiedEmail.makeOverdue,
          emailSendSuccessfully = true,
          expectedEmail = Some(expiredEmail),
          expectedSendEmailRequests = 1,
          expectedAgentApplicationAfterRun = agentApplicationStates.withVerifiedEmail.makeOverdue.makeExpiredEmailSent
        ),
        TestCase(
          description = "expires a long overdue application and sends only the expired email, never a reminder",
          agentApplication = agentApplicationStates.withVerifiedEmail.makeLongOverdue,
          emailSendSuccessfully = true,
          expectedEmail = Some(expiredEmail),
          expectedSendEmailRequests = 1,
          expectedAgentApplicationAfterRun = agentApplicationStates.withVerifiedEmail.makeLongOverdue.makeExpiredEmailSent
        ),
        TestCase(
          description = "expires the application, with no expired email recorded, when the email service fails, so the next run retries",
          agentApplication = agentApplicationStates.withVerifiedEmail.makeOverdue,
          emailSendSuccessfully = false,
          expectedEmail = Some(expiredEmail),
          expectedSendEmailRequests = 1,
          expectedAgentApplicationAfterRun = agentApplicationStates.withVerifiedEmail.makeOverdue.makeExpired
        ),
        TestCase(
          description = "expires an application with no verified email address without sending any email",
          agentApplication = agentApplicationStates.started.makeLongOverdue,
          emailSendSuccessfully = true,
          expectedEmail = None,
          expectedSendEmailRequests = 0,
          expectedAgentApplicationAfterRun = agentApplicationStates.started.makeLongOverdue.makeExpired
        ),
        TestCase(
          description = "does not send the expired email to an application whose grace period has ended, as the deletion email is due",
          agentApplication = agentApplicationStates.withVerifiedEmail.makeOverdue.makeExpired.makeDueForDeletion,
          emailSendSuccessfully = true,
          expectedEmail = None,
          expectedSendEmailRequests = 0,
          expectedAgentApplicationAfterRun = agentApplicationStates.withVerifiedEmail.makeOverdue.makeExpired.makeDueForDeletion
        )
      ).foreach: testCase =>
        s"run ${testCase.description}" in:
          testCase.expectedEmail.foreach: expectedEmail =>
            if testCase.emailSendSuccessfully
            then EmailStubs.stubSendEmail(expectedEmail)
            else EmailStubs.stubSendEmailFailure(expectedEmail)
          agentApplicationRepo.upsert(testCase.agentApplication).futureValue

          applicationExpiryRunner.run().futureValue

          EmailStubs.verifySendEmail(count = testCase.expectedSendEmailRequests)
          agentApplicationRepo.findById(testCase.agentApplication.agentApplicationId).futureValue.value shouldBe testCase.expectedAgentApplicationAfterRun
    }
