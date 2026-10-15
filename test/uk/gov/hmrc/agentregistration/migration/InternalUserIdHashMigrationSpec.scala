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

import org.mongodb.scala.SingleObservableFuture
import org.mongodb.scala.model.Filters
import uk.gov.hmrc.agentregistration.crypto.FieldHasher
import uk.gov.hmrc.agentregistration.crypto.FieldLevelEncryption
import uk.gov.hmrc.agentregistration.repository.AgentApplicationRepo
import uk.gov.hmrc.agentregistration.shared.InternalUserId
import uk.gov.hmrc.agentregistration.testsupport.ISpec
import uk.gov.hmrc.mongo.MongoComponent

class InternalUserIdHashMigrationSpec
extends ISpec:

  override protected def configOverrides: Map[String, Any] = Map[String, Any](
    "field-level-encryption.enabled" -> true,
    "migration.internalUserId.runOnStartup" -> false
  )

  private lazy val migration = app.injector.instanceOf[InternalUserIdHashMigration]
  private lazy val repo = app.injector.instanceOf[AgentApplicationRepo]
  private lazy val fieldHasher = app.injector.instanceOf[FieldHasher]
  private lazy val fieldLevelEncryption = app.injector.instanceOf[FieldLevelEncryption]
  private lazy val appMongoComponent = app.injector.instanceOf[MongoComponent]
  private lazy val rawCollection = appMongoComponent.database.getCollection[org.bson.Document](AgentApplicationRepo.collectionName)

  private def rawInternalUserId(id: String): String = rawCollection
    .find(Filters.eq("_id", id))
    .first()
    .toFuture()
    .futureValue
    .getString("internalUserId")

  "InternalUserIdHashMigration should migrate an encrypted internalUserId to a hash" in:
    val internalUserId = InternalUserId("pre-migration-123")
    val record = tdAll.agentApplicationLlp.afterAgentDetailsComplete.copy(internalUserId = internalUserId)
    repo.upsert(record).futureValue
    val encrypted = fieldLevelEncryption.encrypt(internalUserId.value)
    rawCollection
      .updateOne(
        Filters.eq("_id", record._id.value),
        org.mongodb.scala.model.Updates.set("internalUserId", encrypted)
      )
      .toFuture()
      .futureValue

    rawInternalUserId(record._id.value) shouldBe encrypted
    rawInternalUserId(record._id.value) should not startWith "hash:"

    migration.run.futureValue

    val storedAfter = rawInternalUserId(record._id.value)
    storedAfter should startWith("hash:")
    storedAfter shouldBe fieldHasher.hash(record.internalUserId.value)

  "InternalUserIdHashMigration should not re-migrate an already hashed internalUserId" in:
    val record = tdAll.agentApplicationLlp.afterAgentDetailsComplete
    repo.upsert(record).futureValue

    val storedBefore = rawInternalUserId(record._id.value)
    storedBefore should startWith("hash:")

    migration.run.futureValue

    rawInternalUserId(record._id.value) shouldBe storedBefore

  "InternalUserIdHashMigration should allow findByInternalUserId to succeed on an encrypted document during migration" in:
    val record = tdAll.agentApplicationLlp.afterAgentDetailsComplete
    repo.upsert(record).futureValue

    rawCollection
      .updateOne(
        Filters.eq("_id", record._id.value),
        org.mongodb.scala.model.Updates.set("internalUserId", fieldLevelEncryption.encrypt(record.internalUserId.value))
      )
      .toFuture()
      .futureValue

    val found = repo.findByInternalUserId(record.internalUserId).futureValue
    found.value._id shouldBe record._id

  "InternalUserIdHashMigration should allow findByInternalUserId to succeed on a hashed document after migration" in:
    val record = tdAll.agentApplicationLlp.afterAgentDetailsComplete
    repo.upsert(record).futureValue

    migration.run.futureValue

    val found = repo.findByInternalUserId(record.internalUserId).futureValue
    found.value._id shouldBe record._id

  "InternalUserIdHashMigration should be idempotent" in:
    val record = tdAll.agentApplicationLlp.afterAgentDetailsComplete
    repo.upsert(record).futureValue
    rawCollection
      .updateOne(
        Filters.eq("_id", record._id.value),
        org.mongodb.scala.model.Updates.set("internalUserId", fieldLevelEncryption.encrypt(record.internalUserId.value))
      )
      .toFuture()
      .futureValue

    migration.run.futureValue
    val afterFirst = rawInternalUserId(record._id.value)

    migration.run.futureValue
    val afterSecond = rawInternalUserId(record._id.value)

    afterFirst shouldBe afterSecond
