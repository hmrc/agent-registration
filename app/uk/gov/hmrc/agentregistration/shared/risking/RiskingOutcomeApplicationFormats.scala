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
import play.api.libs.json.OWrites
import play.api.libs.json.Reads
import uk.gov.hmrc.agentregistration.shared.risking.RiskingOutcomeApplication.Approved
import uk.gov.hmrc.agentregistration.shared.risking.RiskingOutcomeApplication.FailedFixable
import uk.gov.hmrc.agentregistration.shared.risking.RiskingOutcomeApplication.FailedNonFixable
import uk.gov.hmrc.agentregistration.shared.util.JsonConfig

import java.time.LocalDate
import scala.annotation.nowarn

object RiskingOutcomeApplicationFormats:

  @nowarn
  val format: OFormat[RiskingOutcomeApplication] =
    given jsonConfiguration: JsonConfiguration = JsonConfig.jsonConfiguration(discriminator = "outcome")
    given OFormat[Approved] = Json.format[Approved]
    given OFormat[FailedFixable] = Json.format[FailedFixable]
    given OFormat[FailedNonFixable] = Json.format[FailedNonFixable]

    """
    Don't delete me.
    I will emit a warning so `@nowarn` can be applied to address below
    `Unreachable case except for null` problem emited by Play Json macro"""

    Json.format[RiskingOutcomeApplication]
