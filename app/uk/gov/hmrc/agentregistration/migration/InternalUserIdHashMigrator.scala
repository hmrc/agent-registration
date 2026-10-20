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
import org.bson.conversions.Bson
import org.mongodb.scala.MongoCollection
import org.mongodb.scala.ObservableFuture
import org.mongodb.scala.SingleObservableFuture
import org.mongodb.scala.model.Filters
import org.mongodb.scala.model.Projections
import org.mongodb.scala.model.UpdateOneModel
import org.mongodb.scala.model.Updates
import org.mongodb.scala.model.WriteModel
import play.api.Logging
import uk.gov.hmrc.agentregistration.repository.FieldNames
import uk.gov.hmrc.agentregistration.crypto.Hasher
import uk.gov.hmrc.agentregistration.crypto.FieldLevelEncryption
import uk.gov.hmrc.agentregistration.migration.InternalUserIdHashMigrator.*
import uk.gov.hmrc.agentregistration.repository.AgentApplicationRepo
import uk.gov.hmrc.agentregistration.repository.providedetails.llp.IndividualProvidedDetailsRepo
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.===

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

// TODO: remove, with InternalUserIdHashMigratorInitializer and the internal-user-id-hash-migrator config, once the migration has run in every environment
@Singleton
class InternalUserIdHashMigrator @Inject() (
  agentApplicationRepo: AgentApplicationRepo,
  individualProvidedDetailsRepo: IndividualProvidedDetailsRepo,
  fieldLevelEncryption: FieldLevelEncryption,
  hasher: Hasher
)(using ExecutionContext)
extends Logging:

  /** @return the number of documents given a hash; a collection whose migration failed adds none */
  def migrate(): Future[Long] =
    for
      applications <- migrateCollection(agentApplicationRepo.collection)
      individuals <- migrateCollection(individualProvidedDetailsRepo.collection)
    yield applications + individuals

  private def migrateCollection(collection: MongoCollection[?]): Future[Long] =
    val collectionName: String = collection.namespace.getCollectionName
    logger.info(s"Migrating '$collectionName': Started...")
    val documents: MongoCollection[Document] = collection.withDocumentClass[Document]()

    @SuppressWarnings(Array("org.wartremover.warts.Recursion"))
    def run(
      runNumber: Int,
      quietRuns: Int,
      hashed: Long
    ): Future[Long] = hashBatch(documents).flatMap: hashedInRun =>
      logger.info(s"Migrating '$collectionName': hashed $hashedInRun documents in run $runNumber")
      val quietRunsNow: Int = if hashedInRun === 0L then quietRuns + 1 else 0
      if quietRunsNow === 2
      then Future.successful(hashed)
      else
        run(
          runNumber = runNumber + 1,
          quietRuns = quietRunsNow,
          hashed = hashed + hashedInRun
        )

    run(
      runNumber = 1,
      quietRuns = 0,
      hashed = 0L
    )
      .flatMap: hashed =>
        logger.info(s"Migrating '$collectionName': hashed $hashed documents in total DONE")
        logDocumentsLeft(documents, collectionName).map(_ => hashed)
      .recover:
        case ex =>
          logger.error(s"Migrating '$collectionName': FAILED", ex)
          0L

  private def logDocumentsLeft(
    documents: MongoCollection[Document],
    collectionName: String
  ): Future[Unit] = documents
    .countDocuments(needsHash)
    .toFuture()
    .map: left =>
      if left === 0L
      then logger.info(s"Migrating '$collectionName': no documents left without a hash")
      else logger.error(s"Migrating '$collectionName': $left documents still have no hash")

  private def hashBatch(documents: MongoCollection[Document]): Future[Long] = documents
    .find(needsHash)
    .projection(idAndInternalUserId)
    .limit(batchSize)
    .toFuture()
    .flatMap: batch =>
      val updates: Seq[WriteModel[? <: Document]] = batch.map(hashUpdate)
      if updates.isEmpty
      then Future.successful(0L)
      else
        documents
          .bulkWrite(updates)
          .toFuture()
          .map(_.getModifiedCount.toLong)

  private def hashUpdate(document: Document): WriteModel[? <: Document] = UpdateOneModel(
    Filters.eq("_id", document.get("_id")),
    Updates.set(
      FieldNames.internalUserIdHash,
      hasher.hash(fieldLevelEncryption.decrypt(document.getString(FieldNames.internalUserId)))
    )
  )

object InternalUserIdHashMigrator:

  private val batchSize: Int = 500

  /** An individual who has not signed in has no internal user id and so never gets a hash: excluding those here rather than skipping them once read is what
    * stops every run selecting the same documents forever.
    */
  private val needsHash: Bson = Filters.and(
    Filters.exists(FieldNames.internalUserIdHash, false),
    Filters.exists(FieldNames.internalUserId, true)
  )

  private val idAndInternalUserId: Bson = Projections.include("_id", FieldNames.internalUserId)
