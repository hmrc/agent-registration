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

package uk.gov.hmrc.agentregistration.migration

import org.bson.Document
import org.mongodb.scala.MongoCollection
import org.mongodb.scala.SingleObservableFuture
import org.mongodb.scala.model.Filters
import org.mongodb.scala.model.Updates
import uk.gov.hmrc.agentregistration.repository.FieldNames
import uk.gov.hmrc.agentregistration.repository.AgentApplicationRepo
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.testsupport.ISpec
import uk.gov.hmrc.mongo.lock.MongoLockRepository

import scala.concurrent.duration.*

class InternalUserIdHashMigratorInitializerSpec
extends ISpec:

  override protected def configOverrides: Map[String, Any] = Map[String, Any](
    "field-level-encryption.enabled" -> true
  )

  "run the migrator when no other instance holds the lock" in:
    storeWithoutHash(application)

    initializer.start().futureValue shouldBe Some(1L)

    hashOf(application.agentApplicationId.value) shouldBe defined

  "do nothing while another instance holds the lock" in:
    storeWithoutHash(application)
    mongoLockRepository.takeLock(
      lockId = InternalUserIdHashMigratorInitializer.lockId,
      owner = "another-instance",
      ttl = 1.hour
    ).futureValue shouldBe defined

    initializer.start().futureValue shouldBe None

    hashOf(application.agentApplicationId.value) shouldBe None

  private lazy val initializer: InternalUserIdHashMigratorInitializer = app.injector.instanceOf[InternalUserIdHashMigratorInitializer]
  private lazy val agentApplicationRepo: AgentApplicationRepo = app.injector.instanceOf[AgentApplicationRepo]
  private lazy val mongoLockRepository: MongoLockRepository = app.injector.instanceOf[MongoLockRepository]

  private lazy val applications: MongoCollection[Document] = agentApplicationRepo.collection.withDocumentClass[Document]()
  private lazy val application: AgentApplication = tdAll.agentApplicationLlp.afterAgentDetailsComplete

  private def storeWithoutHash(agentApplication: AgentApplication): Unit =
    agentApplicationRepo.upsert(agentApplication).futureValue
    applications
      .updateOne(Filters.eq("_id", agentApplication.agentApplicationId.value), Updates.unset(FieldNames.internalUserIdHash))
      .toFuture()
      .futureValue
    ()

  private def hashOf(id: String): Option[String] = applications
    .find(Filters.eq("_id", id))
    .headOption()
    .futureValue
    .flatMap(document => Option(document.getString(FieldNames.internalUserIdHash)))
