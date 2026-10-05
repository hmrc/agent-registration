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
import org.bson.conversions.Bson
import org.mongodb.scala.Document
import org.mongodb.scala.MongoCollection
import org.mongodb.scala.SingleObservableFuture
import org.mongodb.scala.model.Filters
import play.api.Logging
import uk.gov.hmrc.agentregistration.config.AppConfig
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

/** One-off migration that rewrites `applicationExpiresAt` on the `agent-application` collection from Play-JSON-default ISO string to BSON `Date`.
  */
@Singleton
class AgentApplicationExpiresAtMigration @Inject() (
  mongoComponent: MongoComponent,
  mongoLockRepository: MongoLockRepository,
  appConfig: AppConfig
)(using
  ec: ExecutionContext,
  materializer: Materializer
)
extends Logging:

  private val logPrefix: String = "[AgentApplicationExpiresAtMigration]"
  private val legacyStringFilter: Bson = BsonDocument.parse("""{ "applicationExpiresAt": { "$type": "string" } }""")
  private val toBsonDatePipeline: Seq[Bson] = Seq(BsonDocument.parse("""{ "$set": { "applicationExpiresAt": { "$toDate": "$applicationExpiresAt" } } }"""))
  private val lockService: LockService = LockService(
    mongoLockRepository,
    lockId = "agent-application-applicationExpiresAt-migration-lock",
    ttl = 1.hour
  )

  if appConfig.Migration.applicationExpiresAtToBsonDateEnabled then
    logger.warn(s"$logPrefix migration is starting...")
    migrateWithLock(body = runMigration())
  else
    countRemaining().onComplete:
      case Success(count) => logger.warn(s"$logPrefix migration is disabled, $count records left to migrate")
      case Failure(throwable) => logger.warn(s"$logPrefix migration is disabled, could not count records left to migrate: ${throwable.getMessage}")

  def countRemaining(): Future[Long] = collection.countDocuments(legacyStringFilter).toFuture()

  def runMigration(rate: Int = appConfig.Migration.applicationExpiresAtToBsonDateRatePerSecond): Future[Long] = countRemaining()
    .flatMap: before =>
      logger.warn(s"$logPrefix migrating started, $before records to migrate")
      Source
        .fromPublisher(collection.find(legacyStringFilter))
        .throttle(rate, 1.second)
        .mapAsync(parallelism = 1)(migrateOne)
        .runFold(0L)(_ + _)
    .flatMap: modifiedCount =>
      countRemaining().map: after =>
        logger.warn(s"$logPrefix migration completed, modified $modifiedCount records, $after records left to migrate")
        modifiedCount

  // The `$type: "string"` filter is re-applied per record so that a concurrent run on another instance converting the same record yields a 0-modified no-op here.
  private def migrateOne(document: Document): Future[Long] = collection
    .updateOne(
      filter = Filters.and(Filters.eq("_id", document("_id")), legacyStringFilter),
      update = toBsonDatePipeline
    )
    .toFuture()
    .map(_.getModifiedCount)

  private def migrateWithLock(body: => Future[Long]): Unit =
    logger.warn(s"$logPrefix Attempting to take lock for migration...")
    lockService.withLock(body).onComplete:
      case Success(Some(modifiedCount)) => logger.warn(s"$logPrefix Finished, modified $modifiedCount records. Lock has been released.")
      case Success(None) => logger.warn(s"$logPrefix Failed to take lock")
      case Failure(throwable) => logger.error(s"$logPrefix migration failed: ${throwable.getMessage}", throwable)

  private def collection: MongoCollection[Document] = mongoComponent.database.getCollection(AgentApplicationRepo.collectionName)
