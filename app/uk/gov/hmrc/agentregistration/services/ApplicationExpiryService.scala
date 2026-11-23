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

package uk.gov.hmrc.agentregistration.services

import play.api.mvc.RequestHeader
import uk.gov.hmrc.agentregistration.config.AppConfig
import uk.gov.hmrc.agentregistration.repository.AgentApplicationRepo
import uk.gov.hmrc.agentregistration.util.RequestAwareLogging

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.jdk.DurationConverters.*

@Singleton
class ApplicationExpiryService @Inject() (
  agentApplicationRepo: AgentApplicationRepo,
  appConfig: AppConfig
)(using ExecutionContext)
extends RequestAwareLogging:

  def processExpiry()(using RequestHeader): Future[Unit] =
    logger.info("Expiring unsubmitted applications: Started...")
    agentApplicationRepo
      .updateManyToExpiredForUnsubmitted(appConfig.postExpiryGracePeriod.toJava)
      .map: modifiedCount =>
        logger.info(s"Expiring unsubmitted applications: marked $modifiedCount as Expired DONE")
