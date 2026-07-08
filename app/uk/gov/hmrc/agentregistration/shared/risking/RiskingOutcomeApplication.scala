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

import play.api.libs.json.Format
import play.api.libs.json.Json
import play.api.libs.json.OFormat
import play.api.libs.json.OWrites
import play.api.libs.json.Reads
import uk.gov.hmrc.agentregistration.shared.util.JsonFormatsFactory

import java.time.LocalDate

final case class RiskingOutcomeApplication(
  actualDecisionDate: LocalDate,
  outcome: RiskingOutcomeApplication.Outcome,
  correctiveActionExpiryDate: Option[LocalDate] // this is populated only if the outcome is FailedFixable
)

object RiskingOutcomeApplication:

  enum Outcome:

    case Approved
    case FailedFixable
    case FailedNonFixable

  object Outcome:
    given Format[Outcome] = JsonFormatsFactory.makeEnumFormat[Outcome]

  private final case class RiskingOutcomeApplicationLegacy(
    riskingCompletedDate: LocalDate,
    outcome: RiskingOutcomeApplication.Outcome,
    correctiveActionExpiryDate: Option[LocalDate] // this is populated only if the outcome is FailedFixable
  )

  given OFormat[RiskingOutcomeApplication] =

    val legacyReads: Reads[RiskingOutcomeApplication] = Json
      .reads[RiskingOutcomeApplicationLegacy]
      .map(x =>
        RiskingOutcomeApplication(
          actualDecisionDate = x.riskingCompletedDate,
          outcome = x.outcome,
          correctiveActionExpiryDate = x.correctiveActionExpiryDate
        )
      )
    val reads: Reads[RiskingOutcomeApplication] = Json.reads[RiskingOutcomeApplication].orElse(legacyReads)
    val writes: OWrites[RiskingOutcomeApplication] = Json.writes[RiskingOutcomeApplication]

    OFormat(reads, writes)
