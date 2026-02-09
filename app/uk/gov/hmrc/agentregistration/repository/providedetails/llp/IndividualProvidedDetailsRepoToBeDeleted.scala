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

package uk.gov.hmrc.agentregistration.repository.providedetails.llp

import org.mongodb.scala.model.Filters
import org.mongodb.scala.model.IndexModel
import org.mongodb.scala.model.IndexOptions
import org.mongodb.scala.model.Indexes
import uk.gov.hmrc.agentregistration.config.AppConfig
import uk.gov.hmrc.agentregistration.repository.Repo.IdExtractor
import uk.gov.hmrc.agentregistration.repository.Repo.IdString
import uk.gov.hmrc.agentregistration.repository.Repo
import uk.gov.hmrc.agentregistration.repository.providedetails
import uk.gov.hmrc.agentregistration.repository.providedetails.llp.ProvidedDetailsRepoHelpToBeDeleted.given
import uk.gov.hmrc.agentregistration.shared.AgentApplicationId
import uk.gov.hmrc.agentregistration.shared.InternalUserId
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetailsToBeDeleted.given
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetailsToBeDeleted
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetailsId
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.Codecs

import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

/*
  @deprecated("Use `IndividualProvidedDetailsRepo` with optional internalUserId instead.", "2026-02-02")
 */
@Singleton
final class IndividualProvidedDetailsRepoToBeDeleted @Inject() (
  mongoComponent: MongoComponent,
  appConfig: AppConfig
)(using ec: ExecutionContext)
extends Repo[IndividualProvidedDetailsId, IndividualProvidedDetailsToBeDeleted](
  collectionName = "individual-to-be-deleted",
  mongoComponent = mongoComponent,
  indexes = ProvidedDetailsRepoHelpToBeDeleted.indexes(appConfig.ProvideDetailsRepo.ttl),
  extraCodecs = Seq(Codecs.playFormatCodec(IndividualProvidedDetailsToBeDeleted.format)),
  replaceIndexes = true
):

  def findByInternalUserId(internalUserId: InternalUserId): Future[List[IndividualProvidedDetailsToBeDeleted]] = collection
    .find(
      filter = Filters.eq("internalUserId", internalUserId.value)
    )
    .toFuture()
    .map(_.toList)

  def findForApplication(agentApplicationId: AgentApplicationId): Future[List[IndividualProvidedDetailsToBeDeleted]] = collection
    .find(
      filter = Filters.eq("agentApplicationId", agentApplicationId.value)
    )
    .toFuture()
    .map(_.toList)

  def find(
    internalUserId: InternalUserId,
    agentApplicationId: AgentApplicationId
  ): Future[Option[IndividualProvidedDetailsToBeDeleted]] = collection
    .find(
      Filters.and(
        Filters.eq("internalUserId", internalUserId.value),
        Filters.eq("agentApplicationId", agentApplicationId.value)
      )
    )
    .headOption()

/*
  @deprecated("Use `ProvidedDetailsRepoHelpToBe` with optional internalUserId instead.", "2026-02-02")
 */
object ProvidedDetailsRepoHelpToBeDeleted:

  given IdString[IndividualProvidedDetailsId] =
    new IdString[IndividualProvidedDetailsId]:
      override def idString(i: IndividualProvidedDetailsId): String = i.value

  given IdExtractor[IndividualProvidedDetailsToBeDeleted, IndividualProvidedDetailsId] =
    new IdExtractor[IndividualProvidedDetailsToBeDeleted, IndividualProvidedDetailsId]:
      override def id(memberProvidedDetails: IndividualProvidedDetailsToBeDeleted): IndividualProvidedDetailsId =
        memberProvidedDetails.individualProvidedDetailsId

  def indexes(ttl: FiniteDuration): Seq[IndexModel] = Seq(
    IndexModel(
      Indexes.ascending("lastUpdated"),
      IndexOptions().expireAfter(ttl.toSeconds, TimeUnit.SECONDS).name("lastUpdatedIdx")
    ),
    IndexModel(
      Indexes.ascending("internalUserId", "agentApplicationId"),
      IndexOptions()
        .unique(true)
        .name("internalUserId_applicationId_unique")
    )
  )
