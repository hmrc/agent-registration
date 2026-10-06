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

import play.api.Logging
import play.api.inject.ApplicationLifecycle
import uk.gov.hmrc.agentregistration.config.AppConfig
import uk.gov.hmrc.mongo.lock.LockRepository
import uk.gov.hmrc.mongo.lock.LockService
import uk.gov.hmrc.mongo.lock.MongoLockRepository

import java.time.Clock
import java.time.LocalTime
import java.time.ZonedDateTime
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.duration.*
import scala.util.Failure
import scala.util.Success

@Singleton
class Scheduler @Inject() (
  clock: Clock,
  mongoLockRepository: MongoLockRepository,
  applicationLifecycle: ApplicationLifecycle
)(using ec: ExecutionContext)
extends Logging:

  private val executor: ScheduledExecutorService = Executors.newScheduledThreadPool(1)

  applicationLifecycle.addStopHook(() => Future.successful(stop()))

  def stop(): Unit =
    executor.shutdownNow()
    ()

  def isStopped: Boolean = executor.isShutdown

  private def now(): ZonedDateTime = ZonedDateTime.now(clock.withZone(AppConfig.zoneId))

  private def lockServiceFor(name: String): LockService =
    new LockService:
      override val lockId: String = s"schedules.$name"
      override val ttl: scala.concurrent.duration.Duration = 1.hour
      override val lockRepository: LockRepository = mongoLockRepository

  @SuppressWarnings(Array("org.wartremover.warts.Recursion"))
  def scheduleDaily(
    name: String,
    timeOfDay: LocalTime,
    job: () => Future[Unit]
  ): Unit =
    val nextRun: ZonedDateTime = nextDailyRunTime(timeOfDay)
    val delayMillis: Long = nextRun.toInstant.toEpochMilli - now().toInstant.toEpochMilli
    schedule(
      name,
      delayMillis,
      job
    )(reschedule =
      scheduleDaily(
        name,
        timeOfDay,
        job
      )
    )
    logger.info(s"$name scheduled for ${nextRun.toString}")

  @SuppressWarnings(Array("org.wartremover.warts.Recursion"))
  def scheduleEvery(
    name: String,
    interval: FiniteDuration,
    job: () => Future[Unit]
  ): Unit =
    schedule(
      name,
      interval.toMillis,
      job
    )(reschedule =
      scheduleEvery(
        name,
        interval,
        job
      )
    )
    logger.info(s"$name scheduled to run in ${interval.toString}")

  private def schedule(
    name: String,
    delayMillis: Long,
    job: () => Future[Unit]
  )(reschedule: => Unit): Unit =
    if executor.isShutdown then
      logger.info(s"$name not scheduled as the scheduler has been stopped")
    else
      executor.schedule(
        new Runnable:
          // The lock is taken inside a Future so that a synchronous failure (e.g. a closed Mongo client) is logged and rescheduled like any other.
          def run(): Unit = Future
            .unit
            .flatMap: _ =>
              lockServiceFor(name).withLock {
                logger.info(s"Running $name...")
                job()
              }
            .onComplete { result =>
              result match
                case Success(Some(_)) => logger.info(s"Running $name DONE")
                case Success(None) => logger.debug(s"Skipped $name: already running on another instance")
                case Failure(e) => logger.error(s"Running $name FAILED", e)
              reschedule
            }
        ,
        delayMillis,
        TimeUnit.MILLISECONDS
      )
      ()

  private def nextDailyRunTime(timeOfDay: LocalTime): ZonedDateTime =
    val currentTime: ZonedDateTime = now()
    val today: ZonedDateTime = currentTime.`with`(timeOfDay)
    if currentTime.isBefore(today) then
      today
    else
      today.plusDays(1)
