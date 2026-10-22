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
      case ApplicationState.SentToMinerva =>
        val json: JsValue = Json.parse(""""SentToMinerva"""")
        Json.toJson[ApplicationState](ApplicationState.SentToMinerva) shouldBe json
        json.as[ApplicationState] shouldBe ApplicationState.SentToMinerva
      case ApplicationState.RiskingCompleted =>
        val json: JsValue = Json.parse(""""RiskingCompleted"""")
        Json.toJson[ApplicationState](ApplicationState.RiskingCompleted) shouldBe json
        json.as[ApplicationState] shouldBe ApplicationState.RiskingCompleted
      case ApplicationState.Expired =>
        val json: JsValue = Json.parse(""""Expired"""")
        Json.toJson[ApplicationState](ApplicationState.Expired) shouldBe json
        json.as[ApplicationState] shouldBe ApplicationState.Expired

  "canTransitionTo has an explicitly defined set of next states for every ApplicationState - a new state MUST update the match below" in:
    ApplicationState.values.foreach: state =>
      val expectedNextStates: Set[ApplicationState] =
        state match
          case ApplicationState.Started =>
            Set(
              ApplicationState.Started,
              ApplicationState.GrsDataReceived,
              ApplicationState.Expired
            )
          case ApplicationState.GrsDataReceived =>
            Set(
              ApplicationState.GrsDataReceived,
              ApplicationState.SentForRisking,
              ApplicationState.Expired
            )
          case ApplicationState.SentForRisking =>
            Set(
              ApplicationState.SentForRisking,
              ApplicationState.SentToMinerva,
              ApplicationState.RiskingCompleted
            )
          case ApplicationState.SentToMinerva => Set(ApplicationState.SentToMinerva, ApplicationState.RiskingCompleted)
          case ApplicationState.RiskingCompleted =>
            Set(
              ApplicationState.RiskingCompleted,
              ApplicationState.SentForRisking,
              ApplicationState.Expired
            )
          case ApplicationState.Expired => Set.empty
      ApplicationState.values.foreach: nextState =>
        state.canTransitionTo(nextState) shouldBe expectedNextStates.contains(nextState) withClue s"transition $state -> $nextState"

  "allowedPreviousStates lists every state that can transition into the given state" in:
    ApplicationState.allowedPreviousStates(ApplicationState.Started) shouldBe Set(ApplicationState.Started)
    ApplicationState.allowedPreviousStates(ApplicationState.GrsDataReceived) shouldBe Set(ApplicationState.Started, ApplicationState.GrsDataReceived)
    ApplicationState.allowedPreviousStates(ApplicationState.SentForRisking) shouldBe Set(
      ApplicationState.GrsDataReceived,
      ApplicationState.SentForRisking,
      ApplicationState.RiskingCompleted
    )
    ApplicationState.allowedPreviousStates(ApplicationState.SentToMinerva) shouldBe Set(ApplicationState.SentForRisking, ApplicationState.SentToMinerva)
    ApplicationState.allowedPreviousStates(ApplicationState.RiskingCompleted) shouldBe Set(
      ApplicationState.SentForRisking,
      ApplicationState.SentToMinerva,
      ApplicationState.RiskingCompleted
    )
    ApplicationState.allowedPreviousStates(ApplicationState.Expired) shouldBe Set(
      ApplicationState.Started,
      ApplicationState.GrsDataReceived,
      ApplicationState.RiskingCompleted
    )

  "Expired is the only terminal state" in:
    ApplicationState.values.filter(_.isTerminal).toSet shouldBe Set(ApplicationState.Expired)

  "initialState is Started" in:
    ApplicationState.initialState shouldBe ApplicationState.Started
