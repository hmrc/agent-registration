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
import org.mongodb.scala.ObservableFuture
import org.mongodb.scala.model.Filters
import org.mongodb.scala.model.Projections
import org.mongodb.scala.model.Updates
import play.api.Logging
import uk.gov.hmrc.agentregistration.config.AppConfig
import uk.gov.hmrc.agentregistration.migration.LegacyValueHasher
import uk.gov.hmrc.agentregistration.repository.AgentApplicationRepo
import uk.gov.hmrc.agentregistration.repository.providedetails.llp.IndividualProvidedDetailsRepo
import uk.gov.hmrc.mongo.MongoComponent

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@Singleton
class InternalUserIdHashMigration @Inject() (
  mongoComponent: MongoComponent,
  legacyValueHasher: LegacyValueHasher,
  appConfig: AppConfig
)(using ec: ExecutionContext)
extends Logging:

  private val field = "internalUserId"

  val startupRun: Future[Unit] =
    if appConfig.Migration.HashInternalUserId.runOnStartup
    then run
    else Future.unit

  def run: Future[Unit] =
    for
      _ <- migrateCollection(rawCollection(AgentApplicationRepo.collectionName), field)
      _ <- migrateCollection(rawCollection(IndividualProvidedDetailsRepo.collectionName), field)
    yield ()

  private def rawCollection(name: String): MongoCollection[Document] = mongoComponent.database.getCollection(name)

  private def migrateCollection(
    collection: org.mongodb.scala.MongoCollection[org.bson.Document],
    field: String
  ): Future[Unit] =
    val collectionName = collection.namespace.getCollectionName
    collection
      .find(
        // Only documents that have NOT yet been migrated (no hash: prefix)
        Filters.and(
          Filters.exists(field, true),
          Filters.not(Filters.regex(field, "^hash:"))
        )
      )
      .projection(Projections.include(field))
      .toFuture()
      .flatMap: docs =>
        logger.info(s"[$collectionName] Starting internalUserId hash migration for ${docs.size} documents")
        docs.grouped(500).toSeq.foldLeft(Future.unit): (acc, batch) =>
          acc.flatMap(_ =>
            Future.traverse(batch): doc =>
              val encrypted = doc.getString(field)
              val hashed = legacyValueHasher.decryptThenHash(encrypted)
              collection
                .updateOne(
                  Filters.eq("_id", doc.get("_id")),
                  Updates.set(field, hashed)
                )
                .toFuture()
                .map(_ => ())
            .map(_ => ())
          )
      .map: total =>
        logger.info(s"[$collectionName] Completed internalUserId hash migration: $total documents updated")
      .recover:
        case ex =>
          logger.error(s"[$collectionName] Failed during internalUserId hash migration", ex)
          throw ex
