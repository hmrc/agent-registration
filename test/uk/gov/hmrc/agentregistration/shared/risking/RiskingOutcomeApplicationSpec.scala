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

import java.time.LocalDate

class RiskingOutcomeApplicationSpec
extends UnitSpec:

  "serialize and deserialize RiskingOutcomeApplication" in:
    val riskingOutcomeApplication: RiskingOutcomeApplication = RiskingOutcomeApplication(
      riskingCompletedDate = LocalDate.of(2024, 1, 15),
      outcome = RiskingOutcomeApplication.Outcome.Approved,
      correctiveActionExpiryDate = None
    )
    val json: JsValue = Json.parse(
      // language=JSON
      """{
        |"riskingCompletedDate":"2024-01-15",
        |"outcome":"Approved"
        |}""".stripMargin
    )
    Json.toJson(riskingOutcomeApplication) shouldBe json
    json.as[RiskingOutcomeApplication] shouldBe riskingOutcomeApplication

  "serialize and deserialize RiskingOutcomeApplication.Outcome" in:
    RiskingOutcomeApplication.Outcome.values.foreach:
      case RiskingOutcomeApplication.Outcome.Approved =>
        val json: JsValue = Json.parse(""""Approved"""")
        Json.toJson[RiskingOutcomeApplication.Outcome](RiskingOutcomeApplication.Outcome.Approved) shouldBe json
        json.as[RiskingOutcomeApplication.Outcome] shouldBe RiskingOutcomeApplication.Outcome.Approved
      case RiskingOutcomeApplication.Outcome.FailedFixable =>
        val json: JsValue = Json.parse(""""FailedFixable"""")
        Json.toJson[RiskingOutcomeApplication.Outcome](RiskingOutcomeApplication.Outcome.FailedFixable) shouldBe json
        json.as[RiskingOutcomeApplication.Outcome] shouldBe RiskingOutcomeApplication.Outcome.FailedFixable
      case RiskingOutcomeApplication.Outcome.FailedNonFixable =>
        val json: JsValue = Json.parse(""""FailedNonFixable"""")
        Json.toJson[RiskingOutcomeApplication.Outcome](RiskingOutcomeApplication.Outcome.FailedNonFixable) shouldBe json
        json.as[RiskingOutcomeApplication.Outcome] shouldBe RiskingOutcomeApplication.Outcome.FailedNonFixable
