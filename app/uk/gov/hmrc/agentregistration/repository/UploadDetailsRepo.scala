/*
 * Copyright 2025 HM Revenue & Customs
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

import org.bson.types.ObjectId
import org.mongodb.scala.model.Filters.equal
import org.mongodb.scala.model.Updates.set
import org.mongodb.scala.model.FindOneAndUpdateOptions
import org.mongodb.scala.model.IndexModel
import org.mongodb.scala.model.IndexOptions
import org.mongodb.scala.model.Indexes
import org.mongodb.scala.SingleObservableFuture
import play.api.libs.functional.syntax.*
import play.api.libs.json.*
import play.api.libs.json.OFormat.oFormatFromReadsAndOWrites
import uk.gov.hmrc.agentregistration.shared.upscan.FileUploadReference
import uk.gov.hmrc.agentregistration.shared.upscan.UploadDetails
import uk.gov.hmrc.agentregistration.shared.upscan.UploadId
import uk.gov.hmrc.agentregistration.shared.upscan.UploadStatus
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.formats.MongoFormats
import uk.gov.hmrc.mongo.play.json.Codecs
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository

import java.net.URI
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

object UploadDetailsRepo:

  val status = "status"

  private given Format[UploadStatus] =
    given Format[URL] = summon[Format[String]].inmap(new URI(_).toURL, _.toString)
    given Format[UploadStatus.UploadedSuccessfully] = Json.format[UploadStatus.UploadedSuccessfully]
    val read: Reads[UploadStatus] =
      case jsObject: JsObject =>
        (jsObject \ "_type").asOpt[JsString] match
          case Some(JsString("InProgress")) => JsSuccess(UploadStatus.InProgress)
          case Some(JsString("Failed")) => JsSuccess(UploadStatus.Failed)
          case Some(JsString("UploadedSuccessfully")) => Json.fromJson[UploadStatus.UploadedSuccessfully](jsObject)
          case Some(JsString(otherVal)) => JsError(s"Unexpected value of _type: $otherVal")
          case None => JsError("Missing _type field")
      case _ => JsError("Expected JsObject for UploadStatus")

    val write: Writes[UploadStatus] =
      case UploadStatus.InProgress => JsObject(Map("_type" -> JsString("InProgress")))
      case UploadStatus.Failed => JsObject(Map("_type" -> JsString("Failed")))
      case s: UploadStatus.UploadedSuccessfully => Json.toJsObject(s)(UploadStatus.uploadedFormat) + ("_type" -> JsString("UploadedSuccessfully"))

    Format(read, write)

  private given Format[UploadId] = Format.at[String](__ \ "value")
    .inmap[UploadId](UploadId.apply, _.value)

  private[repository] val mongoFormat: Format[UploadDetails] =
    given Format[ObjectId] = MongoFormats.objectIdFormat
    ((__ \ "uploadId").format[UploadId]
      ~ (__ \ "reference").format[FileUploadReference]
      ~ (__ \ "status").format[UploadStatus])(UploadDetails.apply, Tuple.fromProductTyped)

@Singleton
final class UploadDetailsRepo @Inject() (
  mongoComponent: MongoComponent
)(using
  ExecutionContext
)
extends PlayMongoRepository[UploadDetails](
  collectionName = "upscan-upload-details",
  mongoComponent = mongoComponent,
  domainFormat = UploadDetailsRepo.mongoFormat,
  indexes = Seq(
    IndexModel(Indexes.ascending("uploadId"), IndexOptions().unique(true)),
    IndexModel(Indexes.ascending("reference"), IndexOptions().unique(true))
  ),
  replaceIndexes = true
):

  import UploadDetailsRepo.given

  def insert(details: UploadDetails): Future[Unit] = collection.insertOne(details)
    .toFuture()
    .map(_ => ())

  def findByUploadId(uploadId: UploadId): Future[Option[UploadDetails]] = collection.find(equal("uploadId", Codecs.toBson(uploadId))).headOption()

  def updateStatus(
    reference: FileUploadReference,
    newStatus: UploadStatus
  ): Future[UploadStatus] = collection
    .findOneAndUpdate(
      filter = equal("reference", Codecs.toBson(reference)),
      update = set("status", Codecs.toBson(newStatus)),
      options = FindOneAndUpdateOptions().upsert(true)
    )
    .toFuture()
    .map(_.status)
