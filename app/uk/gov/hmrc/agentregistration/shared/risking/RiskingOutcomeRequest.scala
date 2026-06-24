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
import play.api.libs.json.OFormat
import uk.gov.hmrc.agentregistration.shared.PersonReference

final case class RiskingOutcomeRequest(
  riskingOutcomeApplication: RiskingOutcomeApplication,
  riskingOutcomeEntity: RiskingOutcomeEntity,
  individualOutcomes: Seq[IndividualOutcome]
)

object RiskingOutcomeRequest:
  given OFormat[RiskingOutcomeRequest] = Json.format[RiskingOutcomeRequest]

final case class IndividualOutcome(
  personReference: PersonReference,
  riskingOutcomeIndividual: RiskingOutcomeIndividual
)

object IndividualOutcome:
  given OFormat[IndividualOutcome] = Json.format[IndividualOutcome]
