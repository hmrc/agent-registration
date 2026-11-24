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

import uk.gov.hmrc.agentregistration.testsupport.UnitSpec

import java.util.concurrent.atomic.AtomicReference
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.Promise

class RunnerHelperSpec
extends UnitSpec:

  private val runnerName: String = "TestRunner"

  "run runs every stage in order when all stages succeed" in:
    val startedStageNames: AtomicReference[Seq[String]] = new AtomicReference(Seq.empty)
    val stages: Seq[Stage] = Seq(
      Stage(
        name = "first",
        run = {
          val _ = startedStageNames.updateAndGet(_ :+ "first")
          Future.unit
        }
      ),
      Stage(
        name = "second",
        run = {
          val _ = startedStageNames.updateAndGet(_ :+ "second")
          Future.unit
        }
      ),
      Stage(
        name = "third",
        run = {
          val _ = startedStageNames.updateAndGet(_ :+ "third")
          Future.unit
        }
      )
    )

    RunnerHelper.run(runnerName, stages).futureValue shouldBe ()

    startedStageNames.get() shouldBe Seq("first", "second", "third")

  "run starts a stage only after the previous stage has finished" in:
    val startedStageNames: AtomicReference[Seq[String]] = new AtomicReference(Seq.empty)
    val firstStageFinished: Promise[Unit] = Promise[Unit]()
    val stages: Seq[Stage] = Seq(
      Stage(
        name = "first",
        run = {
          val _ = startedStageNames.updateAndGet(_ :+ "first")
          firstStageFinished.future
        }
      ),
      Stage(
        name = "second",
        run = {
          val _ = startedStageNames.updateAndGet(_ :+ "second")
          Future.unit
        }
      )
    )

    val running: Future[Unit] = RunnerHelper.run(runnerName, stages)

    eventually(startedStageNames.get() shouldBe Seq("first"))
    running.isCompleted shouldBe false withClue "the run must wait for the first stage"
    startedStageNames.get() shouldBe Seq("first") withClue "the second stage must not start while the first is running"

    firstStageFinished.success(())
    running.futureValue shouldBe ()

    startedStageNames.get() shouldBe Seq("first", "second")

  "run still runs the remaining stages when a stage fails" in:
    val startedStageNames: AtomicReference[Seq[String]] = new AtomicReference(Seq.empty)
    val stages: Seq[Stage] = Seq(
      Stage(
        name = "first",
        run = {
          val _ = startedStageNames.updateAndGet(_ :+ "first")
          Future.unit
        }
      ),
      Stage(
        name = "second",
        run = {
          val _ = startedStageNames.updateAndGet(_ :+ "second")
          Future.failed(new RuntimeException("stage failed"))
        }
      ),
      Stage(
        name = "third",
        run = {
          val _ = startedStageNames.updateAndGet(_ :+ "third")
          Future.unit
        }
      )
    )

    RunnerHelper.run(runnerName, stages).futureValue shouldBe ()

    startedStageNames.get() shouldBe Seq("first", "second", "third")

  "run still runs the remaining stages when a stage throws an exception instead of returning a failed Future" in:
    val startedStageNames: AtomicReference[Seq[String]] = new AtomicReference(Seq.empty)
    val stages: Seq[Stage] = Seq(
      Stage(
        name = "first",
        run = {
          val _ = startedStageNames.updateAndGet(_ :+ "first")
          throw new RuntimeException("stage threw an exception")
        }
      ),
      Stage(
        name = "second",
        run = {
          val _ = startedStageNames.updateAndGet(_ :+ "second")
          throw new RuntimeException("stage threw an exception")
        }
      ),
      Stage(
        name = "third",
        run = {
          val _ = startedStageNames.updateAndGet(_ :+ "third")
          Future.unit
        }
      )
    )

    RunnerHelper.run(runnerName, stages).futureValue shouldBe ()

    startedStageNames.get() shouldBe Seq("first", "second", "third")

  "run succeeds even when every stage fails, so the scheduler is never broken" in:
    val startedStageNames: AtomicReference[Seq[String]] = new AtomicReference(Seq.empty)
    val stages: Seq[Stage] = Seq(
      Stage(
        name = "first",
        run = {
          val _ = startedStageNames.updateAndGet(_ :+ "first")
          Future.failed(new RuntimeException("stage failed"))
        }
      ),
      Stage(
        name = "second",
        run = {
          val _ = startedStageNames.updateAndGet(_ :+ "second")
          Future.failed(new RuntimeException("stage failed"))
        }
      ),
      Stage(
        name = "third",
        run = {
          val _ = startedStageNames.updateAndGet(_ :+ "third")
          Future.failed(new RuntimeException("stage failed"))
        }
      )
    )

    RunnerHelper.run(runnerName, stages).futureValue shouldBe ()

    startedStageNames.get() shouldBe Seq("first", "second", "third")
