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

import org.mongodb.scala.ObservableFuture
import org.mongodb.scala.SingleObservableFuture
import org.mongodb.scala.model.Aggregates
import org.mongodb.scala.model.Filters
import org.mongodb.scala.model.IndexModel
import org.mongodb.scala.model.IndexOptions
import org.mongodb.scala.model.Indexes
import uk.gov.hmrc.agentregistration.config.AppConfig
import uk.gov.hmrc.agentregistration.crypto.AgentApplicationEncryption
import uk.gov.hmrc.agentregistration.repository.Repo.IdExtractor
import uk.gov.hmrc.agentregistration.repository.Repo.IdString
import uk.gov.hmrc.agentregistration.repository.providedetails.llp.IndividualProvidedDetailsRepo
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.AgentApplicationId
import uk.gov.hmrc.agentregistration.shared.ApplicationReference
import uk.gov.hmrc.agentregistration.shared.ApplicationState
import uk.gov.hmrc.agentregistration.shared.InternalUserId
import uk.gov.hmrc.agentregistration.shared.LinkId
import uk.gov.hmrc.agentregistration.shared.individual.ProvidedDetailsState
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.Codecs

import java.time.Instant
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration
import AgentApplicationRepoHelp.given
import org.mongodb.scala.Document

@Singleton
final class AgentApplicationRepo @Inject() (
  mongoComponent: MongoComponent,
  appConfig: AppConfig,
  agentApplicationEncryption: AgentApplicationEncryption
)(using ec: ExecutionContext)
extends Repo[AgentApplicationId, AgentApplication](
  collectionName = AgentApplicationRepo.collectionName,
  mongoComponent = mongoComponent,
  indexes = AgentApplicationRepoHelp.indexes(appConfig.AgentApplicationRepo.ttl),
  extraCodecs = Seq(Codecs.playFormatCodec(agentApplicationEncryption.formats)),
  replaceIndexes = true
)(using domainFormat = agentApplicationEncryption.formats):

  def findByInternalUserId(internalUserId: InternalUserId): Future[Option[AgentApplication]] = collection
    .find(
      filter = Filters.eq(FieldNames.internalUserId, agentApplicationEncryption.encrypt(internalUserId).value)
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

  def findReadyForReadyToSubmitEmail(): Future[Seq[AgentApplication]] = collection
    .aggregate[AgentApplication](Seq(
      Aggregates.filter(
        Filters.and(
          Filters.eq(FieldNames.applicationState, ApplicationState.GrsDataReceived.toString),
          Filters.ne(FieldNames.isApplicationReadyToSubmitEmailSent, true)
        )
      ),
      Aggregates.lookup(
        from = IndividualProvidedDetailsRepo.collectionName,
        localField = FieldNames._id,
        foreignField = FieldNames.agentApplicationId,
        as = FieldNames.individuals
      ),
      Aggregates.filter(
        Filters.and(
          Filters.exists(FieldNames.individualsFirstElement),
          Repo.forall(FieldNames.individuals, Filters.eq(FieldNames.providedDetailsState, ProvidedDetailsState.Finished.toString))
        )
      )
    ))
    .toFuture()

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

  def indexes(cacheTtl: FiniteDuration): Seq[IndexModel] = Seq(
    IndexModel(
      keys = Indexes.ascending(FieldNames.lastUpdated),
      indexOptions = IndexOptions().expireAfter(cacheTtl.toSeconds, TimeUnit.SECONDS).name("lastUpdatedIdx")
    ),
    IndexModel(
      keys = Indexes.ascending(FieldNames.internalUserId),
      IndexOptions()
        .unique(true)
        .name("internalUserId")
    ),
    IndexModel(
      keys = Indexes.ascending(FieldNames.linkId),
      IndexOptions()
        .unique(true)
        .name("linkId")
    ),
    IndexModel(
      keys = Indexes.ascending(FieldNames.applicationReference),
      IndexOptions()
        .unique(true)
        .name("applicationReference")
    )
  )
