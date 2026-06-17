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

class RiskingOutcomeEntitySpec
extends UnitSpec:

  "serialize and deserialize Approved" in:
    val entity: RiskingOutcomeEntity = RiskingOutcomeEntity.Approved
    val json: JsValue = Json.parse("""{"type":"Approved"}""")
    Json.toJson[RiskingOutcomeEntity](entity) shouldBe json
    json.as[RiskingOutcomeEntity] shouldBe entity

  "serialize and deserialize FailedFixable" in:
    val entity: RiskingOutcomeEntity = RiskingOutcomeEntity.FailedFixable(
      fixes = Seq(EntityFix._3.AmlsFix(
        failure = EntityFailure._3._1,
        isConfirmed = None,
        amlsDetails = None
      ))
    )
    val json: JsValue = Json.parse(
      // language=JSON
      """{
        |"type":"FailedFixable",
        |"fixes":[{
        |  "type":"AmlsFix",
        |  "failure":{"type":"_3._1"}
        |}]
        |}""".stripMargin
    )
    Json.toJson[RiskingOutcomeEntity](entity) shouldBe json
    json.as[RiskingOutcomeEntity] shouldBe entity

  "serialize and deserialize FailedNonFixable" in:
    val entity: RiskingOutcomeEntity = RiskingOutcomeEntity.FailedNonFixable(
      failures = Seq(EntityFailure._7)
    )
    val json: JsValue = Json.parse(
      // language=JSON
      """{
        |"type":"FailedNonFixable",
        |"failures":[{"type":"_7"}]
        |}""".stripMargin
    )
    Json.toJson[RiskingOutcomeEntity](entity) shouldBe json
    json.as[RiskingOutcomeEntity] shouldBe entity
