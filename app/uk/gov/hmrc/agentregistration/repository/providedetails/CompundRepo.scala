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

package uk.gov.hmrc.agentregistration.repository.providedetails

import org.bson.codecs.Codec
import org.mongodb.scala.model.{Filters, IndexModel, ReplaceOptions}
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.result.DeleteResult
import org.mongodb.scala.bson.BsonDocument
import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.agentregistration.shared.{InternalUserId, LinkId}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag


@SuppressWarnings(Array("org.wartremover.warts.Any"))
abstract class CompoundRepo[
  ID: OFormat,
  A: ClassTag
](
   collectionName: String,
   mongoComponent: MongoComponent,
   indexes: Seq[IndexModel],
   extraCodecs: Seq[Codec[?]],
   replaceIndexes: Boolean = false
 )(using
   domainFormat: OFormat[A],
   executionContext: ExecutionContext,
   idExtractor: CompoundRepo.IdExtractor[A, ID]
 ) extends PlayMongoRepository[A](
  mongoComponent = mongoComponent,
  collectionName = collectionName,
  domainFormat = domainFormat,
  indexes = indexes,
  replaceIndexes = replaceIndexes,
  extraCodecs = extraCodecs
):
  
  protected def idAsBson(id: ID): Bson =
    val jsonStr = Json.stringify(Json.toJson(id))
    BsonDocument(jsonStr)
  
  def upsert(a: A): Future[Unit] =
    val id = idExtractor.id(a)
    collection
      .replaceOne(
        filter = Filters.eq("_id", idAsBson(id)),
        replacement = a,
        options = ReplaceOptions().upsert(true)
      )
      .toFuture()
      .map(_ => ())
  
  def findById(id: ID): Future[Option[A]] =
    collection.find(Filters.eq("_id", idAsBson(id))).headOption()
  
  def removeById(id: ID): Future[Option[DeleteResult]] =
    collection.deleteOne(Filters.eq("_id", idAsBson(id))).headOption()
  
  def findByInternalUserId(internalUserId: InternalUserId): Future[List[A]] =
    collection
      .find(Filters.eq("_id.internalUserId", internalUserId.value))
      .toFuture()
      .map(_.toList)
  
  def findByLinkId(linkId: LinkId): Future[List[A]] =
    collection
      .find(Filters.eq("_id.linkId", linkId.value))
      .toFuture()
      .map(_.toList)

object CompoundRepo:

  /** Extracts compound ID from domain object */
  trait IdExtractor[A, ID]:
    def id(a: A): ID
