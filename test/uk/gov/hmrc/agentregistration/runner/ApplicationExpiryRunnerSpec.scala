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
import uk.gov.hmrc.agentregistration.services.ApplicationExpiryService
import uk.gov.hmrc.agentregistration.services.email.EmailServiceForExpiredApplications
import uk.gov.hmrc.agentregistration.services.email.EmailServiceForExpiryReminders
import uk.gov.hmrc.agentregistration.testsupport.UnitSpec

import java.util.concurrent.atomic.AtomicReference
import scala.annotation.nowarn
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ApplicationExpiryRunnerSpec
extends UnitSpec:

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
