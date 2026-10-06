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
import org.bson.conversions.Bson
import org.mongodb.scala.MongoCollection
import org.mongodb.scala.SingleObservableFuture
import org.mongodb.scala.model.Filters
import org.mongodb.scala.model.Updates
import uk.gov.hmrc.agentregistration.repository.providedetails.llp.IndividualProvidedDetailsRepo
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetails
import uk.gov.hmrc.agentregistration.shared.risking.RiskingOutcomeApplication
import uk.gov.hmrc.agentregistration.testsupport.ISpec

import java.time.Instant
import java.time.LocalDate
import scala.concurrent.Future

class AgentApplicationDatesMigrationSpec
extends ISpec:

  private lazy val repo: AgentApplicationRepo = app.injector.instanceOf[AgentApplicationRepo]
  private lazy val individualRepo: IndividualProvidedDetailsRepo = app.injector.instanceOf[IndividualProvidedDetailsRepo]
  private lazy val migration: AgentApplicationDatesMigration = app.injector.instanceOf[AgentApplicationDatesMigration]

  private def rawDocument(
    collection: MongoCollection[?],
    id: String
  ): BsonDocument =
    collection
      .withDocumentClass[BsonDocument]()
      .find(Filters.eq("_id", id))
      .headOption()
      .futureValue
      .value

  private def rawApplication(record: AgentApplication): BsonDocument = rawDocument(repo.collection, record.agentApplicationId.value)

  private def setRawStrings(
    collection: MongoCollection[?],
    id: String,
    valuesByFieldName: Seq[(String, String)]
  ): Unit =
    val updates: Seq[Bson] = valuesByFieldName.map(
      (
        fieldName,
        value
      ) => Updates.set(fieldName, value)
    )
    collection
      .updateOne(Filters.eq("_id", id), Updates.combine(updates*))
      .toFuture()
      .futureValue
    ()

  private def downgradeToLegacyStrings(record: AgentApplication): Unit =
    val reSubmittedAt: Option[Instant] =
      record.riskingOutcomeApplication.collect:
        case failedFixable: RiskingOutcomeApplication.FailedFixable => failedFixable.reSubmittedAt
      .flatten
    val actualDecisionDate: Option[LocalDate] = record.riskingOutcomeApplication.map(_.actualDecisionDate)
    val correctiveActionExpiryDate: Option[LocalDate] = record.riskingOutcomeApplication.collect:
      case failedFixable: RiskingOutcomeApplication.FailedFixable => failedFixable.correctiveActionExpiryDate
      case failedNonFixable: RiskingOutcomeApplication.FailedNonFixable => failedNonFixable.correctiveActionExpiryDate
    val storedDates: Seq[(String, Option[String])] = Seq(
      "createdAt" -> Some(record.createdAt.toString),
      "applicationExpiresAt" -> record.applicationExpiresAt.map(_.toString),
      "submittedAt" -> record.submittedAt.map(_.toString),
      "riskingOutcomeApplication.actualDecisionDate" -> actualDecisionDate.map(_.toString),
      "riskingOutcomeApplication.reSubmittedAt" -> reSubmittedAt.map(_.toString),
      "riskingOutcomeApplication.correctiveActionExpiryDate" -> correctiveActionExpiryDate.map(_.toString)
    )
    setRawStrings(
      repo.collection,
      record.agentApplicationId.value,
      storedDates.collect { case (fieldName, Some(value)) => fieldName -> value }
    )

  "run converts createdAt and applicationExpiresAt of a pre-submission application to BSON Date and the values round-trip unchanged" in:
    val record: AgentApplication = tdAll.agentApplicationLlp.afterStarted
    repo.upsert(record).futureValue
    downgradeToLegacyStrings(record)
    rawApplication(record).get("createdAt").getBsonType shouldBe BsonType.STRING
    rawApplication(record).get("applicationExpiresAt").getBsonType shouldBe BsonType.STRING

    migration.run().futureValue shouldBe 2L

    rawApplication(record).get("createdAt").getBsonType shouldBe BsonType.DATE_TIME
    rawApplication(record).get("applicationExpiresAt").getBsonType shouldBe BsonType.DATE_TIME
    rawApplication(record).containsKey("submittedAt") shouldBe false
    rawApplication(record).containsKey("riskingOutcomeApplication") shouldBe false
    repo.findById(record.agentApplicationId).futureValue.value shouldBe record

  "run converts createdAt and submittedAt of a submitted application to BSON Date and does not add applicationExpiresAt" in:
    val record: AgentApplication = tdAll.agentApplicationLlp.afterSentForRisking
    record.submittedAt shouldBe defined
    repo.upsert(record).futureValue
    downgradeToLegacyStrings(record)

    migration.run().futureValue shouldBe 2L

    rawApplication(record).get("createdAt").getBsonType shouldBe BsonType.DATE_TIME
    rawApplication(record).get("submittedAt").getBsonType shouldBe BsonType.DATE_TIME
    rawApplication(record).containsKey("applicationExpiresAt") shouldBe false
    repo.findById(record.agentApplicationId).futureValue.value shouldBe record

  "run converts the nested riskingOutcomeApplication.reSubmittedAt of a resubmitted application to BSON Date" in:
    val record: AgentApplication = tdAll.agentApplicationLlp.afterResubmitted
    repo.upsert(record).futureValue
    downgradeToLegacyStrings(record)
    rawApplication(record).getDocument("riskingOutcomeApplication").get("reSubmittedAt").getBsonType shouldBe BsonType.STRING

    migration.run().futureValue

    rawApplication(record).getDocument("riskingOutcomeApplication").get("reSubmittedAt").getBsonType shouldBe BsonType.DATE_TIME
    migration.run().futureValue shouldBe 0L
    repo.findById(record.agentApplicationId).futureValue.value shouldBe record

  "run converts riskingOutcomeApplication.actualDecisionDate and correctiveActionExpiryDate to BSON Date and does not add reSubmittedAt" in:
    val record: AgentApplication = tdAll.agentApplicationLlp.afterRiskingCompletedFixable
    repo.upsert(record).futureValue
    downgradeToLegacyStrings(record)
    rawApplication(record).getDocument("riskingOutcomeApplication").get("actualDecisionDate").getBsonType shouldBe BsonType.STRING
    rawApplication(record).getDocument("riskingOutcomeApplication").get("correctiveActionExpiryDate").getBsonType shouldBe BsonType.STRING

    migration.run().futureValue

    val riskingOutcomeApplication: BsonDocument = rawApplication(record).getDocument("riskingOutcomeApplication")
    riskingOutcomeApplication.get("actualDecisionDate").getBsonType shouldBe BsonType.DATE_TIME
    riskingOutcomeApplication.get("correctiveActionExpiryDate").getBsonType shouldBe BsonType.DATE_TIME
    riskingOutcomeApplication.containsKey("reSubmittedAt") shouldBe false
    migration.run().futureValue shouldBe 0L
    repo.findById(record.agentApplicationId).futureValue.value shouldBe record

  "run converts createdAt in the individual collection to BSON Date and the value round-trips unchanged" in:
    val record: IndividualProvidedDetails = tdAll.providedDetails.afterFinished
    val id: String = record.individualProvidedDetailsId.value
    individualRepo.upsert(record).futureValue
    rawDocument(individualRepo.collection, id).get("createdAt").getBsonType shouldBe BsonType.DATE_TIME
    setRawStrings(
      individualRepo.collection,
      id,
      Seq("createdAt" -> record.createdAt.toString)
    )

    migration.run().futureValue shouldBe 1L

    rawDocument(individualRepo.collection, id).get("createdAt").getBsonType shouldBe BsonType.DATE_TIME
    individualRepo.findById(record.individualProvidedDetailsId).futureValue.value shouldBe record

  "run is idempotent — a second run converts nothing" in:
    val record: AgentApplication = tdAll.agentApplicationLlp.afterStarted
    repo.upsert(record).futureValue
    downgradeToLegacyStrings(record)

    migration.run().futureValue shouldBe 2L
    migration.run().futureValue shouldBe 0L

  "run leaves a record already stored as BSON Date untouched" in:
    val record: AgentApplication = tdAll.agentApplicationLlp.afterStarted
    repo.upsert(record).futureValue

    migration.run().futureValue shouldBe 0L

    repo.findById(record.agentApplicationId).futureValue.value shouldBe record

  "run is safe when two instances run it at the same time — each value is converted exactly once and the values round-trip" in:
    val record: AgentApplication = tdAll.agentApplicationLlp.afterStarted
    repo.upsert(record).futureValue
    downgradeToLegacyStrings(record)

    val runA: Future[Long] = migration.run()
    val runB: Future[Long] = migration.run()

    runA.futureValue + runB.futureValue shouldBe 2L withClue "each of the two values must be converted by exactly one of the two runs"
    repo.findById(record.agentApplicationId).futureValue.value shouldBe record

  "run leaves a malformed value as it is, still converts the other fields and completes without failing" in:
    val record: AgentApplication = tdAll.agentApplicationLlp.afterStarted
    repo.upsert(record).futureValue
    downgradeToLegacyStrings(record)
    setRawStrings(
      repo.collection,
      record.agentApplicationId.value,
      Seq("applicationExpiresAt" -> "not-a-date")
    )

    migration.run().futureValue shouldBe 1L

    rawApplication(record).get("createdAt").getBsonType shouldBe BsonType.DATE_TIME
    rawApplication(record).get("applicationExpiresAt").getBsonType shouldBe BsonType.STRING
    migration.run().futureValue shouldBe 0L
