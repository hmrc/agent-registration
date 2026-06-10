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
import uk.gov.hmrc.agentregistration.config.AppConfig
import uk.gov.hmrc.mongo.lock.LockRepository
import uk.gov.hmrc.mongo.lock.LockService
import uk.gov.hmrc.mongo.lock.MongoLockRepository

import java.time.Clock
import java.time.LocalTime
import java.time.ZonedDateTime
import java.util.concurrent.Executors
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
  mongoLockRepository: MongoLockRepository
)(using ec: ExecutionContext)
extends Logging:

  private val executor = Executors.newScheduledThreadPool(1)

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

    val nextRun: ZonedDateTime = nextRunTime(timeOfDay)
    val delayMillis: Long = nextRun.toInstant.toEpochMilli - now().toInstant.toEpochMilli

    executor.schedule(
      new Runnable:
        def run(): Unit = lockServiceFor(name).withLock {
          logger.info(s"Starting scheduled task: $name at ${ZonedDateTime.now(clock).toString}")
          job()
        }.onComplete { result =>
          result match
            case Success(Some(_)) => logger.info(s"Scheduled task completed successfully: $name")
            case Success(None) => logger.debug(s"Scheduled task skipped - already running on another instance: $name")
            case Failure(e) => logger.error(s"Scheduled task failed: $name, ${e.getMessage}", e)
          scheduleDaily(
            name = name,
            timeOfDay = timeOfDay,
            job = job
          )
        }
      ,
      delayMillis,
      TimeUnit.MILLISECONDS
    )
    logger.info(s"$name scheduled for ${nextRun.toString}")
    ()

  private def nextRunTime(timeOfDay: LocalTime): ZonedDateTime =
    val currentTime: ZonedDateTime = now()
    val today: ZonedDateTime = currentTime.`with`(timeOfDay)
    if currentTime.isBefore(today) then
      today
    else
      today.plusDays(1)
