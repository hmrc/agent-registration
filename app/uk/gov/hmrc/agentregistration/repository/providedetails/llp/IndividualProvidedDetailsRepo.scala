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
import uk.gov.hmrc.agentregistration.repository.providedetails.llp.ProvidedDetailsRepoHelp.given
import uk.gov.hmrc.agentregistration.shared.AgentApplicationId
import uk.gov.hmrc.agentregistration.shared.InternalUserId
import uk.gov.hmrc.agentregistration.shared.llp.IndividualProvidedDetails.given
import uk.gov.hmrc.agentregistration.shared.llp.IndividualProvidedDetails
import uk.gov.hmrc.agentregistration.shared.llp.IndividualProvidedDetailsId
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.Codecs

import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

@Singleton
final class IndividualProvidedDetailsRepo @Inject() (
  mongoComponent: MongoComponent,
  appConfig: AppConfig
)(using ec: ExecutionContext)
extends Repo[IndividualProvidedDetailsId, IndividualProvidedDetails](
  collectionName = "individual",
  mongoComponent = mongoComponent,
  indexes = ProvidedDetailsRepoHelp.indexes(appConfig.ProvideDetailsRepo.ttl),
  extraCodecs = Seq(Codecs.playFormatCodec(IndividualProvidedDetails.format)),
  replaceIndexes = true
):

  def findByInternalUserId(internalUserId: InternalUserId): Future[List[IndividualProvidedDetails]] = collection
    .find(
      filter = Filters.eq("internalUserId", internalUserId.value)
    )
    .toFuture()
    .map(_.toList)

  def find(
    internalUserId: InternalUserId,
    agentApplicationId: AgentApplicationId
  ): Future[Option[IndividualProvidedDetails]] = collection
    .find(
      Filters.and(
        Filters.eq("internalUserId", internalUserId.value),
        Filters.eq("agentApplicationId", agentApplicationId.value)
      )
    )
    .headOption()

object ProvidedDetailsRepoHelp:

  given IdString[IndividualProvidedDetailsId] =
    new IdString[IndividualProvidedDetailsId]:
      override def idString(i: IndividualProvidedDetailsId): String = i.value

  given IdExtractor[IndividualProvidedDetails, IndividualProvidedDetailsId] =
    new IdExtractor[IndividualProvidedDetails, IndividualProvidedDetailsId]:
      override def id(memberProvidedDetails: IndividualProvidedDetails): IndividualProvidedDetailsId = memberProvidedDetails.individualProvidedDetailsId

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
