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
import uk.gov.hmrc.agentregistration.runner.ApplicationDeletionRunner
import uk.gov.hmrc.agentregistration.runner.ApplicationExpiryRunner

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AgentRegistrationSchedulerInitializer @Inject() (
  scheduler: Scheduler,
  appConfig: AppConfig,
  applicationExpiryRunner: ApplicationExpiryRunner,
  applicationDeletionRunner: ApplicationDeletionRunner
)
extends Logging:

  initialize()

  private def initialize(): Unit =
    if appConfig.Scheduler.expiryEnabled then
      logger.info("Bootstrapping application expiry scheduler")
      scheduler.scheduleDaily(
        "application expiry",
        appConfig.Scheduler.expiryTime,
        () => applicationExpiryRunner.run()
      )
    else
      logger.info("application expiry scheduler not scheduled as it is not enabled")

    if appConfig.Scheduler.deletionEnabled then
      logger.info("Bootstrapping application deletion scheduler")
      scheduler.scheduleDaily(
        "application deletion",
        appConfig.Scheduler.deletionTime,
        () => applicationDeletionRunner.run()
      )
    else
      logger.info("application deletion scheduler not scheduled as it is not enabled")
