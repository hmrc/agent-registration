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

package uk.gov.hmrc.agentregistration.scheduler

import org.scalatest.concurrent.Eventually
import play.api.inject.DefaultApplicationLifecycle
import uk.gov.hmrc.agentregistration.testsupport.ISpec
import uk.gov.hmrc.mongo.lock.MongoLockRepository

import java.util.concurrent.atomic.AtomicInteger
import scala.concurrent.Future
import scala.concurrent.duration.*

class SchedulerSpec
extends ISpec,
  Eventually:

  private def newScheduler(applicationLifecycle: DefaultApplicationLifecycle): Scheduler =
    new Scheduler(
      clock,
      app.injector.instanceOf[MongoLockRepository],
      applicationLifecycle
    )

  "scheduleEvery runs the job again after each interval, and stop ends the schedule" in:
    val scheduler: Scheduler = newScheduler(new DefaultApplicationLifecycle())
    val runCount: AtomicInteger = new AtomicInteger(0)

    scheduler.scheduleEvery(
      name = "scheduler-spec-job",
      interval = 200.millis,
      job =
        () =>
          runCount.incrementAndGet()
          Future.successful(())
    )

    eventually:
      runCount.get() should be >= 2

    scheduler.stop()
    scheduler.isStopped shouldBe true

  "stopping the application stops the scheduler" in:
    val applicationLifecycle: DefaultApplicationLifecycle = new DefaultApplicationLifecycle()
    val scheduler: Scheduler = newScheduler(applicationLifecycle)
    val runCount: AtomicInteger = new AtomicInteger(0)

    scheduler.scheduleEvery(
      name = "scheduler-spec-lifecycle-job",
      interval = 200.millis,
      job =
        () =>
          runCount.incrementAndGet()
          Future.successful(())
    )

    eventually:
      runCount.get() should be >= 1

    scheduler.isStopped shouldBe false
    applicationLifecycle.stop().futureValue
    scheduler.isStopped shouldBe true
