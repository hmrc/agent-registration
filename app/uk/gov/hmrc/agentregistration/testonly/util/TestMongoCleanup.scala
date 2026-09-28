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

package uk.gov.hmrc.agentregistration.testonly.util

import org.mongodb.scala.Document
import org.mongodb.scala.ObservableFuture
import org.mongodb.scala.model.Filters
import uk.gov.hmrc.agentregistration.repository.AgentApplicationRepo
import uk.gov.hmrc.agentregistration.repository.providedetails.llp.IndividualProvidedDetailsRepo
import uk.gov.hmrc.mongo.MongoComponent

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@Singleton
class TestMongoCleanup @Inject() (
  mongoComponent: MongoComponent
)(using ec: ExecutionContext):

  private def deleteAll(collectionName: String): Future[Unit] = mongoComponent.database
    .getCollection(collectionName)
    .deleteMany(Document())
    .toFuture()
    .map(_ => ())

  private def deleteByField(
    collectionName: String,
    fieldName: String,
    values: Seq[String]
  ): Future[Unit] =
    if values.isEmpty then
      Future.unit
    else
      mongoComponent.database
        .getCollection(collectionName)
        .deleteMany(
          Filters.in(fieldName, values*)
        )
        .toFuture()
        .map(_ => ())

  private def deleteIndividualsForApplications(
    agentApplicationIds: Seq[String]
  ): Future[Unit] = deleteByField(
    IndividualProvidedDetailsRepo.collectionName,
    "agentApplicationId",
    agentApplicationIds
  )

  private def deleteApplications(
    agentApplicationIds: Seq[String]
  ): Future[Unit] = deleteByField(
    AgentApplicationRepo.collectionName,
    "_id",
    agentApplicationIds
  )

  def deleteApplicationsAndIndividuals(
    agentApplicationIds: Seq[String]
  ): Future[Unit] =
    if agentApplicationIds.isEmpty then
      Future.unit
    else
      for
        _ <- deleteIndividualsForApplications(agentApplicationIds)
        _ <- deleteApplications(agentApplicationIds)
      yield ()

  def deleteAllIndividuals: Future[Unit] = deleteAll(IndividualProvidedDetailsRepo.collectionName)

  def deleteAllApplications: Future[Unit] = deleteAll(AgentApplicationRepo.collectionName)
