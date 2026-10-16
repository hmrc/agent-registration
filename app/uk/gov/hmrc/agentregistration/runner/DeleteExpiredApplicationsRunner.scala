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

import play.api.Logging
import play.api.mvc.RequestHeader
import uk.gov.hmrc.agentregistration.repository.AgentApplicationRepo
import uk.gov.hmrc.agentregistration.services.AgentApplicationDeletionService
import uk.gov.hmrc.agentregistration.shared.AgentApplicationId
import uk.gov.hmrc.agentregistration.util.ProcessInSequence
import uk.gov.hmrc.agentregistration.util.EmptyRequest

import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@Singleton
class DeleteExpiredApplicationsRunner @Inject() (
  agentApplicationRepo: AgentApplicationRepo,
  agentApplicationDeletionService: AgentApplicationDeletionService,
  clock: Clock
)(using ec: ExecutionContext)
extends Logging:

  def run(): Future[Unit] =
    given RequestHeader = EmptyRequest.emptyRequestHeader
    val now: Instant = Instant.now(clock)
    logger.info(s"Deleting Expired applications: Started... (postExpiryGracePeriodEndsAt < $now)")
    for
      agentApplicationIds <- agentApplicationRepo.findIdsOfExpiredWithPostExpiryGracePeriodEnded(now)
      deletedCount <-
        ProcessInSequence.processAllInSequence(agentApplicationIds)(deleteExpiredApplication):
          (
            throwable: Throwable,
            agentApplicationId: AgentApplicationId
          ) =>
            logger.error(s"Deleting Expired applications: FAILED for ${agentApplicationId.value}", throwable)
    yield logger.info(s"Deleting Expired applications: deleted $deletedCount of ${agentApplicationIds.size} DONE")

  private def deleteExpiredApplication(agentApplicationId: AgentApplicationId)(using request: RequestHeader): Future[Unit] = agentApplicationRepo
    .findById(agentApplicationId)
    .flatMap:
      case Some(agentApplication) => agentApplicationDeletionService.deleteWithIndividualsAndAmlsEvidence(agentApplication)
      case None => Future.successful(())
