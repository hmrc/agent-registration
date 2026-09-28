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

package uk.gov.hmrc.agentregistration.repository

import play.api.libs.json.*
import uk.gov.hmrc.agentregistration.testsupport.UnitSpec

import java.time.Instant

class MongoInstantFieldOverridesSpec
extends UnitSpec:

  private val instantValue: Instant = Instant.parse("2026-09-28T13:00:00.500Z")
  private val bsonDateShape: JsObject = Json.obj("$date" -> Json.obj("$numberLong" -> instantValue.toEpochMilli.toString))
  private val isoStringShape: JsString = JsString(instantValue.toString)

  "toMongo rewrites applicationExpiresAt from ISO string to BSON Date shape" in:
    val input: JsObject = Json.obj("applicationExpiresAt" -> isoStringShape)
    (MongoInstantFieldOverrides.toMongo(input) \ "applicationExpiresAt").get shouldBe bsonDateShape

  "toMongo rewrites gracePeriodEndsAt from ISO string to BSON Date shape" in:
    val input: JsObject = Json.obj("gracePeriodEndsAt" -> isoStringShape)
    (MongoInstantFieldOverrides.toMongo(input) \ "gracePeriodEndsAt").get shouldBe bsonDateShape

  "toMongo rewrites both fields when both are present" in:
    val input: JsObject = Json.obj(
      "applicationExpiresAt" -> isoStringShape,
      "gracePeriodEndsAt" -> isoStringShape
    )
    val output: JsObject = MongoInstantFieldOverrides.toMongo(input)
    (output \ "applicationExpiresAt").get shouldBe bsonDateShape
    (output \ "gracePeriodEndsAt").get shouldBe bsonDateShape

  "toMongo leaves unrelated fields untouched" in:
    val input: JsObject = Json.obj(
      "createdAt" -> isoStringShape,
      "applicationState" -> JsString("Started")
    )
    MongoInstantFieldOverrides.toMongo(input) shouldBe input

  "toMongo is a no-op when neither field is present" in:
    val input: JsObject = Json.obj("createdAt" -> isoStringShape)
    MongoInstantFieldOverrides.toMongo(input) shouldBe input

  "fromMongo rewrites applicationExpiresAt from BSON Date shape to ISO string" in:
    val input: JsObject = Json.obj("applicationExpiresAt" -> bsonDateShape)
    (MongoInstantFieldOverrides.fromMongo(input) \ "applicationExpiresAt").get shouldBe JsString(instantValue.toString)

  "fromMongo accepts applicationExpiresAt as a legacy ISO string via the dual-read fallback" in:
    val input: JsObject = Json.obj("applicationExpiresAt" -> isoStringShape)
    (MongoInstantFieldOverrides.fromMongo(input) \ "applicationExpiresAt").get shouldBe JsString(instantValue.toString)

  "fromMongo rewrites gracePeriodEndsAt from BSON Date shape to ISO string" in:
    val input: JsObject = Json.obj("gracePeriodEndsAt" -> bsonDateShape)
    (MongoInstantFieldOverrides.fromMongo(input) \ "gracePeriodEndsAt").get shouldBe JsString(instantValue.toString)

  "fromMongo leaves unrelated fields untouched" in:
    val input: JsObject = Json.obj("createdAt" -> isoStringShape)
    MongoInstantFieldOverrides.fromMongo(input) shouldBe input

  "fromMongo passes through non-JsObject input unchanged" in:
    MongoInstantFieldOverrides.fromMongo(JsNull) shouldBe JsNull
    MongoInstantFieldOverrides.fromMongo(JsString("not an object")) shouldBe JsString("not an object")
    MongoInstantFieldOverrides.fromMongo(JsNumber(42)) shouldBe JsNumber(42)

  "fromMongo(toMongo(x)) is identity for a JsObject holding both fields as ISO strings" in:
    val input: JsObject = Json.obj(
      "applicationExpiresAt" -> isoStringShape,
      "gracePeriodEndsAt" -> isoStringShape,
      "createdAt" -> isoStringShape
    )
    MongoInstantFieldOverrides.fromMongo(MongoInstantFieldOverrides.toMongo(input)) shouldBe input
