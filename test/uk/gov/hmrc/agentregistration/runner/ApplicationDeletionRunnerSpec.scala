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
import uk.gov.hmrc.agentregistration.services.AgentApplicationDeletionService
import uk.gov.hmrc.agentregistration.services.email.EmailServiceForDeletedApplications
import uk.gov.hmrc.agentregistration.testsupport.UnitSpec

import java.util.concurrent.atomic.AtomicReference
import scala.annotation.nowarn
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ApplicationDeletionRunnerSpec
extends UnitSpec:

  private val allStages: List[String] = List(
    "processEmailsForDeletedApplications",
    "processDeletions"
  )

  "run" - {

    "runs every stage in order when all stages succeed" in:
      val fixture: Fixture = new Fixture(failingStages = Set.empty)
      fixture.runner.run().futureValue shouldBe ()
      fixture.executedStages shouldBe allStages

    "still runs all downstream stages when a stage fails" in:
      val fixture: Fixture = new Fixture(failingStages = Set("processEmailsForDeletedApplications"))
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

    val runner: ApplicationDeletionRunner =
      new ApplicationDeletionRunner(
        emailServiceForDeletedApplications,
        agentApplicationDeletionService
      )
