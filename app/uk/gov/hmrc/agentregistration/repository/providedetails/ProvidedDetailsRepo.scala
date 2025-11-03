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

import org.mongodb.scala.model.{IndexModel, IndexOptions, Indexes}
import uk.gov.hmrc.agentregistration.config.AppConfig
import uk.gov.hmrc.agentregistration.repository.providedetails.CompoundRepo.IdExtractor
import uk.gov.hmrc.agentregistration.repository.providedetails.ProvidedDetailsRepoHelp.given
import uk.gov.hmrc.agentregistration.repository.providedetails
import uk.gov.hmrc.agentregistration.shared.ProvidedDetails
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.Codecs

import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration

import ProvidedDetailsRepoHelp.given

@Singleton
final class ProvidedDetailsRepo @Inject() (
  mongoComponent: MongoComponent,
  appConfig: AppConfig
)(using ec: ExecutionContext)
extends CompoundRepo[ProvideDetailsId, ProvidedDetails](
  collectionName = "provided-details",
  mongoComponent = mongoComponent,
  indexes = ProvidedDetailsRepoHelp.indexes(appConfig.ProvideDetailsRepo.ttl),
  extraCodecs = Seq(Codecs.playFormatCodec(ProvidedDetails.format)),
  replaceIndexes = true
)

object ProvidedDetailsRepoHelp:

  given IdExtractor[ProvidedDetails, ProvideDetailsId] with
    def id(pd: ProvidedDetails): ProvideDetailsId =
      ProvideDetailsId(pd.internalUserId, pd.linkId)

  def indexes(ttl: FiniteDuration): Seq[IndexModel] = Seq(
    IndexModel(
      Indexes.ascending("lastUpdated"),
      IndexOptions().expireAfter(ttl.toSeconds, TimeUnit.SECONDS).name("lastUpdatedIdx")
    ),
    IndexModel(Indexes.ascending("_id.linkId"), IndexOptions().name("linkIdIdx")),
    IndexModel(Indexes.ascending("_id.internalUserId"), IndexOptions().name("internalUserIdIdx"))
  )

