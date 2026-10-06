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
import org.bson.BsonValue
import org.bson.conversions.Bson
import org.mongodb.scala.MongoCollection
import org.mongodb.scala.SingleObservableFuture
import org.mongodb.scala.model.Filters
import org.mongodb.scala.model.Updates
import play.api.libs.json.JsDefined
import play.api.libs.json.JsLookupResult
import play.api.libs.json.JsObject
import uk.gov.hmrc.agentregistration.repository.providedetails.llp.IndividualProvidedDetailsRepo
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.AgentApplicationFormat
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetails
import uk.gov.hmrc.agentregistration.testsupport.ISpec

class DatesMigratorSpec
extends ISpec:

  "migrate converts the dates of a started application and adds no risking outcome" in:
    val record: AgentApplication = tdAll.agentApplicationLlp.afterStarted
    storeWithIsoStringDates(record)

    migrator.migrate().futureValue shouldBe 1L

    dateTypes(rawApplication(record)) shouldBe Map(
      "createdAt" -> BsonType.DATE_TIME,
      "applicationExpiresAt" -> BsonType.DATE_TIME
    )
    rawApplication(record).containsKey("riskingOutcomeApplication") shouldBe false
    repo.findById(record.agentApplicationId).futureValue.value shouldBe record

  "migrate converts every date of a resubmitted application, the nested risking outcome dates included" in:
    val record: AgentApplication = tdAll.agentApplicationLlp.afterResubmitted
    storeWithIsoStringDates(record)

    migrator.migrate().futureValue shouldBe 1L

    dateTypes(rawApplication(record)) shouldBe Map(
      "createdAt" -> BsonType.DATE_TIME,
      "submittedAt" -> BsonType.DATE_TIME,
      "riskingOutcomeApplication.actualDecisionDate" -> BsonType.DATE_TIME,
      "riskingOutcomeApplication.reSubmittedAt" -> BsonType.DATE_TIME,
      "riskingOutcomeApplication.correctiveActionExpiryDate" -> BsonType.DATE_TIME
    )
    repo.findById(record.agentApplicationId).futureValue.value shouldBe record

  "migrate converts only the nested dates that are present" in:
    val record: AgentApplication = tdAll.agentApplicationLlp.afterRiskingCompletedFixable
    storeWithIsoStringDates(record)

    migrator.migrate().futureValue shouldBe 1L

    dateTypes(rawApplication(record)) shouldBe Map(
      "createdAt" -> BsonType.DATE_TIME,
      "submittedAt" -> BsonType.DATE_TIME,
      "riskingOutcomeApplication.actualDecisionDate" -> BsonType.DATE_TIME,
      "riskingOutcomeApplication.correctiveActionExpiryDate" -> BsonType.DATE_TIME
    )
    repo.findById(record.agentApplicationId).futureValue.value shouldBe record

  "migrate converts the dates of both applications and individuals" in:
    val application: AgentApplication = tdAll.agentApplicationLlp.afterStarted
    val individual: IndividualProvidedDetails = tdAll.providedDetails.afterFinished
    storeWithIsoStringDates(application)
    storeWithIsoStringDates(individual)

    migrator.migrate().futureValue shouldBe 2L

    rawIndividual(individual).get("createdAt").getBsonType shouldBe BsonType.DATE_TIME
    individualRepo.findById(individual.individualProvidedDetailsId).futureValue.value shouldBe individual

  "migrate converts the individuals when converting the applications fails" in:
    val application: AgentApplication = tdAll.agentApplicationLlp.afterStarted
    val individual: IndividualProvidedDetails = tdAll.providedDetails.afterFinished
    storeWithIsoStringDates(application)
    storeWithIsoStringDates(individual)
    makeApplicationsUpdateFail()

    migrator.migrate().futureValue shouldBe 1L

    dateTypes(rawApplication(application)) shouldBe Map(
      "createdAt" -> BsonType.STRING,
      "applicationExpiresAt" -> BsonType.STRING
    )
    rawIndividual(individual).get("createdAt").getBsonType shouldBe BsonType.DATE_TIME

  "migrate converts nothing when every date is a BSON date already" in:
    repo.upsert(tdAll.agentApplicationLlp.afterStarted).futureValue

    migrator.migrate().futureValue shouldBe 0L

  "migrate keeps only the milliseconds of a date stored with more precision, as a BSON date holds no more" in:
    val record: AgentApplication = tdAll.agentApplicationLlp.afterStarted
    storeWithIsoStringDates(record)
    setRawString(
      collection = repo.collection,
      id = record.agentApplicationId.value,
      field = "createdAt",
      value = record.createdAt.plusNanos(123456L).toString
    )

    migrator.migrate().futureValue shouldBe 1L

    repo.findById(record.agentApplicationId).futureValue.value shouldBe record

  "migrate leaves a date it cannot convert and converts the others" in:
    val record: AgentApplication = tdAll.agentApplicationLlp.afterStarted
    storeWithIsoStringDates(record)
    setRawString(
      collection = repo.collection,
      id = record.agentApplicationId.value,
      field = "applicationExpiresAt",
      value = "not-a-date"
    )

    migrator.migrate().futureValue shouldBe 1L

    dateTypes(rawApplication(record)) shouldBe Map(
      "createdAt" -> BsonType.DATE_TIME,
      "applicationExpiresAt" -> BsonType.STRING
    )

  private lazy val repo: AgentApplicationRepo = app.injector.instanceOf[AgentApplicationRepo]
  private lazy val individualRepo: IndividualProvidedDetailsRepo = app.injector.instanceOf[IndividualProvidedDetailsRepo]
  private lazy val migrator: DatesMigrator = app.injector.instanceOf[DatesMigrator]

  private val applicationDateFields: Seq[String] = Seq(
    "createdAt",
    "applicationExpiresAt",
    "submittedAt",
    "riskingOutcomeApplication.actualDecisionDate",
    "riskingOutcomeApplication.reSubmittedAt",
    "riskingOutcomeApplication.correctiveActionExpiryDate"
  )

  // stores the record as written before the migration: its dates as the ISO strings the REST format writes
  private def storeWithIsoStringDates(record: AgentApplication): Unit =
    repo.upsert(record).futureValue
    holdAsIsoStrings(
      collection = repo.collection,
      id = record.agentApplicationId.value,
      restJson = AgentApplicationFormat.restFormat.writes(record),
      dateFields = applicationDateFields
    )

  private def storeWithIsoStringDates(record: IndividualProvidedDetails): Unit =
    individualRepo.upsert(record).futureValue
    holdAsIsoStrings(
      collection = individualRepo.collection,
      id = record.individualProvidedDetailsId.value,
      restJson = IndividualProvidedDetails.restFormat.writes(record),
      dateFields = Seq("createdAt")
    )

  private def holdAsIsoStrings(
    collection: MongoCollection[?],
    id: String,
    restJson: JsObject,
    dateFields: Seq[String]
  ): Unit =
    val updates: Seq[Bson] = dateFields.flatMap(field => isoString(restJson, field).map(value => Updates.set(field, value)))
    collection
      .updateOne(Filters.eq("_id", id), Updates.combine(updates*))
      .toFuture()
      .futureValue
    ()

  private def isoString(
    restJson: JsObject,
    path: String
  ): Option[String] = path.split('.').foldLeft[JsLookupResult](JsDefined(restJson))(_ \ _).asOpt[String]

  private def setRawString(
    collection: MongoCollection[?],
    id: String,
    field: String,
    value: String
  ): Unit =
    collection
      .updateOne(Filters.eq("_id", id), Updates.set(field, value))
      .toFuture()
      .futureValue
    ()

  private def makeApplicationsUpdateFail(): Unit =
    mongoDatabase
      .runCommand(BsonDocument.parse(s"""{ "collMod": "${AgentApplicationRepo.collectionName}", "validator": { "createdAt": { "$$type": "string" } } }"""))
      .toFuture()
      .futureValue
    ()

  private def rawApplication(record: AgentApplication): BsonDocument = rawDocument(repo.collection, record.agentApplicationId.value)

  private def rawIndividual(record: IndividualProvidedDetails): BsonDocument = rawDocument(individualRepo.collection, record.individualProvidedDetailsId.value)

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

  private def dateTypes(document: BsonDocument): Map[String, BsonType] =
    applicationDateFields
      .flatMap(field => rawValue(document, field).map(value => field -> value.getBsonType))
      .toMap

  private def rawValue(
    document: BsonDocument,
    path: String
  ): Option[BsonValue] =
    path.split('.').foldLeft(Option[BsonValue](document)): (value, name) =>
      value.filter(_.isDocument).flatMap(parent => Option(parent.asDocument().get(name)))
