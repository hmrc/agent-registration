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
import org.bson.conversions.Bson
import org.mongodb.scala.MongoCollection
import org.mongodb.scala.SingleObservableFuture
import org.mongodb.scala.model.Filters
import play.api.Logging
import uk.gov.hmrc.agentregistration.repository.DatesMigrator.*
import uk.gov.hmrc.agentregistration.repository.providedetails.llp.IndividualProvidedDetailsRepo
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.===

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

// TODO: remove, with DatesMigratorStarter and the dates-migrator config, once the dates migration has run in every environment
/** Converts the dates that `agent-application` and `individual` records hold as ISO strings into BSON dates. */
@Singleton
class DatesMigrator @Inject() (
  agentApplicationRepo: AgentApplicationRepo,
  individualProvidedDetailsRepo: IndividualProvidedDetailsRepo
)(using ExecutionContext)
extends Logging:

  /** @return the number of documents converted; a collection whose migration failed adds none */
  def migrate(): Future[Long] =
    for
      applications <- runUntilTwoQuietRuns(
        collection = agentApplicationRepo.collection,
        filter = applicationsWithStringDates,
        update = applicationDatesConverted
      )
        .map: converted =>
          logger.info(s"Migrating '${AgentApplicationRepo.collectionName}': converted $converted documents in total DONE")
          converted
        .recover:
          case ex =>
            logger.error(s"Migrating '${AgentApplicationRepo.collectionName}': FAILED", ex)
            0L
      individuals <- runUntilTwoQuietRuns(
        collection = individualProvidedDetailsRepo.collection,
        filter = individualsWithStringDates,
        update = individualDatesConverted
      )
        .map: converted =>
          logger.info(s"Migrating '${IndividualProvidedDetailsRepo.collectionName}': converted $converted documents in total DONE")
          converted
        .recover:
          case ex =>
            logger.error(s"Migrating '${IndividualProvidedDetailsRepo.collectionName}': FAILED", ex)
            0L
    yield applications + individuals

  private def runUntilTwoQuietRuns(
    collection: MongoCollection[?],
    filter: Bson,
    update: Bson
  ): Future[Long] =
    val collectionName: String = collection.namespace.getCollectionName
    logger.info(s"Migrating '$collectionName': Started...")

    @SuppressWarnings(Array("org.wartremover.warts.Recursion"))
    def run(
      runNumber: Int,
      quietRuns: Int,
      converted: Long
    ): Future[Long] = collection
      .updateMany(filter = filter, update = Seq(update))
      .toFuture()
      .map(_.getModifiedCount)
      .flatMap: convertedInRun =>
        logger.info(s"Migrating '$collectionName': converted $convertedInRun documents in run $runNumber")
        val quietRunsNow: Int = if convertedInRun === 0L then quietRuns + 1 else 0
        if quietRunsNow === 2
        then Future.successful(converted)
        else
          run(
            runNumber = runNumber + 1,
            quietRuns = quietRunsNow,
            converted = converted + convertedInRun
          )

    run(
      runNumber = 1,
      quietRuns = 0,
      converted = 0L
    )

object DatesMigrator:

  private val applicationsWithStringDates: Bson = Filters.or(
    Filters.bsonType("createdAt", BsonType.STRING),
    Filters.bsonType("applicationExpiresAt", BsonType.STRING),
    Filters.bsonType("submittedAt", BsonType.STRING),
    Filters.bsonType("riskingOutcomeApplication.actualDecisionDate", BsonType.STRING),
    Filters.bsonType("riskingOutcomeApplication.reSubmittedAt", BsonType.STRING),
    Filters.bsonType("riskingOutcomeApplication.correctiveActionExpiryDate", BsonType.STRING)
  )

  // The nested dates are set through riskingOutcomeApplication, and only where it exists: setting the dotted fields would add an empty riskingOutcomeApplication the format cannot read.
  private val applicationDatesConverted: Bson = BsonDocument.parse(
    // language=JSON
    """{ "$set": {
      |  "createdAt": { "$convert": { "input": "$createdAt", "to": "date", "onError": "$createdAt", "onNull": "$createdAt" } },
      |  "applicationExpiresAt": { "$convert": { "input": "$applicationExpiresAt", "to": "date", "onError": "$applicationExpiresAt", "onNull": "$applicationExpiresAt" } },
      |  "submittedAt": { "$convert": { "input": "$submittedAt", "to": "date", "onError": "$submittedAt", "onNull": "$submittedAt" } },
      |  "riskingOutcomeApplication": { "$cond": [
      |    { "$eq": [{ "$type": "$riskingOutcomeApplication" }, "object"] },
      |    { "$mergeObjects": ["$riskingOutcomeApplication", {
      |      "actualDecisionDate": { "$convert": { "input": "$riskingOutcomeApplication.actualDecisionDate", "to": "date", "onError": "$riskingOutcomeApplication.actualDecisionDate", "onNull": "$riskingOutcomeApplication.actualDecisionDate" } },
      |      "reSubmittedAt": { "$convert": { "input": "$riskingOutcomeApplication.reSubmittedAt", "to": "date", "onError": "$riskingOutcomeApplication.reSubmittedAt", "onNull": "$riskingOutcomeApplication.reSubmittedAt" } },
      |      "correctiveActionExpiryDate": { "$convert": { "input": "$riskingOutcomeApplication.correctiveActionExpiryDate", "to": "date", "onError": "$riskingOutcomeApplication.correctiveActionExpiryDate", "onNull": "$riskingOutcomeApplication.correctiveActionExpiryDate" } }
      |    }] },
      |    "$riskingOutcomeApplication"
      |  ] }
      |} }""".stripMargin
  )

  private val individualsWithStringDates: Bson = Filters.bsonType("createdAt", BsonType.STRING)

  private val individualDatesConverted: Bson = BsonDocument.parse(
    // language=JSON
    """{ "$set": {
      |  "createdAt": { "$convert": { "input": "$createdAt", "to": "date", "onError": "$createdAt", "onNull": "$createdAt" } }
      |} }""".stripMargin
  )
