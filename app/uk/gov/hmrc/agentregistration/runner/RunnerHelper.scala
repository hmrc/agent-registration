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
import uk.gov.hmrc.agentregistration.util.EmptyRequest
import uk.gov.hmrc.agentregistration.util.RequestAwareLogging

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

object RunnerHelper
extends RequestAwareLogging:

  def run(
    runnerName: String,
    stages: Seq[Stage]
  )(using ExecutionContext): Future[Unit] =
    given RequestHeader = EmptyRequest.emptyRequestHeader
    logger.info(s"$runnerName: Started...")
    stages
      .foldLeft(Future.unit):
        (
          previousStages,
          stage
        ) =>
          previousStages.flatMap(_ => runStage(runnerName, stage))
      .map(_ => logger.info(s"$runnerName: DONE"))

  private def runStage(
    runnerName: String,
    stage: Stage
  )(using
    RequestHeader,
    ExecutionContext
  ): Future[Unit] =
    logger.info(s"$runnerName: Stage started: ${stage.name}")
    Future
      .delegate(stage.run)
      .map(_ => logger.info(s"$runnerName: Stage completed: ${stage.name}"))
      .recover:
        case throwable => logger.error(s"$runnerName: Stage failed: ${stage.name}, continuing with remaining stages", throwable)
