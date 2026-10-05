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
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats

import java.time.Instant
import scala.util.chaining.scalaUtilChainingOps

/** Field-selective override that stores `applicationExpiresAt` of [[uk.gov.hmrc.agentregistration.shared.AgentApplication]] as BSON `Date` in Mongo, so it can
  * be compared as a date rather than an ISO string. The rest of the JSON contract (and every other Instant field) is untouched.
  *
  * Writes always emit BSON `Date`. Reads accept BSON `Date` (migrated records) or legacy ISO string (records written before this change), so the service stays
  * correct during a rolling deploy and until [[AgentApplicationExpiresAtMigration]] has run. Drop the legacy branch once every environment reports zero
  * string-typed records.
  */
object MongoInstantFieldOverrides:

  private val bsonDateInstantFormat: Format[Instant] = MongoJavatimeFormats.instantFormat
  private val isoStringInstantFormat: Format[Instant] = Format(Reads.DefaultInstantReads, Writes.DefaultInstantWrites)
  private val dualShapeInstantReads: Reads[Instant] = bsonDateInstantFormat.orElse(isoStringInstantFormat)

  def toMongo(jsObject: JsObject): JsObject = jsObject
    .pipe(rewriteInstantField(
      _,
      "applicationExpiresAt",
      isoStringInstantFormat,
      bsonDateInstantFormat
    ))

  def fromMongo(jsValue: JsValue): JsValue =
    jsValue match
      case jsObject: JsObject =>
        jsObject
          .pipe(rewriteInstantField(
            _,
            "applicationExpiresAt",
            dualShapeInstantReads,
            isoStringInstantFormat
          ))
      case other => other

  private def rewriteInstantField(
    jsObject: JsObject,
    fieldName: String,
    readWith: Reads[Instant],
    writeWith: Writes[Instant]
  ): JsObject =
    (jsObject \ fieldName).asOpt[Instant](using readWith) match
      case Some(instantValue) => jsObject + (fieldName -> writeWith.writes(instantValue))
      case None => jsObject
