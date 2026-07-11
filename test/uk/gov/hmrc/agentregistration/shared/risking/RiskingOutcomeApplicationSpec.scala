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

import play.api.libs.json.JsValue
import play.api.libs.json.Json
import uk.gov.hmrc.agentregistration.testsupport.UnitSpec

import java.time.Instant
import java.time.LocalDate

class RiskingOutcomeApplicationSpec
extends UnitSpec:

  "serialize and deserialize RiskingOutcomeApplication: Approved" in:
    val riskingOutcomeApplication: RiskingOutcomeApplication = RiskingOutcomeApplication.Approved(
      actualDecisionDate = LocalDate.of(2024, 1, 15)
    )
    val json: JsValue = Json.parse(
      // language=JSON
      """{
        |"outcome":"Approved",
        |"actualDecisionDate":"2024-01-15"
        |}""".stripMargin
    )
    Json.toJson(riskingOutcomeApplication) shouldBe json
    json.as[RiskingOutcomeApplication] shouldBe riskingOutcomeApplication

  "serialize and deserialize RiskingOutcomeApplication: FailedFixable" in:
    val riskingOutcomeApplication: RiskingOutcomeApplication = RiskingOutcomeApplication.FailedFixable(
      actualDecisionDate = LocalDate.of(2024, 1, 15),
      correctiveActionExpiryDate = LocalDate.of(2024, 2, 29),
      reSubmittedAt = Some(Instant.parse("2024-06-06T12:00:00Z"))
    )
    val json: JsValue = Json.parse(
      // language=JSON
      """{
        |"outcome":"FailedFixable",
        |"actualDecisionDate":"2024-01-15",
        |"correctiveActionExpiryDate": "2024-02-29",
        |"reSubmittedAt": "2024-06-06T12:00:00Z"
        |}""".stripMargin
    )
    Json.toJson(riskingOutcomeApplication) shouldBe json
    json.as[RiskingOutcomeApplication] shouldBe riskingOutcomeApplication

  "serialize and deserialize RiskingOutcomeApplication: FailedNonFixable" in:
    val riskingOutcomeApplication: RiskingOutcomeApplication = RiskingOutcomeApplication.FailedNonFixable(
      actualDecisionDate = LocalDate.of(2024, 1, 15),
      correctiveActionExpiryDate = LocalDate.of(2024, 2, 29)
    )
    val json: JsValue = Json.parse(
      // language=JSON
      """{
        |"outcome":"FailedNonFixable",
        |"actualDecisionDate":"2024-01-15",
        |"correctiveActionExpiryDate": "2024-02-29"
        |}""".stripMargin
    )
    Json.toJson(riskingOutcomeApplication) shouldBe json
    json.as[RiskingOutcomeApplication] shouldBe riskingOutcomeApplication
