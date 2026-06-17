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

class EntityFailureSpec
extends UnitSpec:

  "serialize and deserialize _3._1" in:
    val json: JsValue = Json.parse(
      // language=JSON
      """{"type":"_3._1"}"""
    )
    Json.toJson[EntityFailure](EntityFailure._3._1) shouldBe json
    json.as[EntityFailure] shouldBe EntityFailure._3._1

  "serialize and deserialize _3._2" in:
    val json: JsValue = Json.parse(
      // language=JSON
      """{"type":"_3._2"}"""
    )
    Json.toJson[EntityFailure](EntityFailure._3._2) shouldBe json
    json.as[EntityFailure] shouldBe EntityFailure._3._2

  "serialize and deserialize _3._3" in:
    val json: JsValue = Json.parse(
      // language=JSON
      """{"type":"_3._3"}"""
    )
    Json.toJson[EntityFailure](EntityFailure._3._3) shouldBe json
    json.as[EntityFailure] shouldBe EntityFailure._3._3

  "serialize and deserialize _3._4" in:
    val json: JsValue = Json.parse(
      // language=JSON
      """{"type":"_3._4"}"""
    )
    Json.toJson[EntityFailure](EntityFailure._3._4) shouldBe json
    json.as[EntityFailure] shouldBe EntityFailure._3._4

  "serialize and deserialize _3._5" in:
    val json: JsValue = Json.parse(
      // language=JSON
      """{"type":"_3._5"}"""
    )
    Json.toJson[EntityFailure](EntityFailure._3._5) shouldBe json
    json.as[EntityFailure] shouldBe EntityFailure._3._5

  "serialize and deserialize _4._1" in:
    val json: JsValue = Json.parse(
      // language=JSON
      """{"type":"_4._1"}"""
    )
    Json.toJson[EntityFailure](EntityFailure._4._1) shouldBe json
    json.as[EntityFailure] shouldBe EntityFailure._4._1

  "serialize and deserialize _4._2" in:
    val json: JsValue = Json.parse(
      // language=JSON
      """{"type":"_4._2"}"""
    )
    Json.toJson[EntityFailure](EntityFailure._4._2) shouldBe json
    json.as[EntityFailure] shouldBe EntityFailure._4._2

  "serialize and deserialize _4._3" in:
    val json: JsValue = Json.parse(
      // language=JSON
      """{"type":"_4._3"}"""
    )
    Json.toJson[EntityFailure](EntityFailure._4._3) shouldBe json
    json.as[EntityFailure] shouldBe EntityFailure._4._3

  "serialize and deserialize _4._4" in:
    val json: JsValue = Json.parse(
      // language=JSON
      """{"type":"_4._4"}"""
    )
    Json.toJson[EntityFailure](EntityFailure._4._4) shouldBe json
    json.as[EntityFailure] shouldBe EntityFailure._4._4

  "serialize and deserialize _5._1" in:
    val json: JsValue = Json.parse(
      // language=JSON
      """{"type":"_5._1"}"""
    )
    Json.toJson[EntityFailure](EntityFailure._5._1) shouldBe json
    json.as[EntityFailure] shouldBe EntityFailure._5._1

  "serialize and deserialize _5._2" in:
    val json: JsValue = Json.parse(
      // language=JSON
      """{"type":"_5._2"}"""
    )
    Json.toJson[EntityFailure](EntityFailure._5._2) shouldBe json
    json.as[EntityFailure] shouldBe EntityFailure._5._2

  "serialize and deserialize _5._3" in:
    val json: JsValue = Json.parse(
      // language=JSON
      """{"type":"_5._3"}"""
    )
    Json.toJson[EntityFailure](EntityFailure._5._3) shouldBe json
    json.as[EntityFailure] shouldBe EntityFailure._5._3

  "serialize and deserialize _5._4" in:
    val json: JsValue = Json.parse(
      // language=JSON
      """{"type":"_5._4"}"""
    )
    Json.toJson[EntityFailure](EntityFailure._5._4) shouldBe json
    json.as[EntityFailure] shouldBe EntityFailure._5._4

  "serialize and deserialize _5._5" in:
    val json: JsValue = Json.parse(
      // language=JSON
      """{"type":"_5._5"}"""
    )
    Json.toJson[EntityFailure](EntityFailure._5._5) shouldBe json
    json.as[EntityFailure] shouldBe EntityFailure._5._5

  "serialize and deserialize _5._6" in:
    val json: JsValue = Json.parse(
      // language=JSON
      """{"type":"_5._6"}"""
    )
    Json.toJson[EntityFailure](EntityFailure._5._6) shouldBe json
    json.as[EntityFailure] shouldBe EntityFailure._5._6

  "serialize and deserialize _5._7" in:
    val json: JsValue = Json.parse(
      // language=JSON
      """{"type":"_5._7"}"""
    )
    Json.toJson[EntityFailure](EntityFailure._5._7) shouldBe json
    json.as[EntityFailure] shouldBe EntityFailure._5._7

  "serialize and deserialize _7" in:
    val json: JsValue = Json.parse(
      // language=JSON
      """{"type":"_7"}"""
    )
    Json.toJson[EntityFailure](EntityFailure._7) shouldBe json
    json.as[EntityFailure] shouldBe EntityFailure._7

  "serialize and deserialize _8._1" in:
    val json: JsValue = Json.parse(
      // language=JSON
      """{"type":"_8._1"}"""
    )
    Json.toJson[EntityFailure](EntityFailure._8._1) shouldBe json
    json.as[EntityFailure] shouldBe EntityFailure._8._1

  "serialize and deserialize _8._4" in:
    val json: JsValue = Json.parse(
      // language=JSON
      """{"type":"_8._4"}"""
    )
    Json.toJson[EntityFailure](EntityFailure._8._4) shouldBe json
    json.as[EntityFailure] shouldBe EntityFailure._8._4

  "serialize and deserialize _8._5" in:
    val json: JsValue = Json.parse(
      // language=JSON
      """{"type":"_8._5"}"""
    )
    Json.toJson[EntityFailure](EntityFailure._8._5) shouldBe json
    json.as[EntityFailure] shouldBe EntityFailure._8._5

  "serialize and deserialize _8._6" in:
    val json: JsValue = Json.parse(
      // language=JSON
      """{"type":"_8._6"}"""
    )
    Json.toJson[EntityFailure](EntityFailure._8._6) shouldBe json
    json.as[EntityFailure] shouldBe EntityFailure._8._6

  "serialize and deserialize _8._7" in:
    val json: JsValue = Json.parse(
      // language=JSON
      """{"type":"_8._7"}"""
    )
    Json.toJson[EntityFailure](EntityFailure._8._7) shouldBe json
    json.as[EntityFailure] shouldBe EntityFailure._8._7
