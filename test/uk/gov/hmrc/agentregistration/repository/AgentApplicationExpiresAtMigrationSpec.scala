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

import org.bson.BsonDocument
import org.bson.BsonType
import org.mongodb.scala.ObservableFuture
import org.mongodb.scala.SingleObservableFuture
import org.mongodb.scala.model.Filters
import org.mongodb.scala.model.Updates
import org.scalatest.Assertion
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.AgentApplicationId
import uk.gov.hmrc.agentregistration.shared.ApplicationReference
import uk.gov.hmrc.agentregistration.shared.InternalUserId
import uk.gov.hmrc.agentregistration.shared.LinkId
import uk.gov.hmrc.agentregistration.testsupport.ISpec

import scala.concurrent.Future

class AgentApplicationExpiresAtMigrationSpec
extends ISpec:

  private lazy val repo: AgentApplicationRepo = app.injector.instanceOf[AgentApplicationRepo]
  private lazy val migration: AgentApplicationExpiresAtMigration = app.injector.instanceOf[AgentApplicationExpiresAtMigration]

  private def rawApplicationExpiresAtType(agentApplicationId: AgentApplicationId): BsonType =
    repo
      .collection
      .withDocumentClass[BsonDocument]()
      .find(Filters.eq("_id", agentApplicationId.value))
      .headOption()
      .futureValue
      .value
      .get("applicationExpiresAt")
      .getBsonType

  private def downgradeToLegacyString(record: AgentApplication): Assertion =
    repo
      .collection
      .updateOne(
        filter = Filters.eq("_id", record.agentApplicationId.value),
        update = Updates.set("applicationExpiresAt", record.applicationExpiresAt.value.toString)
      )
      .toFuture()
      .futureValue
    rawApplicationExpiresAtType(record.agentApplicationId) shouldBe BsonType.STRING withClue "sanity: seeded as legacy ISO string"

  "runMigration converts a legacy string-typed applicationExpiresAt to BSON Date and the value round-trips unchanged" in:
    val record: AgentApplication = tdAll.agentApplicationLlp.afterStarted
    repo.upsert(record).futureValue
    downgradeToLegacyString(record)

    migration.runMigration().futureValue shouldBe 1L

    rawApplicationExpiresAtType(record.agentApplicationId) shouldBe BsonType.DATE_TIME
    repo.findById(record.agentApplicationId).futureValue.value shouldBe record

  "runMigration is idempotent — a second run modifies nothing" in:
    val record: AgentApplication = tdAll.agentApplicationLlp.afterStarted
    repo.upsert(record).futureValue
    downgradeToLegacyString(record)

    migration.runMigration().futureValue shouldBe 1L
    migration.runMigration().futureValue shouldBe 0L

  "runMigration leaves a record already stored as BSON Date untouched" in:
    val record: AgentApplication = tdAll.agentApplicationLlp.afterStarted
    repo.upsert(record).futureValue
    rawApplicationExpiresAtType(record.agentApplicationId) shouldBe BsonType.DATE_TIME withClue "sanity: the codec writes BSON Date"

    migration.runMigration().futureValue shouldBe 0L

    repo.findById(record.agentApplicationId).futureValue.value shouldBe record

  "runMigration leaves a record without applicationExpiresAt untouched" in:
    val record: AgentApplication = tdAll.agentApplicationLlp.afterSentForRisking
    record.applicationExpiresAt shouldBe None withClue "sanity: post-submission applications carry no applicationExpiresAt"
    repo.upsert(record).futureValue

    migration.runMigration().futureValue shouldBe 0L

    repo.findById(record.agentApplicationId).futureValue.value shouldBe record

  "runMigration is safe when two instances run it concurrently without the lock — every record converted exactly once, counts sum to the total, values round-trip" in:
    val recordCount: Int = 20
    val records: Seq[AgentApplication] = (1 to recordCount).map: index =>
      tdAll.agentApplicationLlp.afterStarted.copy(
        _id = AgentApplicationId(s"agent-application-id-$index"),
        applicationReference = ApplicationReference(s"APPREF$index"),
        internalUserId = InternalUserId(s"internal-user-id-$index"),
        linkId = LinkId(s"link-id-$index")
      )
    records.foreach(record => repo.upsert(record).futureValue)
    repo
      .collection
      .updateMany(
        filter = BsonDocument.parse("""{ "applicationExpiresAt": { "$type": "date" } }"""),
        update = Seq(BsonDocument.parse("""{ "$set": { "applicationExpiresAt": { "$toString": "$applicationExpiresAt" } } }"""))
      )
      .toFuture()
      .futureValue
    countByType("string").futureValue shouldBe recordCount.toLong withClue "sanity: every seeded record is a legacy string before the run"

    val runA: Future[Long] = migration.runMigration()
    val runB: Future[Long] = migration.runMigration()
    val modifiedByA: Long = runA.futureValue
    val modifiedByB: Long = runB.futureValue

    modifiedByA + modifiedByB shouldBe recordCount.toLong withClue "each record must be converted by exactly one of the two runs"
    countByType("string").futureValue shouldBe 0L withClue "no legacy strings may remain"
    countByType("date").futureValue shouldBe recordCount.toLong withClue "every record must now be a BSON Date"
    repo.collection.find().toFuture().futureValue.toSet shouldBe records.toSet withClue "every migrated value must round-trip to the seeded Instant"

  "countRemaining reports how many records still hold applicationExpiresAt as a legacy string, and drops to zero after the migration" in:
    val record: AgentApplication = tdAll.agentApplicationLlp.afterStarted
    repo.upsert(record).futureValue
    downgradeToLegacyString(record)

    migration.countRemaining().futureValue shouldBe 1L

    migration.runMigration().futureValue shouldBe 1L
    migration.countRemaining().futureValue shouldBe 0L

  private def countByType(bsonTypeAlias: String): Future[Long] = repo
    .collection
    .countDocuments(BsonDocument.parse(s"""{ "applicationExpiresAt": { "$$type": "$bsonTypeAlias" } }"""))
    .toFuture()
