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

package uk.gov.hmrc.agentregistration.services.email

import play.api.mvc.RequestHeader
import uk.gov.hmrc.agentregistration.connectors.email.EmailTemplateId
import uk.gov.hmrc.agentregistration.connectors.email.SendEmailRequest
import uk.gov.hmrc.agentregistration.repository.AgentApplicationRepo
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.testsupport.ISpec
import uk.gov.hmrc.agentregistration.testsupport.wiremock.stubs.EmailStubs

class EmailServiceForDeletedApplicationsSpec
extends ISpec:

  private lazy val emailServiceForDeletedApplications: EmailServiceForDeletedApplications = app.injector.instanceOf[EmailServiceForDeletedApplications]
  private lazy val agentApplicationRepo: AgentApplicationRepo = app.injector.instanceOf[AgentApplicationRepo]

  private given RequestHeader = tdAll.fakeBackendRequest

  private def expectedEmail(agentApplication: AgentApplication): SendEmailRequest = SendEmailRequest(
    to = Seq(tdAll.applicantEmailAddress),
    templateId = EmailTemplateId.ApplicationDeleted,
    parameters = Map(
      "agentName" -> tdAll.applicantName.value,
      "applicationRef" -> agentApplication.applicationReference.value
    )
  )

  "processEmails" - {

    "sends the deletion email to the verified applicant email and records deletionEmailSentAt" in:
      val agentApplication: AgentApplication = tdAll.agentApplicationLlp.afterContactDetailsCompleteExpiredPostExpiryGracePeriodEnded
      EmailStubs.stubSendEmail(expectedEmail(agentApplication))
      agentApplicationRepo.upsert(agentApplication).futureValue

      emailServiceForDeletedApplications.processEmails().futureValue

      EmailStubs.verifySendEmail(count = 1)
      agentApplicationRepo.findById(agentApplication.agentApplicationId).futureValue.value shouldBe
        tdAll.agentApplicationLlp.afterContactDetailsCompleteExpiredPostExpiryGracePeriodEndedDeletionEmailSent

    "does not send the deletion email a second time when deletionEmailSentAt is already recorded" in:
      val agentApplication: AgentApplication = tdAll.agentApplicationLlp.afterContactDetailsCompleteExpiredPostExpiryGracePeriodEndedDeletionEmailSent
      agentApplicationRepo.upsert(agentApplication).futureValue

      emailServiceForDeletedApplications.processEmails().futureValue

      EmailStubs.verifySendEmail(count = 0)
      agentApplicationRepo.findById(agentApplication.agentApplicationId).futureValue.value shouldBe agentApplication

    "does not send the deletion email and leaves the application untouched when there is no verified email address" in:
      val agentApplication: AgentApplication = tdAll.agentApplicationLlp.afterExpiredPostExpiryGracePeriodEnded
      agentApplicationRepo.upsert(agentApplication).futureValue

      emailServiceForDeletedApplications.processEmails().futureValue

      EmailStubs.verifySendEmail(count = 0)
      agentApplicationRepo.findById(agentApplication.agentApplicationId).futureValue.value shouldBe agentApplication

    "does not send the deletion email while the grace period has not ended" in:
      val agentApplication: AgentApplication = tdAll.agentApplicationLlp.afterContactDetailsCompleteExpired
      agentApplicationRepo.upsert(agentApplication).futureValue

      emailServiceForDeletedApplications.processEmails().futureValue

      EmailStubs.verifySendEmail(count = 0)
      agentApplicationRepo.findById(agentApplication.agentApplicationId).futureValue.value shouldBe agentApplication

    "leaves deletionEmailSentAt empty when the email service fails, so the email is retried on the next run" in:
      val agentApplication: AgentApplication = tdAll.agentApplicationLlp.afterContactDetailsCompleteExpiredPostExpiryGracePeriodEnded
      EmailStubs.stubSendEmailFailure(expectedEmail(agentApplication))
      agentApplicationRepo.upsert(agentApplication).futureValue

      emailServiceForDeletedApplications.processEmails().futureValue

      EmailStubs.verifySendEmail(count = 1)
      agentApplicationRepo.findById(agentApplication.agentApplicationId).futureValue.value shouldBe agentApplication
  }
