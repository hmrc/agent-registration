/*
 * Copyright 2026 HM Revenue & Customs
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
import uk.gov.hmrc.agentregistration.model.ApplicationScheduler
import uk.gov.hmrc.agentregistration.repository.ApplicationSchedulerRepoHelp.given
import uk.gov.hmrc.agentregistration.repository.Repo.IdExtractor
import uk.gov.hmrc.agentregistration.repository.Repo.IdString
import uk.gov.hmrc.agentregistration.shared.ApplicationReference
import uk.gov.hmrc.mongo.MongoComponent

import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration

/** Holds one record per `ApplicationReference` tracking what each scheduler job has decided about its per-application emails. Writes go through
  * [[Repo.upsert]] (whole-document upsert) — the service constructs the new state via [[ApplicationScheduler.makeNew]] + `.copy(...)` and upserts it.
  * Stored separately from `AgentApplication` so scheduler writes don't race concurrent FE writes of the application document (APB-11490).
  */
@Singleton
final class ApplicationSchedulerRepo @Inject() (
  mongoComponent: MongoComponent,
  appConfig: AppConfig
)(using ec: ExecutionContext)
extends Repo[ApplicationReference, ApplicationScheduler](
  collectionName = ApplicationSchedulerRepo.collectionName,
  mongoComponent = mongoComponent,
  indexes = ApplicationSchedulerRepoHelp.indexes(appConfig.ApplicationSchedulerRepo.ttl),
  extraCodecs = Seq.empty,
  replaceIndexes = true
)

object ApplicationSchedulerRepo:
  val collectionName = "application-scheduler"

object ApplicationSchedulerRepoHelp:

  given IdString[ApplicationReference] =
    new IdString[ApplicationReference]:
      override def idString(i: ApplicationReference): String = i.value

  given IdExtractor[ApplicationScheduler, ApplicationReference] =
    new IdExtractor[ApplicationScheduler, ApplicationReference]:
      override def id(applicationScheduler: ApplicationScheduler): ApplicationReference = applicationScheduler._id

  def indexes(ttl: FiniteDuration): Seq[IndexModel] = Seq(
    IndexModel(
      keys = Indexes.ascending(FieldNames.lastUpdated),
      indexOptions = IndexOptions().expireAfter(ttl.toSeconds, TimeUnit.SECONDS).name("lastUpdatedIdx")
    )
  )
