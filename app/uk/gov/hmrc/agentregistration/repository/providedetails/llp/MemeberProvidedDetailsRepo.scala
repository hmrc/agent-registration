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

import org.mongodb.scala.model.IndexModel
import org.mongodb.scala.model.IndexOptions
import org.mongodb.scala.model.Indexes
import uk.gov.hmrc.agentregistration.config.AppConfig
import uk.gov.hmrc.agentregistration.repository.Repo.IdExtractor
import uk.gov.hmrc.agentregistration.repository.Repo.IdString
import uk.gov.hmrc.agentregistration.repository.Repo
import uk.gov.hmrc.agentregistration.repository.providedetails
import uk.gov.hmrc.agentregistration.repository.providedetails.llp.ProvidedDetailsRepoHelp.given
import uk.gov.hmrc.agentregistration.shared.llp.MemberProvidedDetails.given
import uk.gov.hmrc.agentregistration.shared.llp.MemberProvidedDetails
import uk.gov.hmrc.agentregistration.shared.llp.MemberProvidedDetailsId
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.Codecs

import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration

@Singleton
final class MemeberProvidedDetailsRepo @Inject() (
  mongoComponent: MongoComponent,
  appConfig: AppConfig
)(using ec: ExecutionContext)
extends Repo[MemberProvidedDetailsId, MemberProvidedDetails](
  collectionName = "llp-member-provided-details",
  mongoComponent = mongoComponent,
  indexes = ProvidedDetailsRepoHelp.indexes(appConfig.ProvideDetailsRepo.ttl),
  extraCodecs = Seq(Codecs.playFormatCodec(MemberProvidedDetails.format)),
  replaceIndexes = true
)

object ProvidedDetailsRepoHelp:

  given IdString[MemberProvidedDetailsId] =
    new IdString[MemberProvidedDetailsId]:
      override def idString(i: MemberProvidedDetailsId): String = i.value

  given IdExtractor[MemberProvidedDetails, MemberProvidedDetailsId] =
    new IdExtractor[MemberProvidedDetails, MemberProvidedDetailsId]:
      override def id(memberProvidedDetails: MemberProvidedDetails): MemberProvidedDetailsId = memberProvidedDetails.memberProvidedDetailsId

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
