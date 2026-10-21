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
import org.mongodb.scala.SingleObservableFuture
import org.mongodb.scala.model.Filters
import org.mongodb.scala.model.Updates
import uk.gov.hmrc.agentregistration.repository.FieldNames
import uk.gov.hmrc.agentregistration.crypto.FieldLevelEncryption
import uk.gov.hmrc.agentregistration.repository.AgentApplicationRepo
import uk.gov.hmrc.agentregistration.repository.providedetails.llp.IndividualProvidedDetailsRepo
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.PersonReference
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetails
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetailsId
import uk.gov.hmrc.agentregistration.testsupport.ISpec

class InternalUserIdHashMigratorSpec
extends ISpec:

  override protected def configOverrides: Map[String, Any] = Map[String, Any](
    "field-level-encryption.enabled" -> true
  )

  "hash an application that has none" in:
    storeWithoutHash(application)

    migrator.migrate().futureValue shouldBe 1L

    hashOf(applications, application.agentApplicationId.value).value shouldBe internalUserIdHash

  "leave the encrypted internalUserId as it was" in:
    storeWithoutHash(application)

    migrator.migrate().futureValue shouldBe 1L

    encryptedInternalUserIdOf(applications, application.agentApplicationId.value).value shouldBe
      fieldLevelEncryption.encrypt(application.internalUserId.value)

  "hash an individual who has signed in" in:
    storeWithoutHash(signedInIndividual)

    migrator.migrate().futureValue shouldBe 1L

    hashOf(individuals, signedInIndividual._id.value).value shouldBe internalUserIdHash

  "leave an individual who has not signed in without a hash, and still hash the others" in:
    storeWithoutHash(notSignedInIndividual)
    storeWithoutHash(signedInIndividual)

    migrator.migrate().futureValue shouldBe 1L

    withClue("an individual with no internalUserId has nothing to hash: "):
      hashOf(individuals, notSignedInIndividual._id.value) shouldBe None
    hashOf(individuals, signedInIndividual._id.value).value shouldBe internalUserIdHash

  "leave no document that should have a hash without one" in:
    storeWithoutHash(application)
    storeWithoutHash(signedInIndividual)
    storeWithoutHash(notSignedInIndividual)

    migrator.migrate().futureValue shouldBe 2L

    countNeedingHash(applications) shouldBe 0L
    countNeedingHash(individuals) shouldBe 0L

  "count both collections" in:
    storeWithoutHash(application)
    storeWithoutHash(signedInIndividual)

    migrator.migrate().futureValue shouldBe 2L

  "hash nothing on a second run" in:
    storeWithoutHash(application)

    migrator.migrate().futureValue shouldBe 1L
    migrator.migrate().futureValue shouldBe 0L

  "not touch a document the write path has already hashed" in:
    agentApplicationRepo.upsert(application).futureValue
    val alreadyHashed: Option[String] = hashOf(applications, application.agentApplicationId.value)
    storeWithoutHash(signedInIndividual)

    withClue("only the individual is left to do: "):
      migrator.migrate().futureValue shouldBe 1L

    hashOf(applications, application.agentApplicationId.value) shouldBe alreadyHashed

  private lazy val migrator: InternalUserIdHashMigrator = app.injector.instanceOf[InternalUserIdHashMigrator]
  private lazy val agentApplicationRepo: AgentApplicationRepo = app.injector.instanceOf[AgentApplicationRepo]
  private lazy val individualProvidedDetailsRepo: IndividualProvidedDetailsRepo = app.injector.instanceOf[IndividualProvidedDetailsRepo]
  private lazy val fieldLevelEncryption: FieldLevelEncryption = app.injector.instanceOf[FieldLevelEncryption]

  private lazy val applications: MongoCollection[Document] = agentApplicationRepo.collection.withDocumentClass[Document]()
  private lazy val individuals: MongoCollection[Document] = individualProvidedDetailsRepo.collection.withDocumentClass[Document]()

  private val internalUserIdHash: String = "hash:iRGKF9SU4ftOBe8VJ8i+famfrXWuHB/tS4z4O4K4hUI="

  private lazy val application: AgentApplication = tdAll.agentApplicationLlp.afterAgentDetailsComplete
  private lazy val signedInIndividual: IndividualProvidedDetails = tdAll.providedDetails.afterStarted
  private lazy val notSignedInIndividual: IndividualProvidedDetails = tdAll.providedDetails.precreated
    .copy(
      _id = IndividualProvidedDetailsId(s"${tdAll.providedDetails.precreated._id.value}-not-signed-in"),
      personReference = PersonReference(s"${tdAll.providedDetails.precreated.personReference.value}-not-signed-in")
    )

  private def storeWithoutHash(agentApplication: AgentApplication): Unit =
    agentApplicationRepo.upsert(agentApplication).futureValue
    unsetHash(applications, agentApplication.agentApplicationId.value)

  private def storeWithoutHash(individualProvidedDetails: IndividualProvidedDetails): Unit =
    individualProvidedDetailsRepo.upsert(individualProvidedDetails).futureValue
    unsetHash(individuals, individualProvidedDetails._id.value)

  private def unsetHash(
    collection: MongoCollection[Document],
    id: String
  ): Unit =
    collection
      .updateOne(Filters.eq("_id", id), Updates.unset(FieldNames.internalUserIdHash))
      .toFuture()
      .futureValue
    ()

  private def countNeedingHash(collection: MongoCollection[Document]): Long =
    collection
      .countDocuments(Filters.and(
        Filters.exists(FieldNames.internalUserIdHash, false),
        Filters.exists(FieldNames.internalUserId, true)
      ))
      .toFuture()
      .futureValue

  private def hashOf(
    collection: MongoCollection[Document],
    id: String
  ): Option[String] = fieldOf(
    collection,
    id,
    FieldNames.internalUserIdHash
  )

  private def encryptedInternalUserIdOf(
    collection: MongoCollection[Document],
    id: String
  ): Option[String] = fieldOf(
    collection,
    id,
    "internalUserId"
  )

  private def fieldOf(
    collection: MongoCollection[Document],
    id: String,
    fieldName: String
  ): Option[String] = collection
    .find(Filters.eq("_id", id))
    .headOption()
    .futureValue
    .flatMap(document => Option(document.getString(fieldName)))
