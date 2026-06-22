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
import uk.gov.hmrc.agentregistration.shared.Nino
import uk.gov.hmrc.agentregistration.shared.SaUtr
import uk.gov.hmrc.agentregistration.shared.individual.IndividualDateOfBirth
import uk.gov.hmrc.agentregistration.shared.individual.IndividualNino
import uk.gov.hmrc.agentregistration.shared.individual.IndividualSaUtr
import uk.gov.hmrc.agentregistration.testsupport.UnitSpec

import java.time.LocalDate

class IndividualFixSpec
extends UnitSpec:

  "serialize and deserialize _4._1" in:
    val fix: IndividualFix = IndividualFix._4._1(isConfirmed = None)
    val json: JsValue = Json.parse(
      // language=JSON
      """{"type":"IndividualFix._4._1"}"""
    )
    Json.toJson[IndividualFix](fix) shouldBe json
    json.as[IndividualFix] shouldBe fix

  "serialize and deserialize _4._3" in:
    val fix: IndividualFix = IndividualFix._4._3(isConfirmed = None)
    val json: JsValue = Json.parse(
      // language=JSON
      """{"type":"IndividualFix._4._3"}"""
    )
    Json.toJson[IndividualFix](fix) shouldBe json
    json.as[IndividualFix] shouldBe fix

  "serialize and deserialize _4._4" in:
    val fix: IndividualFix = IndividualFix._4._4(isConfirmed = None)
    val json: JsValue = Json.parse(
      // language=JSON
      """{"type":"IndividualFix._4._4"}"""
    )
    Json.toJson[IndividualFix](fix) shouldBe json
    json.as[IndividualFix] shouldBe fix

  "serialize and deserialize _5._1" in:
    val fix: IndividualFix = IndividualFix._5._1(isConfirmed = None)
    val json: JsValue = Json.parse(
      // language=JSON
      """{"type":"IndividualFix._5._1"}"""
    )
    Json.toJson[IndividualFix](fix) shouldBe json
    json.as[IndividualFix] shouldBe fix

  "serialize and deserialize _5._3" in:
    val fix: IndividualFix = IndividualFix._5._3(isConfirmed = None)
    val json: JsValue = Json.parse(
      // language=JSON
      """{"type":"IndividualFix._5._3"}"""
    )
    Json.toJson[IndividualFix](fix) shouldBe json
    json.as[IndividualFix] shouldBe fix

  "serialize and deserialize _5._4" in:
    val fix: IndividualFix = IndividualFix._5._4(isConfirmed = None)
    val json: JsValue = Json.parse(
      // language=JSON
      """{"type":"IndividualFix._5._4"}"""
    )
    Json.toJson[IndividualFix](fix) shouldBe json
    json.as[IndividualFix] shouldBe fix

  "serialize and deserialize _5._5" in:
    val fix: IndividualFix = IndividualFix._5._5(isConfirmed = None)
    val json: JsValue = Json.parse(
      // language=JSON
      """{"type":"IndividualFix._5._5"}"""
    )
    Json.toJson[IndividualFix](fix) shouldBe json
    json.as[IndividualFix] shouldBe fix

  "serialize and deserialize _5._6" in:
    val fix: IndividualFix = IndividualFix._5._6(isConfirmed = None)
    val json: JsValue = Json.parse(
      // language=JSON
      """{"type":"IndividualFix._5._6"}"""
    )
    Json.toJson[IndividualFix](fix) shouldBe json
    json.as[IndividualFix] shouldBe fix

  "serialize and deserialize _5._7" in:
    val fix: IndividualFix = IndividualFix._5._7(isConfirmed = None)
    val json: JsValue = Json.parse(
      // language=JSON
      """{"type":"IndividualFix._5._7"}"""
    )
    Json.toJson[IndividualFix](fix) shouldBe json
    json.as[IndividualFix] shouldBe fix

  "serialize and deserialize _8._7" in:
    val fix: IndividualFix = IndividualFix._8._7(isConfirmed = None)
    val json: JsValue = Json.parse(
      // language=JSON
      """{"type":"IndividualFix._8._7"}"""
    )
    Json.toJson[IndividualFix](fix) shouldBe json
    json.as[IndividualFix] shouldBe fix

  "serialize and deserialize _10.IndividualDetailsFix" in:
    val fix: IndividualFix = IndividualFix._10.IndividualDetailsFix(
      dateOfBirth = None,
      saUtr = None,
      nino = None,
      isConfirmed = None
    )
    val json: JsValue = Json.parse(
      // language=JSON
      """{"type":"IndividualFix._10.IndividualDetailsFix"}"""
    )
    Json.toJson[IndividualFix](fix) shouldBe json
    json.as[IndividualFix] shouldBe fix

  "serialize and deserialize _10.IndividualDetailsFix with all fields" in:
    val fix: IndividualFix = IndividualFix._10.IndividualDetailsFix(
      dateOfBirth = Some(IndividualDateOfBirth.Provided(LocalDate.of(2000, 1, 1))),
      saUtr = Some(IndividualSaUtr.Provided(SaUtr("1234567890"))),
      nino = Some(IndividualNino.Provided(Nino("AA123456A"))),
      isConfirmed = Some(true)
    )
    val json: JsValue = Json.parse(
      // language=JSON
      """{
        |  "type": "IndividualFix._10.IndividualDetailsFix",
        |  "dateOfBirth": {
        |    "dateOfBirth": "2000-01-01"
        |  },
        |  "saUtr": {
        |    "saUtr": "1234567890"
        |  },
        |  "nino": {
        |    "nino": "AA123456A"
        |  },
        |  "isConfirmed": true
        |}""".stripMargin
    )
    Json.toJson[IndividualFix](fix) shouldBe json
    json.as[IndividualFix] shouldBe fix
