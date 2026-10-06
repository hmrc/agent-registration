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

import play.api.libs.json.JsObject
import play.api.libs.json.Json
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetails
import uk.gov.hmrc.agentregistration.testsupport.UnitSpec
import uk.gov.hmrc.agentregistration.testsupport.testdata.TdAll

import java.time.Instant

class IndividualProvidedDetailsMongoFormatSpec
extends UnitSpec:

  private val tdAll: TdAll = TdAll()

  // the date under test, set explicitly so the expected JSON below does not depend on the shared test data
  private val createdAt: Instant = Instant.parse("2026-03-04T05:06:07.890Z")

  private val expectedRestDates: JsObject = Json.obj("createdAt" -> "2026-03-04T05:06:07.890Z")
  private val expectedMongoDates: JsObject = Json.obj("createdAt" -> Json.obj("$date" -> Json.obj("$numberLong" -> createdAt.toEpochMilli.toString)))

  private val individual: IndividualProvidedDetails = tdAll.providedDetails.afterFinished.copy(createdAt = createdAt)
  private val restJson: JsObject = IndividualProvidedDetails.format.writes(individual)
  private val mongoJson: JsObject = IndividualProvidedDetailsMongoFormat.format.writes(individual)

  "REST JSON holds createdAt as an ISO string" in:
    restJson shouldBe restJson.deepMerge(expectedRestDates)

  "Mongo JSON is the REST JSON with exactly createdAt replaced by a BSON Date" in:
    mongoJson shouldBe restJson.deepMerge(expectedMongoDates)

  "Mongo format reads back what it wrote" in:
    IndividualProvidedDetailsMongoFormat.format.reads(mongoJson).get shouldBe individual

  "Mongo format reads createdAt stored as a legacy ISO string" in:
    IndividualProvidedDetailsMongoFormat.format.reads(mongoJson.deepMerge(expectedRestDates)).get shouldBe individual
