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
import uk.gov.hmrc.agentregistration.shared.util.JsonConfig
import scala.annotation.nowarn

import java.time.LocalDate

/** Represents the risking results for an agent application along with all individuals in the application.
  *
  * The risking follows the heuristic that one spoiled apple makes a spoiled basket - a single failure can impact the overall application status.
  */
sealed trait ApplicationRiskingResponse

object ApplicationRiskingResponse:

  /** Indicates that the application was accepted by the risking microservice, and not yet submitted for risking at Minerva
    */
  case object ReadyForSubmission
  extends ApplicationRiskingResponse

  /** Indicates that the application has been submitted for risking and is awaiting results. The application is currently being processed by the risking system.
    */
  case object SubmittedForRisking
  extends ApplicationRiskingResponse

  /** Represents states where risking results for all Applications and Individuals have been received from the risking system. These are terminal states for
    * this round that indicate the outcome of the risking process.
    */
  sealed trait ReceivedRiskingResults
  extends ApplicationRiskingResponse

  /** Represents a risking outcome with at least one FIXABLE failure, but without NON FIXABLE failures.
    */
  final case class FailedFixable(
    riskedEntity: RiskedEntity,
    riskedIndividuals: List[RiskedIndividual],
    riskingCompletedDate: LocalDate
  )
  extends ReceivedRiskingResults

  /** Represents a risking outcome with at least one NON FIXABLE failure which makes entire application Failed Non Fixable.
    */
  final case class FailedNonFixable(
    riskedEntity: RiskedEntity,
    riskedIndividuals: List[RiskedIndividual],
    riskingCompletedDate: LocalDate
  )
  extends ReceivedRiskingResults

  @nowarn()
  given format: OFormat[ApplicationRiskingResponse] =
    given JsonConfiguration = JsonConfig.jsonConfiguration
    // Note: using implicit val instead of given due to Scala compiler bug with given and Play JSON macros

    given OFormat[ApplicationRiskingResponse.ReadyForSubmission.type] = Json.format[ApplicationRiskingResponse.ReadyForSubmission.type]
    given OFormat[ApplicationRiskingResponse.SubmittedForRisking.type] = Json.format[ApplicationRiskingResponse.SubmittedForRisking.type]
    given OFormat[ApplicationRiskingResponse.FailedNonFixable] = Json.format[ApplicationRiskingResponse.FailedNonFixable]
    given OFormat[ApplicationRiskingResponse.FailedFixable] = Json.format[ApplicationRiskingResponse.FailedFixable]

    val dontDeleteMe = """
        |Don't delete me.
        |I will emit a warning so `@nowarn` can be applied to address below
        |`Unreachable case except for null` problem emited by Play Json macro"""

    Json.format[ApplicationRiskingResponse]
