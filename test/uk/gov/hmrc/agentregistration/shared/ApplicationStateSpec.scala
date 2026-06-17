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

package uk.gov.hmrc.agentregistration.shared

import play.api.libs.json.JsValue
import play.api.libs.json.Json
import uk.gov.hmrc.agentregistration.testsupport.UnitSpec

class ApplicationStateSpec
extends UnitSpec:

  "serialize and deserialize" in:
    ApplicationState.values.map:
      case ApplicationState.Started =>
        val json: JsValue = Json.parse(""""Started"""")
        Json.toJson[ApplicationState](ApplicationState.Started) shouldBe json
        json.as[ApplicationState] shouldBe ApplicationState.Started
      case ApplicationState.GrsDataReceived =>
        val json: JsValue = Json.parse(""""GrsDataReceived"""")
        Json.toJson[ApplicationState](ApplicationState.GrsDataReceived) shouldBe json
        json.as[ApplicationState] shouldBe ApplicationState.GrsDataReceived
      case ApplicationState.SentForRisking =>
        val json: JsValue = Json.parse(""""SentForRisking"""")
        Json.toJson[ApplicationState](ApplicationState.SentForRisking) shouldBe json
        json.as[ApplicationState] shouldBe ApplicationState.SentForRisking
      case ApplicationState.RiskingCompleted =>
        val json: JsValue = Json.parse(""""RiskingCompleted"""")
        Json.toJson[ApplicationState](ApplicationState.RiskingCompleted) shouldBe json
        json.as[ApplicationState] shouldBe ApplicationState.RiskingCompleted
