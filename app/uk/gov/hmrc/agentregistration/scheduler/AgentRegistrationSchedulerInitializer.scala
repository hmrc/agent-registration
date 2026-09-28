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
import uk.gov.hmrc.agentregistration.runner.ExpireUnsubmittedApplicationsRunner

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AgentRegistrationSchedulerInitializer @Inject() (
  scheduler: Scheduler,
  appConfig: AppConfig,
  expireUnsubmittedApplicationsRunner: ExpireUnsubmittedApplicationsRunner
)
extends Logging:

  initialize()

  private def initialize(): Unit =
    if appConfig.Scheduler.expiryEnabled then
      logger.info("Bootstrapping expiry scheduler")
      scheduler.scheduleDaily(
        "expiring unsubmitted applications",
        appConfig.Scheduler.expiryTime,
        () => expireUnsubmittedApplicationsRunner.run()
      )
    else
      logger.info("expiry scheduler not scheduled as it is not enabled")
