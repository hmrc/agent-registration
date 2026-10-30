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

import play.api.Configuration
import play.api.Environment
import play.api.mvc.RequestHeader
import play.api.test.FakeRequest
import uk.gov.hmrc.agentregistration.config.AppConfig
import uk.gov.hmrc.agentregistration.services.AgentApplicationDeletionService
import uk.gov.hmrc.agentregistration.services.ApplicationExpiryService
import uk.gov.hmrc.agentregistration.services.email.EmailServiceForDeletedApplications
import uk.gov.hmrc.agentregistration.services.email.EmailServiceForExpiredApplications
import uk.gov.hmrc.agentregistration.services.email.EmailServiceForExpiryReminders
import uk.gov.hmrc.agentregistration.testsupport.UnitSpec
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import java.util.concurrent.atomic.AtomicReference
import scala.annotation.nowarn
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ApplicationExpiryAndDeletionRunnerSpec
extends UnitSpec:

  private val allStages: List[String] = List(
    "processExpiry",
    "processEmailsForExpiryReminders",
    "processEmailsForExpiredApplications",
    "processEmailsForDeletedApplications",
    "processDeletions"
  )

  private val expiryStages: List[String] = allStages.take(3)
  private val deletionStages: List[String] = allStages.drop(3)

  private given RequestHeader = FakeRequest()

  "run" - {

    "runs every stage when all stages succeed and both expiry and deletion are enabled" in:
      val fixture: Fixture =
        new Fixture(
          failingStages = Set.empty,
          expiryEnabled = true,
          deletionEnabled = true
        )
      fixture.runner.run().futureValue shouldBe ()
      fixture.executedStages shouldBe allStages

    "skips the deletion stages when scheduler.deletion.enabled is false" in:
      val fixture: Fixture =
        new Fixture(
          failingStages = Set.empty,
          expiryEnabled = true,
          deletionEnabled = false
        )
      fixture.runner.run().futureValue shouldBe ()
      fixture.executedStages shouldBe expiryStages

    "skips the expiry stages when scheduler.expiry.enabled is false" in:
      val fixture: Fixture =
        new Fixture(
          failingStages = Set.empty,
          expiryEnabled = false,
          deletionEnabled = true
        )
      fixture.runner.run().futureValue shouldBe ()
      fixture.executedStages shouldBe deletionStages

    "still runs all downstream stages when a mid-pipeline stage fails" in:
      val fixture: Fixture =
        new Fixture(
          failingStages = Set("processEmailsForExpiryReminders"),
          expiryEnabled = true,
          deletionEnabled = true
        )
      fixture.runner.run().futureValue shouldBe ()
      fixture.executedStages shouldBe allStages withClue "stages after the failing one must still run"

    "succeeds even when every stage fails, so the scheduler is never broken" in:
      val fixture: Fixture =
        new Fixture(
          failingStages = allStages.toSet,
          expiryEnabled = true,
          deletionEnabled = true
        )
      fixture.runner.run().futureValue shouldBe ()
      fixture.executedStages shouldBe allStages
  }

  "runExpiryStages runs the expiry stages regardless of the flags" in:
    val fixture: Fixture =
      new Fixture(
        failingStages = Set.empty,
        expiryEnabled = false,
        deletionEnabled = false
      )
    fixture.runner.runExpiryStages().futureValue shouldBe ()
    fixture.executedStages shouldBe expiryStages

  "runDeletionStages runs the deletion stages regardless of the flags" in:
    val fixture: Fixture =
      new Fixture(
        failingStages = Set.empty,
        expiryEnabled = false,
        deletionEnabled = false
      )
    fixture.runner.runDeletionStages().futureValue shouldBe ()
    fixture.executedStages shouldBe deletionStages

  // the stage stubs must keep their (using RequestHeader) clause to match the signatures they override
  @nowarn("msg=unused implicit parameter")
  private class Fixture(
    failingStages: Set[String],
    expiryEnabled: Boolean,
    deletionEnabled: Boolean
  ):

    private val calls: AtomicReference[Vector[String]] = new AtomicReference(Vector.empty)

    def executedStages: List[String] = calls.get().toList

    private def stubStage(name: String): Future[Unit] =
      val _ = calls.updateAndGet(_ :+ name)
      if failingStages.contains(name) then Future.failed(new RuntimeException(s"$name failed"))
      else Future.successful(())

    private val configuration: Configuration = Configuration(
      "scheduler.expiry.enabled" -> expiryEnabled,
      "scheduler.deletion.enabled" -> deletionEnabled
    ).withFallback(Configuration.load(Environment.simple()))

    private val appConfig: AppConfig = new AppConfig(new ServicesConfig(configuration), configuration)

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

    private val emailServiceForDeletedApplications: EmailServiceForDeletedApplications =
      new EmailServiceForDeletedApplications(
        null,
        null,
        null
      ):
        override def processEmails()(using RequestHeader): Future[Unit] = stubStage("processEmailsForDeletedApplications")

    private val agentApplicationDeletionService: AgentApplicationDeletionService =
      new AgentApplicationDeletionService(
        null,
        null,
        null,
        null
      ):
        override def processDeletions()(using RequestHeader): Future[Unit] = stubStage("processDeletions")

    val runner: ApplicationExpiryAndDeletionRunner =
      new ApplicationExpiryAndDeletionRunner(
        applicationExpiryService,
        emailServiceForExpiryReminders,
        emailServiceForExpiredApplications,
        emailServiceForDeletedApplications,
        agentApplicationDeletionService,
        appConfig
      )
