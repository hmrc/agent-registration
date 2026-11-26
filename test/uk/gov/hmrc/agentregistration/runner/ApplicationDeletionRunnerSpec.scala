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
import uk.gov.hmrc.agentregistration.shared.testdata.agentapplication.ExpiryTransformations.*
import uk.gov.hmrc.agentregistration.testsupport.ISpec
import uk.gov.hmrc.agentregistration.testsupport.testdata.AgentApplicationPreconditions.*
import uk.gov.hmrc.agentregistration.testsupport.wiremock.stubs.EmailStubs

class ApplicationDeletionRunnerSpec
extends ISpec:

  private lazy val applicationDeletionRunner: ApplicationDeletionRunner = app.injector.instanceOf[ApplicationDeletionRunner]
  private lazy val agentApplicationRepo: AgentApplicationRepo = app.injector.instanceOf[AgentApplicationRepo]

  private def expectedEmail(agentApplication: AgentApplication): SendEmailRequest = SendEmailRequest(
    to = Seq(tdAll.applicantEmailAddress),
    templateId = EmailTemplateId.ApplicationDeleted,
    parameters = Map(
      "agentName" -> tdAll.applicantName.value,
      "applicationRef" -> agentApplication.applicationReference.value
    )
  )

  private final case class TestCase(
    description: String,
    agentApplication: AgentApplication,
    emailSendSuccessfully: Boolean
  )

  "stages lists the deletion emails, then the deletions" in:
    applicationDeletionRunner.stages.map(_.name) shouldBe Seq(
      "EmailServiceForDeletedApplications.processEmails",
      "AgentApplicationDeletionService.processDeletions"
    )

  Seq(
    TestCase(
      description = "sends the deletion email and deletes the application in the same run",
      agentApplication = tdAll.agentApplicationLlp.afterContactDetailsComplete.makeOverdue.makeExpiredEmailSent.makeDueForDeletion,
      emailSendSuccessfully = true
    ),
    TestCase(
      description = "deletes the application even when the email service fails",
      agentApplication = tdAll.agentApplicationLlp.afterContactDetailsComplete.makeOverdue.makeExpiredEmailSent.makeDueForDeletion,
      emailSendSuccessfully = false
    )
  ).foreach: testCase =>
    s"run ${testCase.description}" in:
      testCase.agentApplication.shouldHaveGracePeriodEndInThePast
      testCase.agentApplication.deletionEmailSentAt shouldBe None withClue "precondition: the deletion email must not be recorded"
      if testCase.emailSendSuccessfully
      then EmailStubs.stubSendEmail(expectedEmail(testCase.agentApplication))
      else EmailStubs.stubSendEmailFailure(expectedEmail(testCase.agentApplication))
      agentApplicationRepo.upsert(testCase.agentApplication).futureValue

      applicationDeletionRunner.run().futureValue

      EmailStubs.verifySendEmail(count = 1)
      agentApplicationRepo.findById(testCase.agentApplication.agentApplicationId).futureValue shouldBe None
