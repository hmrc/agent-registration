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
import uk.gov.hmrc.agentregistration.testsupport.testdata.AgentApplicationPreconditions.*
import uk.gov.hmrc.agentregistration.testsupport.testdata.AgentApplicationStates
import uk.gov.hmrc.agentregistration.testsupport.wiremock.stubs.EmailStubs

class EmailServiceForExpiryRemindersSpec
extends ISpec:

  private lazy val emailServiceForExpiryReminders: EmailServiceForExpiryReminders = app.injector.instanceOf[EmailServiceForExpiryReminders]
  private lazy val agentApplicationRepo: AgentApplicationRepo = app.injector.instanceOf[AgentApplicationRepo]

  private given RequestHeader = tdAll.fakeBackendRequest

  private def expectedEmail(
    agentApplication: AgentApplication,
    emailAddress: EmailAddress = tdAll.applicantEmailAddress
  ): SendEmailRequest = SendEmailRequest(
    to = Seq(emailAddress),
    templateId = EmailTemplateId.ApplicationExpiryReminder,
    parameters = Map(
      "agentName" -> tdAll.applicantName.value,
      "applicationRef" -> agentApplication.applicationReference.value,
      "expiryDate" -> "5 December 2059",
      "deletionDate" -> "19 January 2060"
    )
  )

  BusinessType.values.foreach: businessType =>
    s"for a $businessType application" - {

      lazy val agentApplication: AgentApplicationStates = AgentApplicationStates.forBusinessType(businessType)

      "processEmails sends the reminder to the verified applicant email and records it as sent" in:
        val dueForExpiry: AgentApplication = agentApplication.withVerifiedEmail.makeDueForExpiry
        val expiryReminderEmailSent: AgentApplication = agentApplication.withVerifiedEmail.makeExpiryReminderEmailSent
        EmailStubs.stubSendEmail(expectedEmail(dueForExpiry))
        agentApplicationRepo.upsert(dueForExpiry).futureValue

        emailServiceForExpiryReminders.processEmails().futureValue

        EmailStubs.verifySendEmail(count = 1)
        agentApplicationRepo.findById(dueForExpiry.agentApplicationId).futureValue.value shouldBe expiryReminderEmailSent

      "processEmails does not send the reminder to an application already sent it" in:
        val expiryReminderEmailSent: AgentApplication = agentApplication.withVerifiedEmail.makeExpiryReminderEmailSent
        expiryReminderEmailSent.expiryReminderEmailSentAt shouldBe defined withClue "precondition: the reminder must already be recorded"
        expiryReminderEmailSent.shouldHaveExpiryDateWithinReminderPeriod
        agentApplicationRepo.upsert(expiryReminderEmailSent).futureValue

        emailServiceForExpiryReminders.processEmails().futureValue

        EmailStubs.verifySendEmail(count = 0)
        agentApplicationRepo.findById(expiryReminderEmailSent.agentApplicationId).futureValue.value shouldBe expiryReminderEmailSent

      "processEmails does not send the reminder to an application with no verified email address" in:
        val dueForExpiry: AgentApplication = agentApplication.started.makeDueForExpiry
        dueForExpiry.applicantContactDetails shouldBe None withClue "precondition: the application must have no applicant contact details"
        agentApplicationRepo.upsert(dueForExpiry).futureValue

        emailServiceForExpiryReminders.processEmails().futureValue

        EmailStubs.verifySendEmail(count = 0)
        agentApplicationRepo.findById(dueForExpiry.agentApplicationId).futureValue.value shouldBe dueForExpiry

      "processEmails does not send the reminder to an application expiring after the reminder period" in:
        val notYetDueForExpiry: AgentApplication = agentApplication.withVerifiedEmail
        notYetDueForExpiry.shouldHaveExpiryDateAfterReminderPeriod
        agentApplicationRepo.upsert(notYetDueForExpiry).futureValue

        emailServiceForExpiryReminders.processEmails().futureValue

        EmailStubs.verifySendEmail(count = 0)
        agentApplicationRepo.findById(notYetDueForExpiry.agentApplicationId).futureValue.value shouldBe notYetDueForExpiry

      // TODO: confirm with BA whether a long overdue application should be sent the reminder before it expires
      "processEmails does not send the reminder to an application whose expiry date has passed, so a long overdue application expires without one" in:
        val longOverdue: AgentApplication = agentApplication.withVerifiedEmail.makeLongOverdue
        longOverdue.shouldHaveExpiryDateInThePast
        longOverdue.expiryReminderEmailSentAt shouldBe None withClue "precondition: the reminder must not be recorded, so only the date keeps it out"
        agentApplicationRepo.upsert(longOverdue).futureValue

        emailServiceForExpiryReminders.processEmails().futureValue

        EmailStubs.verifySendEmail(count = 0)
        agentApplicationRepo.findById(longOverdue.agentApplicationId).futureValue.value shouldBe longOverdue

      "processEmails does not record the reminder as sent when the email service fails, so it is retried on the next run" in:
        val dueForExpiry: AgentApplication = agentApplication.withVerifiedEmail.makeDueForExpiry
        EmailStubs.stubSendEmailFailure(expectedEmail(dueForExpiry))
        agentApplicationRepo.upsert(dueForExpiry).futureValue

        emailServiceForExpiryReminders.processEmails().futureValue

        EmailStubs.verifySendEmail(count = 1)
        agentApplicationRepo.findById(dueForExpiry.agentApplicationId).futureValue.value shouldBe dueForExpiry
    }

  "processEmails sends the reminder to the verified agent email when agent details are complete" in:
    val dueForExpiry: AgentApplication = tdAll.agentApplicationLlp.afterAgentDetailsCompleteWithOtherAgentEmail.makeDueForExpiry
    val expiryReminderEmailSent: AgentApplication = tdAll.agentApplicationLlp.afterAgentDetailsCompleteWithOtherAgentEmail.makeExpiryReminderEmailSent
    EmailStubs.stubSendEmail(expectedEmail(dueForExpiry, tdAll.otherAgentEmailAddress))
    agentApplicationRepo.upsert(dueForExpiry).futureValue

    emailServiceForExpiryReminders.processEmails().futureValue

    EmailStubs.verifySendEmail(count = 1)
    agentApplicationRepo.findById(dueForExpiry.agentApplicationId).futureValue.value shouldBe expiryReminderEmailSent
