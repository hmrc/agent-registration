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
import uk.gov.hmrc.agentregistration.util.EmptyRequest
import uk.gov.hmrc.agentregistration.util.RequestAwareLogging

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@Singleton
class ApplicationDeletionRunner @Inject() (
  emailServiceForDeletedApplications: EmailServiceForDeletedApplications,
  agentApplicationDeletionService: AgentApplicationDeletionService
)(using ec: ExecutionContext)
extends RequestAwareLogging:

  private val runnerName: String = "ApplicationDeletionRunner"

  def run(): Future[Unit] =
    given RequestHeader = EmptyRequest.emptyRequestHeader
    logger.info(s"$runnerName: Started...")
    (for
      _ <- stage("EmailServiceForDeletedApplications.processEmails")(emailServiceForDeletedApplications.processEmails())
      _ <- stage("AgentApplicationDeletionService.processDeletions")(agentApplicationDeletionService.processDeletions())
    yield logger.info(s"$runnerName: DONE")).recover { case throwable => logger.error(s"$runnerName: Error processing application deletion", throwable) }

  private def stage(name: String)(runStage: => Future[Unit])(using RequestHeader): Future[Unit] =
    logger.info(s"$runnerName: Stage started: $name")
    runStage
      .map { _ => logger.info(s"$runnerName: Stage completed: $name") }
      .recover:
        case throwable => logger.error(s"$runnerName: Stage failed: $name, continuing with remaining stages", throwable)
