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
import uk.gov.hmrc.agentregistration.config.AppConfig
import uk.gov.hmrc.agentregistration.services.AgentApplicationDeletionService
import uk.gov.hmrc.agentregistration.services.ApplicationExpiryService
import uk.gov.hmrc.agentregistration.services.email.EmailServiceForDeletedApplications
import uk.gov.hmrc.agentregistration.services.email.EmailServiceForExpiredApplications
import uk.gov.hmrc.agentregistration.services.email.EmailServiceForExpiryReminders
import uk.gov.hmrc.agentregistration.util.EmptyRequest
import uk.gov.hmrc.agentregistration.util.RequestAwareLogging

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@Singleton
class ApplicationExpiryAndDeletionRunner @Inject() (
  applicationExpiryService: ApplicationExpiryService,
  emailServiceForExpiryReminders: EmailServiceForExpiryReminders,
  emailServiceForExpiredApplications: EmailServiceForExpiredApplications,
  emailServiceForDeletedApplications: EmailServiceForDeletedApplications,
  agentApplicationDeletionService: AgentApplicationDeletionService,
  appConfig: AppConfig
)(using ec: ExecutionContext)
extends RequestAwareLogging:

  def run(): Future[Unit] =
    given RequestHeader = EmptyRequest.emptyRequestHeader
    (for
      _ <- ifEnabled(appConfig.Scheduler.expiryEnabled, "scheduler.expiry.enabled")(runExpiryStages())
      _ <- ifEnabled(appConfig.Scheduler.deletionEnabled, "scheduler.deletion.enabled")(runDeletionStages())
    yield ()).recover { case ex => logger.error("Error processing application expiry and deletion", ex) }

  def runExpiryStages()(using RequestHeader): Future[Unit] =
    for
      _ <- stage("ApplicationExpiryService.processExpiry")(applicationExpiryService.processExpiry())
      _ <- stage("EmailServiceForExpiryReminders.processEmails")(emailServiceForExpiryReminders.processEmails())
      _ <- stage("EmailServiceForExpiredApplications.processEmails")(emailServiceForExpiredApplications.processEmails())
    yield ()

  def runDeletionStages()(using RequestHeader): Future[Unit] =
    for
      _ <- stage("EmailServiceForDeletedApplications.processEmails")(emailServiceForDeletedApplications.processEmails())
      _ <- stage("AgentApplicationDeletionService.processDeletions")(agentApplicationDeletionService.processDeletions())
    yield ()

  private def ifEnabled(
    enabled: Boolean,
    flagName: String
  )(runStages: => Future[Unit])(using RequestHeader): Future[Unit] =
    if enabled then runStages
    else
      logger.info(s"Stages skipped: $flagName is false")
      Future.successful(())

  private def stage(name: String)(runStage: => Future[Unit])(using RequestHeader): Future[Unit] =
    logger.info(s"Stage started: $name")
    runStage
      .map { _ => logger.info(s"Stage completed: $name") }
      .recover:
        case ex => logger.error(s"Stage failed: $name, continuing with remaining stages", ex)
