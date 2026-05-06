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

package uk.gov.hmrc.agentregistration.shared.risking

import play.api.libs.json.Json
import play.api.libs.json.JsonConfiguration
import play.api.libs.json.OFormat
import uk.gov.hmrc.agentregistration.shared.risking.RiskingProgress.ReceivedRiskingResults
import uk.gov.hmrc.agentregistration.shared.util.JsonConfig

import scala.annotation.nowarn
import java.time.LocalDate

/** Represents the risking progress for application results for an agent application along with all individuals in the application.
  *
  * The risking follows the heuristic that one spoiled apple makes a spoiled basket - a single failure can impact the overall application status.
  */
sealed trait RiskingProgress

object RiskingProgress:

  /** Indicates that the application was accepted by the risking microservice, and not yet submitted for risking at Minerva
    */
  case object ReadyForSubmission
  extends RiskingProgress

  /** Indicates that the application has been submitted for risking and is awaiting results. The application is currently being processed by the risking system.
    */
  case object SubmittedForRisking
  extends RiskingProgress

  /** Represents states where risking results for all Applications and Individuals have been received from the risking system. These are terminal states for
    * this round that indicate the outcome of the risking process.
    */
  sealed trait ReceivedRiskingResults
  extends RiskingProgress

  /** Represents a risking outcome with at least one FIXABLE failure, but without NON FIXABLE failures.
    */
  final case class FailedFixable(
    riskedEntity: RiskedEntity,
    riskedIndividuals: Seq[RiskedIndividual],
    riskingCompletedDate: LocalDate
  )
  extends ReceivedRiskingResults

  /** Represents a risking outcome with at least one NON FIXABLE failure which makes entire application Failed Non Fixable.
    */
  final case class FailedNonFixable(
    riskedEntity: RiskedEntity,
    riskedIndividuals: Seq[RiskedIndividual],
    riskingCompletedDate: LocalDate
  )
  extends ReceivedRiskingResults

  object Approved
  extends ReceivedRiskingResults

  @nowarn()
  given format: OFormat[RiskingProgress] =
    given JsonConfiguration = JsonConfig.jsonConfiguration
    // Note: using implicit val instead of given due to Scala compiler bug with given and Play JSON macros

    given OFormat[RiskingProgress.ReadyForSubmission.type] = Json.format[RiskingProgress.ReadyForSubmission.type]

    given OFormat[
      RiskingProgress.SubmittedForRisking.type
    ] = Json.format[RiskingProgress.SubmittedForRisking.type]
    given OFormat[RiskingProgress.Approved.type] = Json.format[RiskingProgress.Approved.type]
    given OFormat[RiskingProgress.FailedNonFixable] = Json.format[RiskingProgress.FailedNonFixable]
    given OFormat[RiskingProgress.FailedFixable] = Json.format[RiskingProgress.FailedFixable]

    val dontDeleteMe = """
        |Don't delete me.
        |I will emit a warning so `@nowarn` can be applied to address below
        |`Unreachable case except for null` problem emited by Play Json macro"""

    Json.format[RiskingProgress]
