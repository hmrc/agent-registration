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

import play.api.mvc.RequestHeader
import uk.gov.hmrc.agentregistration.connectors.email.EmailTemplateId
import uk.gov.hmrc.agentregistration.connectors.email.SendEmailRequest
import uk.gov.hmrc.agentregistration.repository.AgentApplicationRepo
import uk.gov.hmrc.agentregistration.services.ApplicationExpiryService
import uk.gov.hmrc.agentregistration.services.email.EmailServiceForExpiredApplications
import uk.gov.hmrc.agentregistration.services.email.EmailServiceForExpiryReminders
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.BusinessType
import uk.gov.hmrc.agentregistration.shared.testdata.agentapplication.ExpiryTransformations.*
import uk.gov.hmrc.agentregistration.testsupport.ISpec
import uk.gov.hmrc.agentregistration.testsupport.testdata.AgentApplicationPreconditions.*
import uk.gov.hmrc.agentregistration.testsupport.testdata.AgentApplicationStates
import uk.gov.hmrc.agentregistration.testsupport.wiremock.stubs.EmailStubs

import java.util.concurrent.atomic.AtomicReference
import scala.annotation.nowarn
import scala.concurrent.Future

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

  private val allStages: List[String] = List(
    "processExpiry",
    "processEmailsForExpiryReminders",
    "processEmailsForExpiredApplications"
  )

  "run" - {

    "runs every stage in order when all stages succeed" in:
      val fixture: Fixture = new Fixture(failingStages = Set.empty)
      fixture.runner.run().futureValue shouldBe ()
      fixture.executedStages shouldBe allStages

    "still runs all downstream stages when a stage fails" in:
      val fixture: Fixture = new Fixture(failingStages = Set("processEmailsForExpiryReminders"))
      fixture.runner.run().futureValue shouldBe ()
      fixture.executedStages shouldBe allStages withClue "stages after the failing one must still run"

    "succeeds even when every stage fails, so the scheduler is never broken" in:
      val fixture: Fixture = new Fixture(failingStages = allStages.toSet)
      fixture.runner.run().futureValue shouldBe ()
      fixture.executedStages shouldBe allStages
  }

  BusinessType.values.foreach: businessType =>
    s"for a $businessType application" - {

      lazy val agentApplication: AgentApplicationStates = AgentApplicationStates.forBusinessType(businessType)

      "run sends the reminder to an application with 10 days left and does not expire it" in:
        val dueForExpiry: AgentApplication = agentApplication.withVerifiedEmail.makeDueForExpiry
        val expiryReminderEmailSent: AgentApplication = agentApplication.withVerifiedEmail.makeExpiryReminderEmailSent
        dueForExpiry.shouldHaveExpiryDateWithinReminderPeriod
        EmailStubs.stubSendEmail(expectedExpiryReminderEmail(dueForExpiry))
        agentApplicationRepo.upsert(dueForExpiry).futureValue

        applicationExpiryRunner.run().futureValue

        EmailStubs.verifySendEmail(count = 1)
        agentApplicationRepo.findById(dueForExpiry.agentApplicationId).futureValue.value shouldBe expiryReminderEmailSent

      "run expires an application whose expiry date has passed and sends the expired email in the same run" in:
        val overdue: AgentApplication = agentApplication.withVerifiedEmail.makeOverdue
        val expiredEmailSent: AgentApplication = agentApplication.withVerifiedEmail.makeOverdue.makeExpiredEmailSent
        overdue.shouldHaveExpiryDateInThePast
        EmailStubs.stubSendEmail(expectedExpiredEmail(overdue))
        agentApplicationRepo.upsert(overdue).futureValue

        applicationExpiryRunner.run().futureValue

        EmailStubs.verifySendEmail(count = 1)
        agentApplicationRepo.findById(overdue.agentApplicationId).futureValue.value shouldBe expiredEmailSent

      "run expires a long overdue application and sends only the expired email, never a reminder" in:
        val longOverdue: AgentApplication = agentApplication.withVerifiedEmail.makeLongOverdue
        val expiredEmailSent: AgentApplication = agentApplication.withVerifiedEmail.makeLongOverdue.makeExpiredEmailSent
        longOverdue.shouldHaveExpiryDateInThePast
        longOverdue.expiryReminderEmailSentAt shouldBe None withClue "precondition: the reminder must not be recorded"
        EmailStubs.stubSendEmail(expectedExpiredEmail(longOverdue))
        agentApplicationRepo.upsert(longOverdue).futureValue

        applicationExpiryRunner.run().futureValue

        EmailStubs.verifySendEmail(count = 1)
        agentApplicationRepo.findById(longOverdue.agentApplicationId).futureValue.value shouldBe expiredEmailSent

      "run expires the application, with no expired email recorded, when the email service fails, so the next run retries" in:
        val overdue: AgentApplication = agentApplication.withVerifiedEmail.makeOverdue
        overdue.shouldHaveExpiryDateInThePast
        EmailStubs.stubSendEmailFailure(expectedExpiredEmail(overdue))
        agentApplicationRepo.upsert(overdue).futureValue

        applicationExpiryRunner.run().futureValue

        EmailStubs.verifySendEmail(count = 1)
        agentApplicationRepo.findById(overdue.agentApplicationId).futureValue.value shouldBe overdue.makeExpired

      "run expires an application with no verified email address without sending any email" in:
        val longOverdue: AgentApplication = agentApplication.started.makeLongOverdue
        longOverdue.shouldHaveExpiryDateInThePast
        longOverdue.applicantContactDetails shouldBe None withClue "precondition: the application must have no applicant contact details"
        agentApplicationRepo.upsert(longOverdue).futureValue

        applicationExpiryRunner.run().futureValue

        EmailStubs.verifySendEmail(count = 0)
        agentApplicationRepo.findById(longOverdue.agentApplicationId).futureValue.value shouldBe longOverdue.makeExpired
    }

  // the stage stubs must keep their (using RequestHeader) clause to match the signatures they override
  @nowarn("msg=unused implicit parameter")
  private class Fixture(failingStages: Set[String]):

    private val calls: AtomicReference[Vector[String]] = new AtomicReference(Vector.empty)

    def executedStages: List[String] = calls.get().toList

    private def stubStage(name: String): Future[Unit] =
      val _ = calls.updateAndGet(_ :+ name)
      if failingStages.contains(name) then Future.failed(new RuntimeException(s"$name failed"))
      else Future.successful(())

    private val applicationExpiryService: ApplicationExpiryService =
      new ApplicationExpiryService(
        null,
        null,
        null
      ):
        override def processExpiry()(using RequestHeader): Future[Unit] = stubStage("processExpiry")

    private val emailServiceForExpiryReminders: EmailServiceForExpiryReminders =
      new EmailServiceForExpiryReminders(
        null,
        null,
        null,
        null
      ):
        override def processEmails()(using RequestHeader): Future[Unit] = stubStage("processEmailsForExpiryReminders")

    private val emailServiceForExpiredApplications: EmailServiceForExpiredApplications =
      new EmailServiceForExpiredApplications(
        null,
        null,
        null
      ):
        override def processEmails()(using RequestHeader): Future[Unit] = stubStage("processEmailsForExpiredApplications")

    val runner: ApplicationExpiryRunner =
      new ApplicationExpiryRunner(
        applicationExpiryService = applicationExpiryService,
        emailServiceForExpiryReminders = emailServiceForExpiryReminders,
        emailServiceForExpiredApplications = emailServiceForExpiredApplications
      )
