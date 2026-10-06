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
import org.mongodb.scala.SingleObservableFuture
import org.mongodb.scala.model.Filters
import play.api.Logging
import uk.gov.hmrc.agentregistration.repository.providedetails.llp.IndividualProvidedDetailsRepo
import uk.gov.hmrc.agentregistration.util.ProcessInSequence
import uk.gov.hmrc.mongo.MongoComponent

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

/** Rewrites the timestamps and the corrective action expiry date of the `agent-application` and `individual` collections from Play-JSON-default ISO string to
  * BSON `Date`, one `updateMany` per field. Scheduled by [[uk.gov.hmrc.agentregistration.scheduler.AgentRegistrationSchedulerInitializer]].
  */
@Singleton
class AgentApplicationDatesMigration @Inject() (mongoComponent: MongoComponent)(using ec: ExecutionContext)
extends Logging:

  private val logPrefix: String = "[AgentApplicationDatesMigration]"

  private val fieldNamesByCollectionName: Seq[(String, String)] = Seq(
    AgentApplicationRepo.collectionName -> "createdAt",
    AgentApplicationRepo.collectionName -> "applicationExpiresAt",
    AgentApplicationRepo.collectionName -> "submittedAt",
    AgentApplicationRepo.collectionName -> "riskingOutcomeApplication.reSubmittedAt",
    AgentApplicationRepo.collectionName -> "riskingOutcomeApplication.correctiveActionExpiryDate",
    IndividualProvidedDetailsRepo.collectionName -> "createdAt"
  )

  def run(): Future[Long] = ProcessInSequence
    .processInSequence(fieldNamesByCollectionName):
      (
        collectionName,
        fieldName
      ) =>
        convertToBsonDate(collectionName, fieldName)
    .map(_.sum)
    .map: modifiedCount =>
      logger.warn(s"$logPrefix migration run completed, modified $modifiedCount values")
      modifiedCount

  private def convertToBsonDate(
    collectionName: String,
    fieldName: String
  ): Future[Long] = mongoComponent
    .database
    .getCollection(collectionName)
    .updateMany(
      filter = Filters.bsonType(fieldName, BsonType.STRING),
      update = Seq(toBsonDate(fieldName))
    )
    .toFuture()
    .map: updateResult =>
      logger.warn(s"$logPrefix $collectionName.$fieldName: matched ${updateResult.getMatchedCount} records, modified ${updateResult.getModifiedCount}")
      updateResult.getModifiedCount

  // A value that cannot be converted is left as it is (onError); it then shows up as matched but not modified.
  private def toBsonDate(fieldName: String): Bson = BsonDocument.parse(
    s"""{ "$$set": { "$fieldName": { "$$convert": { "input": "$$$fieldName", "to": "date", "onError": "$$$fieldName" } } } }"""
  )
