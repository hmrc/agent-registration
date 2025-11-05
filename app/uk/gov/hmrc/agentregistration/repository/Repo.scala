/*
 * Copyright 2023 HM Revenue & Customs
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

import org.bson.codecs.Codec
import org.mongodb.scala.SingleObservableFuture
import org.mongodb.scala.model.Filters
import org.mongodb.scala.model.IndexModel
import org.mongodb.scala.model.ReplaceOptions
import org.mongodb.scala.result.DeleteResult
import play.api.libs.json.*
import uk.gov.hmrc.agentregistration.repository.Repo.IdExtractor
import uk.gov.hmrc.agentregistration.repository.Repo.IdString
import uk.gov.hmrc.agentregistration.shared.AgentApplicationId
import uk.gov.hmrc.agentregistration.shared.InternalUserId
import uk.gov.hmrc.agentregistration.shared.LinkId
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.reflect.ClassTag

@SuppressWarnings(Array("org.wartremover.warts.Any"))
abstract class Repo[
  ID,
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
  idString: IdString[ID],
  idExtractor: IdExtractor[A, ID]
)
extends PlayMongoRepository[A](
  mongoComponent = mongoComponent,
  collectionName = collectionName,
  domainFormat = domainFormat,
  indexes = indexes,
  replaceIndexes = replaceIndexes,
  extraCodecs = extraCodecs
):

  /** Update or Insert (UpSert)
    */
  def upsert(a: A): Future[Unit] = collection
    .replaceOne(
      filter = Filters.eq("_id", idString.idString(idExtractor.id(a))),
      replacement = a,
      options = ReplaceOptions().upsert(true)
    )
    .toFuture()
    .map(_ => ())

  def findById(i: ID): Future[Option[A]] = collection
    .find(
      filter = Filters.eq("_id", idString.idString(i))
    )
    .headOption()

  def removeById(i: ID): Future[Option[DeleteResult]] = collection
    .deleteOne(
      filter = Filters.eq("_id", idString.idString(i))
    ).headOption()

  def findByInternalUserId(internalUserId: InternalUserId): Future[Option[A]] = collection
    .find(
      filter = Filters.eq("internalUserId", internalUserId.value)
    )
    .headOption()

  def findByAgentApplicationId(agentApplicationId: AgentApplicationId): Future[Option[A]] = collection
    .find(
      filter = Filters.eq("agentApplicationId", agentApplicationId.value)
    )
    .headOption()

  def findByLinkId(linkId: LinkId): Future[Option[A]] = collection
    .find(
      filter = Filters.eq("linkId", linkId.value)
    )
    .headOption()

object Repo:

  trait IdString[I]:
    def idString(i: I): String

  trait IdExtractor[A, ID]:
    def id(a: A): ID
