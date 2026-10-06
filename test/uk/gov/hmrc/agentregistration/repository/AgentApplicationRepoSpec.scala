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
import org.bson.BsonValue
import org.bson.conversions.Bson
import org.mongodb.scala.SingleObservableFuture
import org.mongodb.scala.model.Filters
import org.mongodb.scala.model.Updates
import play.api.libs.json.JsDefined
import play.api.libs.json.JsLookupResult
import uk.gov.hmrc.agentregistration.shared.ApplicationState.SentForRisking
import uk.gov.hmrc.agentregistration.shared.ApplicationState.SentToMinerva
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.AgentApplicationId
import uk.gov.hmrc.agentregistration.shared.ApplicationReference
import uk.gov.hmrc.agentregistration.shared.InternalUserId
import uk.gov.hmrc.agentregistration.shared.LinkId
import uk.gov.hmrc.agentregistration.testsupport.ISpec

import java.time.Instant
import java.time.ZoneOffset

class AgentApplicationRepoSpec
extends ISpec:

  private lazy val repo: AgentApplicationRepo = app.injector.instanceOf[AgentApplicationRepo]

  "updateManyApplicationStateByReference should set the state in all selected applications and leave others untouched" in:
    val record: AgentApplication = tdAll.agentApplicationLlp.afterSentForRisking
    val record2: AgentApplication = tdAll.agentApplicationLlp.afterSentForRisking.copy(
      _id = AgentApplicationId("agent-application-id-23456"),
      applicationReference = ApplicationReference("APPREF234"),
      internalUserId = InternalUserId("internal-user-id-23456"),
      linkId = LinkId("link-id-23456")
    )
    val record3: AgentApplication = tdAll.agentApplicationLlp.afterSentForRisking.copy(
      _id = AgentApplicationId("agent-application-id-34567"),
      applicationReference = ApplicationReference("APPREF345"),
      internalUserId = InternalUserId("internal-user-id-34567"),
      linkId = LinkId("link-id-34567")
    )
    repo.upsert(record).futureValue
    repo.upsert(record2).futureValue
    repo.upsert(record3).futureValue

    repo.findById(record.agentApplicationId).futureValue.value.applicationState shouldBe SentForRisking withClue "sanity check"

    repo.updateManyApplicationStateByReference(Seq(record.applicationReference, record2.applicationReference), SentToMinerva).futureValue

    val updatedRecord = repo.findByApplicationReference(record.applicationReference).futureValue.value
    updatedRecord.applicationState shouldBe SentToMinerva withClue "application state for record 1 should be updated"
    val updatedRecord2 = repo.findByApplicationReference(record2.applicationReference).futureValue.value
    updatedRecord2.applicationState shouldBe SentToMinerva withClue "application state for record 2 should be updated"
    val updatedRecord3 = repo.findByApplicationReference(record3.applicationReference).futureValue.value
    updatedRecord3 shouldBe record3 withClue "application state for record 3 should be untouched"

  // the fields the Mongo format stores as BSON Date and the migration converts; afterStarted and afterResubmitted between them carry all of them
  private val bsonDateFieldNames: Seq[String] = Seq(
    "createdAt",
    "applicationExpiresAt",
    "submittedAt",
    "riskingOutcomeApplication.actualDecisionDate",
    "riskingOutcomeApplication.reSubmittedAt",
    "riskingOutcomeApplication.correctiveActionExpiryDate"
  )

  private val recordsWithDates: Seq[(String, AgentApplication)] = Seq(
    "afterStarted" -> tdAll.agentApplicationLlp.afterStarted,
    "afterResubmitted" -> tdAll.agentApplicationLlp.afterResubmitted
  )

  private def rawDocument(record: AgentApplication): BsonDocument =
    repo
      .collection
      .withDocumentClass[BsonDocument]()
      .find(Filters.eq("_id", record.agentApplicationId.value))
      .headOption()
      .futureValue
      .value

  private def rawValue(
    document: BsonDocument,
    fieldName: String
  ): Option[BsonValue] =
    fieldName.split('.').foldLeft(Option[BsonValue](document)): (value, name) =>
      value.filter(_.isDocument).flatMap(parent => Option(parent.asDocument().get(name)))

  private def restJsonValue(
    record: AgentApplication,
    fieldName: String
  ): Option[String] = fieldName.split('.').foldLeft[JsLookupResult](JsDefined(AgentApplication.restFormat.writes(record)))(_ \ _).asOpt[String]

  private def storedBsonDateFieldNames(record: AgentApplication): Seq[String] =
    val document: BsonDocument = rawDocument(record)
    bsonDateFieldNames.filter(fieldName => rawValue(document, fieldName).isDefined)

  recordsWithDates.foreach: (name, record) =>
    s"upsert stores every date field of $name that the migration covers as BSON Date, not as a string" in:
      repo.upsert(record).futureValue
      val document: BsonDocument = rawDocument(record)
      val storedFieldNames: Seq[String] = storedBsonDateFieldNames(record)

      storedFieldNames should not be empty
      storedFieldNames.foreach: fieldName =>
        withClue(s"$fieldName: "):
          rawValue(document, fieldName).value.getBsonType shouldBe BsonType.DATE_TIME

    // TODO: remove with the ISO-string fallback in MongoDateFormats once the dates migration has run in every environment
    s"findById reads every date field of $name that the migration covers when it is still stored as a legacy ISO string" in:
      repo.upsert(record).futureValue
      val legacyStrings: Seq[Bson] = storedBsonDateFieldNames(record).map(fieldName => Updates.set(fieldName, restJsonValue(record, fieldName).value))
      repo
        .collection
        .updateOne(Filters.eq("_id", record.agentApplicationId.value), Updates.combine(legacyStrings*))
        .toFuture()
        .futureValue

      repo.findById(record.agentApplicationId).futureValue.value shouldBe record

  "afterStarted and afterResubmitted between them store every date field the migration covers" in:
    // both states share an _id, so each is upserted and inspected in turn
    val storedFieldNames: Set[String] =
      recordsWithDates.flatMap: (_, record) =>
        repo.upsert(record).futureValue
        storedBsonDateFieldNames(record)
      .toSet

    storedFieldNames shouldBe bsonDateFieldNames.toSet

  private final case class DateQueryCase(
    description: String,
    record: AgentApplication,
    fieldName: String,
    storedInstant: Instant
  )

  private val dateQueryCases: Seq[DateQueryCase] = Seq(
    DateQueryCase(
      description = "a started application",
      record = tdAll.agentApplicationLlp.afterStarted,
      fieldName = "createdAt",
      storedInstant = tdAll.nowAsInstant
    ),
    DateQueryCase(
      description = "a started application",
      record = tdAll.agentApplicationLlp.afterStarted,
      fieldName = "applicationExpiresAt",
      storedInstant = tdAll.applicationExpiresAtAsInstant
    ),
    DateQueryCase(
      description = "a submitted application",
      record = tdAll.agentApplicationLlp.afterSentForRisking,
      fieldName = "submittedAt",
      storedInstant = tdAll.nowAsInstant
    ),
    DateQueryCase(
      description = "an application with a fixable outcome",
      record = tdAll.agentApplicationLlp.afterRiskingCompletedFixable,
      fieldName = "riskingOutcomeApplication.actualDecisionDate",
      storedInstant = tdAll.riskingCompletedDate.atStartOfDay(ZoneOffset.UTC).toInstant
    ),
    DateQueryCase(
      description = "an application with a fixable outcome",
      record = tdAll.agentApplicationLlp.afterRiskingCompletedFixable,
      fieldName = "riskingOutcomeApplication.correctiveActionExpiryDate",
      storedInstant = tdAll.correctiveActionExpiryDate.atStartOfDay(ZoneOffset.UTC).toInstant
    ),
    DateQueryCase(
      description = "a resubmitted application",
      record = tdAll.agentApplicationLlp.afterResubmitted,
      fieldName = "riskingOutcomeApplication.reSubmittedAt",
      storedInstant = tdAll.nowAsInstant
    )
  )

  private def countWhere(filter: Bson): Long =
    repo
      .collection
      .countDocuments(filter)
      .toFuture()
      .futureValue

  dateQueryCases.foreach: dateQueryCase =>
    s"${dateQueryCase.fieldName} of ${dateQueryCase.description} can be queried with Mongo date operators" in:
      repo.upsert(dateQueryCase.record).futureValue

      countWhere(Filters.lt(dateQueryCase.fieldName, dateQueryCase.storedInstant.plusMillis(1))) shouldBe 1L withClue "a later date matches"
      countWhere(Filters.lt(dateQueryCase.fieldName, dateQueryCase.storedInstant)) shouldBe 0L withClue "the stored date itself is not less than itself"
