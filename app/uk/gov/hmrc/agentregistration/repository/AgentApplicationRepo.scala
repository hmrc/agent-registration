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

import org.mongodb.scala.bson.BsonDateTime
import org.mongodb.scala.bson.BsonDocument
import org.mongodb.scala.model.Filters
import org.mongodb.scala.model.IndexModel
import org.mongodb.scala.model.IndexOptions
import org.mongodb.scala.model.Indexes
import org.mongodb.scala.model.Projections
import org.mongodb.scala.model.Updates
import uk.gov.hmrc.agentregistration.crypto.AgentApplicationMongoFormats
import uk.gov.hmrc.agentregistration.crypto.Hasher
import uk.gov.hmrc.agentregistration.repository.Repo.IdExtractor
import uk.gov.hmrc.agentregistration.repository.Repo.IdString
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.AgentApplicationId
import uk.gov.hmrc.agentregistration.shared.ApplicationReference
import uk.gov.hmrc.agentregistration.shared.ApplicationState
import uk.gov.hmrc.agentregistration.shared.InternalUserId
import uk.gov.hmrc.agentregistration.shared.LinkId
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.Codecs

import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import AgentApplicationRepoHelp.given

@Singleton
final class AgentApplicationRepo @Inject() (
  mongoComponent: MongoComponent,
  agentApplicationMongoFormats: AgentApplicationMongoFormats,
  hasher: Hasher
)(using ec: ExecutionContext)
extends Repo[AgentApplicationId, AgentApplication](
  collectionName = AgentApplicationRepo.collectionName,
  mongoComponent = mongoComponent,
  indexes = AgentApplicationRepoHelp.indexes,
  extraCodecs = Seq(Codecs.playFormatCodec(agentApplicationMongoFormats.encryptingFormat)),
  replaceIndexes = true
)(using domainFormat = agentApplicationMongoFormats.encryptingFormat):

  override lazy val requiresTtlIndex: Boolean = false

  def findByInternalUserId(internalUserId: InternalUserId): Future[Option[AgentApplication]] = collection
    .find(
      filter = Filters.eq(FieldNames.internalUserIdHash, hasher.hash(internalUserId.value))
    )
    .headOption()

  def findByLinkId(linkId: LinkId): Future[Option[AgentApplication]] = collection
    .find(
      filter = Filters.eq(FieldNames.linkId, linkId.value)
    )
    .headOption()

  def findByApplicationReference(applicationReference: ApplicationReference): Future[Option[AgentApplication]] = collection
    .find(
      filter = Filters.eq(FieldNames.applicationReference, applicationReference.value)
    )
    .headOption()

  def updateManyApplicationStateByReference(
    applicationReferences: Seq[ApplicationReference],
    applicationState: ApplicationState
  ): Future[Unit] = collection
    .updateMany(
      filter = Filters.in(FieldNames.applicationReference, applicationReferences.map(_.value)*),
      update = Updates.set(FieldNames.applicationState, applicationState.toString)
    )
    .toFuture()
    .map(_ => ())

  def updateManyToExpiredForUnsubmitted(
    now: Instant,
    postExpiryGracePeriodEndsAt: Instant
  ): Future[Long] = collection
    .updateMany(
      filter = Filters.and(
        Filters.in(
          FieldNames.applicationState,
          ApplicationState.Started.toString,
          ApplicationState.GrsDataReceived.toString
        ),
        Filters.lt(FieldNames.applicationExpiresAt, BsonDateTime(now.toEpochMilli))
      ),
      update = Updates.combine(
        Updates.set(FieldNames.applicationState, ApplicationState.Expired.toString),
        Updates.set(FieldNames.postExpiryGracePeriodEndsAt, BsonDateTime(postExpiryGracePeriodEndsAt.toEpochMilli)),
        Updates.unset(FieldNames.applicationExpiresAt)
      )
    )
    .toFuture()
    .map(_.getModifiedCount)

  def findIdsOfExpiredWithPostExpiryGracePeriodEnded(now: Instant): Future[Seq[AgentApplicationId]] = collection
    .withDocumentClass[BsonDocument]()
    .find(
      filter = Filters.and(
        Filters.eq(FieldNames.applicationState, ApplicationState.Expired.toString),
        Filters.lt(FieldNames.postExpiryGracePeriodEndsAt, BsonDateTime(now.toEpochMilli))
      )
    )
    .projection(Projections.include(FieldNames._id))
    .toFuture()
    .map(_.map(bsonDocument => AgentApplicationId(bsonDocument.getString(FieldNames._id).getValue)))

object AgentApplicationRepo:
  val collectionName = "agent-application"

// when named it AgentApplicationRepo, Scala 3 compiler complains
// about cyclic reference error during compilation ...
object AgentApplicationRepoHelp:

  given IdString[AgentApplicationId] =
    new IdString[AgentApplicationId]:
      override def idString(i: AgentApplicationId): String = i.value

  given IdExtractor[AgentApplication, AgentApplicationId] =
    new IdExtractor[AgentApplication, AgentApplicationId]:
      override def id(agentApplication: AgentApplication): AgentApplicationId = agentApplication.agentApplicationId

  val indexes: Seq[IndexModel] = Seq(
    IndexModel(
      keys = Indexes.ascending(FieldNames.internalUserIdHash),
      IndexOptions()
        .unique(true)
        .name(FieldNames.internalUserIdHash)
    ),
    IndexModel(
      keys = Indexes.ascending(FieldNames.linkId),
      IndexOptions()
        .unique(true)
        .name(FieldNames.linkId)
    ),
    IndexModel(
      keys = Indexes.ascending(FieldNames.applicationReference),
      IndexOptions()
        .unique(true)
        .name(FieldNames.applicationReference)
    )
  )
