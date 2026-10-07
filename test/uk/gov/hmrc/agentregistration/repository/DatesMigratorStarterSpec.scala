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

import org.bson.BsonDocument
import org.bson.BsonType
import org.mongodb.scala.SingleObservableFuture
import org.mongodb.scala.model.Filters
import org.mongodb.scala.model.Updates
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.testsupport.ISpec
import uk.gov.hmrc.mongo.lock.MongoLockRepository

import scala.concurrent.duration.*

class DatesMigratorStarterSpec
extends ISpec:

  "start runs the dates migrator when no other instance holds the lock" in:
    storeWithCreatedAtAsIsoString(record)

    starter.start().futureValue shouldBe Some(1L)

    createdAtType shouldBe BsonType.DATE_TIME

  "start does nothing while another instance holds the lock" in:
    storeWithCreatedAtAsIsoString(record)
    mongoLockRepository.takeLock(
      lockId = DatesMigratorStarter.lockId,
      owner = "another-instance",
      ttl = 1.hour
    ).futureValue shouldBe defined

    starter.start().futureValue shouldBe None

    createdAtType shouldBe BsonType.STRING

  private lazy val repo: AgentApplicationRepo = app.injector.instanceOf[AgentApplicationRepo]
  private lazy val starter: DatesMigratorStarter = app.injector.instanceOf[DatesMigratorStarter]
  private lazy val mongoLockRepository: MongoLockRepository = app.injector.instanceOf[MongoLockRepository]

  private lazy val record: AgentApplication = tdAll.agentApplicationLlp.afterStarted

  private def storeWithCreatedAtAsIsoString(record: AgentApplication): Unit =
    repo.upsert(record).futureValue
    repo
      .collection
      .updateOne(Filters.eq("_id", record.agentApplicationId.value), Updates.set("createdAt", record.createdAt.toString))
      .toFuture()
      .futureValue
    ()

  private def createdAtType: BsonType =
    repo
      .collection
      .withDocumentClass[BsonDocument]()
      .find(Filters.eq("_id", record.agentApplicationId.value))
      .headOption()
      .futureValue
      .value
      .get("createdAt")
      .getBsonType
