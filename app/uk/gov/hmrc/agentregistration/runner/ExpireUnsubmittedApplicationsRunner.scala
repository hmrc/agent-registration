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
import uk.gov.hmrc.agentregistration.config.AppConfig
import uk.gov.hmrc.agentregistration.repository.AgentApplicationRepo

import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.jdk.DurationConverters.*

@Singleton
class ExpireUnsubmittedApplicationsRunner @Inject() (
  agentApplicationRepo: AgentApplicationRepo,
  appConfig: AppConfig,
  clock: Clock
)(using ec: ExecutionContext)
extends Logging:

  def run(): Future[Unit] =
    val now: Instant = Instant.now(clock)
    val gracePeriodEndsAt: Instant = now.plus(appConfig.postExpiryGracePeriod.toJava)
    logger.info(s"Marking unsubmitted applications with applicationExpiresAt < $now as Expired, gracePeriodEndsAt = $gracePeriodEndsAt")
    agentApplicationRepo.updateManyToExpiredForUnsubmitted(now, gracePeriodEndsAt).map { modifiedCount =>
      logger.info(s"Marked $modifiedCount unsubmitted applications as Expired")
    }
