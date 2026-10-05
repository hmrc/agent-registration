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

import org.apache.pekko.stream.Materializer
import org.apache.pekko.stream.scaladsl.Source
import org.bson.BsonDocument
import org.bson.BsonType
import org.bson.conversions.Bson
import org.mongodb.scala.Document
import org.mongodb.scala.MongoCollection
import org.mongodb.scala.SingleObservableFuture
import org.mongodb.scala.model.Filters
import play.api.Logging
import uk.gov.hmrc.agentregistration.config.AppConfig
import uk.gov.hmrc.agentregistration.repository.providedetails.llp.IndividualProvidedDetailsRepo
import uk.gov.hmrc.agentregistration.util.ProcessInSequence
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.lock.LockService
import uk.gov.hmrc.mongo.lock.MongoLockRepository

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.duration.*
import scala.util.Failure
import scala.util.Success

/** One-off migration that rewrites the timestamps and the corrective action expiry date of the `agent-application` and `individual` collections from
  * Play-JSON-default ISO string to BSON `Date`.
  */
@Singleton
class AgentApplicationDatesMigration @Inject() (
  mongoComponent: MongoComponent,
  mongoLockRepository: MongoLockRepository,
  appConfig: AppConfig
)(using
  ec: ExecutionContext,
  materializer: Materializer
)
extends Logging:

  private val logPrefix: String = "[AgentApplicationDatesMigration]"

  private val fieldNamesByCollectionName: Seq[(String, Seq[String])] = Seq(
    AgentApplicationRepo.collectionName -> Seq(
      "createdAt",
      "applicationExpiresAt",
      "submittedAt",
      "riskingOutcomeApplication.reSubmittedAt",
      "riskingOutcomeApplication.correctiveActionExpiryDate"
    ),
    IndividualProvidedDetailsRepo.collectionName -> Seq("createdAt")
  )

  private val lockService: LockService = LockService(
    mongoLockRepository,
    lockId = "agent-application-dates-migration-lock",
    ttl = 1.hour
  )

  if appConfig.Migration.agentApplicationDatesToBsonDateEnabled then
    logger.warn(s"$logPrefix migration is starting...")
    migrateWithLock(body = runMigration())
  else
    countRemaining().onComplete:
      case Success(count) => logger.warn(s"$logPrefix migration is disabled, $count records left to migrate")
      case Failure(throwable) => logger.warn(s"$logPrefix migration is disabled, could not count records left to migrate: ${throwable.getMessage}")

  def countRemaining(): Future[Long] = ProcessInSequence
    .processInSequence(fieldNamesByCollectionName):
      (
        collectionName,
        fieldNames
      ) =>
        collection(collectionName).countDocuments(anyLegacyString(fieldNames)).toFuture()
    .map(_.sum)

  def runMigration(rate: Int = appConfig.Migration.agentApplicationDatesToBsonDateRatePerSecond): Future[Long] = countRemaining()
    .flatMap: before =>
      logger.warn(s"$logPrefix migrating started, $before records to migrate")
      ProcessInSequence.processInSequence(fieldNamesByCollectionName):
        (
          collectionName,
          fieldNames
        ) =>
          migrateCollection(
            collectionName,
            fieldNames,
            rate
          )
    .flatMap: convertedCounts =>
      val convertedCount: Long = convertedCounts.sum
      countRemaining().map: after =>
        logger.warn(s"$logPrefix migration completed, converted $convertedCount values, $after records left to migrate")
        convertedCount

  private def migrateCollection(
    collectionName: String,
    fieldNames: Seq[String],
    rate: Int
  ): Future[Long] =
    Source
      .fromPublisher(collection(collectionName).find(anyLegacyString(fieldNames)))
      .throttle(rate, 1.second)
      .mapAsync(parallelism = 1)(document =>
        migrateOne(
          collectionName,
          fieldNames,
          document
        )
      )
      .runFold(0L)(_ + _)

  // Each field has its own update, applied only while that field is still a string. A concurrent run on another instance then yields a no-op, and a nested
  // field is never set on a record that does not have it (which would add an empty parent object and make the record unreadable).
  private def migrateOne(
    collectionName: String,
    fieldNames: Seq[String],
    document: Document
  ): Future[Long] = ProcessInSequence
    .processInSequence(fieldNames): fieldName =>
      collection(collectionName)
        .updateOne(
          filter = Filters.and(Filters.eq("_id", document("_id")), isLegacyString(fieldName)),
          update = Seq(toBsonDate(fieldName))
        )
        .toFuture()
        .map(_.getModifiedCount)
    .map(_.sum)

  private def migrateWithLock(body: => Future[Long]): Unit =
    logger.warn(s"$logPrefix Attempting to take lock for migration...")
    lockService.withLock(body).onComplete:
      case Success(Some(convertedCount)) => logger.warn(s"$logPrefix Finished, converted $convertedCount values. Lock has been released.")
      case Success(None) => logger.warn(s"$logPrefix Failed to take lock")
      case Failure(throwable) => logger.error(s"$logPrefix migration failed: ${throwable.getMessage}", throwable)

  private def isLegacyString(fieldName: String): Bson = Filters.bsonType(fieldName, BsonType.STRING)

  private def anyLegacyString(fieldNames: Seq[String]): Bson = Filters.or(fieldNames.map(isLegacyString)*)

  // A value that cannot be converted is left as it is (onError).
  private def toBsonDate(fieldName: String): Bson = BsonDocument.parse(
    s"""{ "$$set": { "$fieldName": { "$$convert": { "input": "$$$fieldName", "to": "date", "onError": "$$$fieldName" } } } }"""
  )

  private def collection(collectionName: String): MongoCollection[Document] = mongoComponent.database.getCollection(collectionName)
