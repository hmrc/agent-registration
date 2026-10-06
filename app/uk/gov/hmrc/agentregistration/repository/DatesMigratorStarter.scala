/*
 * Copyright 2025 HM Revenue & Customs
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

package uk.gov.hmrc.agentregistration.repository

import uk.gov.hmrc.agentregistration.config.AppConfig
import uk.gov.hmrc.mongo.lock.LockService
import uk.gov.hmrc.mongo.lock.MongoLockRepository

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.duration.*

// TODO: remove with DatesMigrator once the dates migration has run in every environment
@Singleton
class DatesMigratorStarter @Inject() (
  datesMigrator: DatesMigrator,
  mongoLockRepository: MongoLockRepository,
  appConfig: AppConfig
)(using ExecutionContext):

  private val lockService: LockService = LockService(
    lockRepository = mongoLockRepository,
    lockId = DatesMigratorStarter.lockId,
    ttl = 1.hour
  )

  if appConfig.DatesMigrator.enabled then start(): Unit

  def start(): Future[Option[Long]] = lockService.withLock(datesMigrator.migrate())

object DatesMigratorStarter:
  val lockId: String = "agent-registration-dates-migrator"
