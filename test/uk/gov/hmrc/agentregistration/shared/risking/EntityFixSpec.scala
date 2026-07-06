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
import uk.gov.hmrc.agentregistration.testsupport.testdata.TdAll

class EntityFixSpec
extends UnitSpec:

  "serialize and deserialize _3.AmlsFix" in:
    val fix: EntityFix = EntityFix._3.AmlsFix(
      failure = EntityFailure._3._1,
      isConfirmed = None,
      amlsDetails = None
    )
    val json: JsValue = Json.parse(
      // language=JSON
      """{"type":"EntityFix._3.AmlsFix","failure":{"type":"_3._1"}}"""
    )
    Json.toJson[EntityFix](fix) shouldBe json
    json.as[EntityFix] shouldBe fix

  "serialize and deserialize _3.AmlsFix with AmlsDetails" in:
    val fix: EntityFix = EntityFix._3.AmlsFix(
      failure = EntityFailure._3._1,
      isConfirmed = Some(true),
      amlsDetails = Some(TdAll.tdAll.completeAmlsDetailsAtt)
    )
    val json: JsValue = Json.parse(
      // language=JSON
      """{
        |  "failure": {
        |    "type": "_3._1"
        |  },
        |  "isConfirmed": true,
        |  "amlsDetails": {
        |    "supervisoryBody": "ATT",
        |    "amlsRegistrationNumber": "ATT AML-1-123456",
        |    "amlsEvidence": {
        |      "fileUploadReference": "test-file-reference",
        |      "fileName": "evidence.pdf",
        |      "objectStoreLocation": {
        |        "directory": {
        |          "value": "agent-registration-frontend/9d5ddeed-d26e-4005-97ca-e40f2466e0a3"
        |        },
        |        "fileName": "evidence.pdf"
        |      }
        |    }
        |  },
        |  "type": "EntityFix._3.AmlsFix"
        |}
        |""".stripMargin
    )
    Json.toJson[EntityFix](fix) shouldBe json
    json.as[EntityFix] shouldBe fix

  "serialize and deserialize _4._1" in:
    val fix: EntityFix = EntityFix._4._1(isConfirmed = None)
    val json: JsValue = Json.parse(
      // language=JSON
      """{"type":"EntityFix._4._1"}"""
    )
    Json.toJson[EntityFix](fix) shouldBe json

  "serialize and deserialize _4._2" in:
    val fix: EntityFix = EntityFix._4._2(isConfirmed = None)
    val json: JsValue = Json.parse(
      // language=JSON
      """{"type":"EntityFix._4._2"}"""
    )
    Json.toJson[EntityFix](fix) shouldBe json

  "serialize and deserialize _4._3" in:
    val fix: EntityFix = EntityFix._4._3(isConfirmed = None)
    val json: JsValue = Json.parse(
      // language=JSON
      """{"type":"EntityFix._4._3"}"""
    )
    Json.toJson[EntityFix](fix) shouldBe json

  "serialize and deserialize _4._4" in:
    val fix: EntityFix = EntityFix._4._4(isConfirmed = None)
    val json: JsValue = Json.parse(
      // language=JSON
      """{"type":"EntityFix._4._4"}"""
    )
    Json.toJson[EntityFix](fix) shouldBe json

  "serialize and deserialize _5._1" in:
    val fix: EntityFix = EntityFix._5._1(isConfirmed = None)
    val json: JsValue = Json.parse(
      // language=JSON
      """{"type":"EntityFix._5._1"}"""
    )
    Json.toJson[EntityFix](fix) shouldBe json
    json.as[EntityFix] shouldBe fix

  "serialize and deserialize _5._2" in:
    val fix: EntityFix = EntityFix._5._2(isConfirmed = None)
    val json: JsValue = Json.parse(
      // language=JSON
      """{"type":"EntityFix._5._2"}"""
    )
    Json.toJson[EntityFix](fix) shouldBe json
    json.as[EntityFix] shouldBe fix

  "serialize and deserialize _5._3" in:
    val fix: EntityFix = EntityFix._5._3(isConfirmed = None)
    val json: JsValue = Json.parse(
      // language=JSON
      """{"type":"EntityFix._5._3"}"""
    )
    Json.toJson[EntityFix](fix) shouldBe json
    json.as[EntityFix] shouldBe fix

  "serialize and deserialize _5._4" in:
    val fix: EntityFix = EntityFix._5._4(isConfirmed = None)
    val json: JsValue = Json.parse(
      // language=JSON
      """{"type":"EntityFix._5._4"}"""
    )
    Json.toJson[EntityFix](fix) shouldBe json
    json.as[EntityFix] shouldBe fix

  "serialize and deserialize _5._5" in:
    val fix: EntityFix = EntityFix._5._5(isConfirmed = None)
    val json: JsValue = Json.parse(
      // language=JSON
      """{"type":"EntityFix._5._5"}"""
    )
    Json.toJson[EntityFix](fix) shouldBe json

  "serialize and deserialize _5._6" in:
    val fix: EntityFix = EntityFix._5._6(isConfirmed = None)
    val json: JsValue = Json.parse(
      // language=JSON
      """{"type":"EntityFix._5._6"}"""
    )
    Json.toJson[EntityFix](fix) shouldBe json
    json.as[EntityFix] shouldBe fix

  "serialize and deserialize _5._7" in:
    val fix: EntityFix = EntityFix._5._7(isConfirmed = None)
    val json: JsValue = Json.parse(
      // language=JSON
      """{"type":"EntityFix._5._7"}"""
    )
    Json.toJson[EntityFix](fix) shouldBe json

  "serialize and deserialize _8._5" in:
    val fix: EntityFix = EntityFix._8._5(isConfirmed = None)
    val json: JsValue = Json.parse(
      // language=JSON
      """{"type":"EntityFix._8._5"}"""
    )
    Json.toJson[EntityFix](fix) shouldBe json
    json.as[EntityFix] shouldBe fix

  "serialize and deserialize _8._7" in:
    val fix: EntityFix = EntityFix._8._7(isConfirmed = None)
    val json: JsValue = Json.parse(
      // language=JSON
      """{"type":"EntityFix._8._7"}"""
    )
    Json.toJson[EntityFix](fix) shouldBe json
    json.as[EntityFix] shouldBe fix
