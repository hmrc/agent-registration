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
import play.api.mvc.RequestHeader
import uk.gov.hmrc.agentregistration.config.AppConfig
import uk.gov.hmrc.agentregistration.services.EmailServiceForApplicationsReadyToSubmit
import uk.gov.hmrc.agentregistration.util.EmptyRequest

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext

@Singleton
class ApplicationReadyToSubmitEmailSchedulerInitializer @Inject() (
  emailServiceForApplicationsReadyToSubmit: EmailServiceForApplicationsReadyToSubmit,
  scheduler: Scheduler,
  appConfig: AppConfig
)(using ExecutionContext)
extends Logging:

  initialize()

  private def initialize(): Unit =
    if appConfig.Scheduler.enabled then
      logger.info("Bootstrapping application-ready-to-submit email scheduler")
      scheduler.scheduleDaily(
        "sending application ready to submit emails",
        appConfig.Scheduler.time,
        () =>
          given RequestHeader = EmptyRequest.emptyRequestHeader
          emailServiceForApplicationsReadyToSubmit.processEmails()
      )
    else
      logger.info("application-ready-to-submit email scheduler is not enabled")
