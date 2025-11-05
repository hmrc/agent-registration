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

import org.mongodb.scala.model.IndexModel
import org.mongodb.scala.model.IndexOptions
import org.mongodb.scala.model.Indexes
import uk.gov.hmrc.agentregistration.config.AppConfig
import uk.gov.hmrc.agentregistration.repository.Repo.IdExtractor
import uk.gov.hmrc.agentregistration.repository.Repo.IdString
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.AgentApplicationId
import uk.gov.hmrc.agentregistration.shared.InternalUserId
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.Codecs

import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration

import AgentApplicationRepoHelp.given

@Singleton
final class AgentApplicationRepo @Inject() (
  mongoComponent: MongoComponent,
  appConfig: AppConfig
)(using ec: ExecutionContext)
extends Repo[AgentApplicationId, AgentApplication](
  collectionName = "agent-application",
  mongoComponent = mongoComponent,
  indexes = AgentApplicationRepoHelp.indexes(appConfig.AgentApplicationRepo.ttl),
  extraCodecs = Seq(Codecs.playFormatCodec(AgentApplication.format)),
  replaceIndexes = true
)

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
      keys = Indexes.ascending("lastUpdated"),
      indexOptions = IndexOptions().expireAfter(cacheTtl.toSeconds, TimeUnit.SECONDS).name("lastUpdatedIdx")
    ),
    IndexModel(
      keys = Indexes.ascending("internalUserId"),
      IndexOptions()
        .unique(true)
        .name("internalUserId")
    ),
    IndexModel(
      keys = Indexes.ascending("linkId"),
      IndexOptions()
        .unique(true)
        .name("linkId")
    )
  )
