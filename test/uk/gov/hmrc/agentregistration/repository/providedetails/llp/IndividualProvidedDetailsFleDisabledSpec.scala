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

package uk.gov.hmrc.agentregistration.repository.providedetails.llp

import org.mongodb.scala.SingleObservableFuture
import org.mongodb.scala.bson.collection.immutable.Document
import org.mongodb.scala.model.Filters
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetails
import uk.gov.hmrc.agentregistration.testsupport.ISpec

/** Regression for the local-dev path where field-level encryption is turned off.
  *
  * In deployed environments the flag is always on; locally it is off so engineers can read Mongo documents as plaintext. The whole encrypt/decrypt pipeline
  * must therefore be a no-op when the flag is false.
  */
class IndividualProvidedDetailsFleDisabledSpec
extends ISpec:

  override protected def configOverrides: Map[String, Any] = Map[String, Any](
    "field-level-encryption.enabled" -> false
  )

  private lazy val repo: IndividualProvidedDetailsRepo = app.injector.instanceOf[IndividualProvidedDetailsRepo]

  private def rawDocumentFor(record: IndividualProvidedDetails): Document =
    mongoComponent.database
      .getCollection("individual")
      .find(Filters.eq("_id", record._id.value))
      .first()
      .toFuture()
      .futureValue

  "with FLE disabled the PII fields are stored as plaintext on disk" in:
    val record: IndividualProvidedDetails = tdAll.providedDetails.afterFinished
    repo.upsert(record).futureValue

    val rawJson: String = rawDocumentFor(record).toJson()

    rawJson should include(record.individualName.value) withClue "individualName is plaintext"
    rawJson should include(record.getInternalUserId.value) withClue "internalUserId is plaintext"
    rawJson should include(record.getTelephoneNumber.value) withClue "telephoneNumber is plaintext"
    rawJson should include(record.getEmailAddress.emailAddress.value) withClue "emailAddress is plaintext"

  "with FLE disabled the round-trip via the repo returns the model unchanged" in:
    val record: IndividualProvidedDetails = tdAll.providedDetails.afterFinished
    repo.upsert(record).futureValue
    repo.findById(record._id).futureValue.value shouldBe record

  "with FLE disabled findByInternalUserId still matches" in:
    val record: IndividualProvidedDetails = tdAll.providedDetails.afterFinished
    repo.upsert(record).futureValue
    repo.findByInternalUserId(record.getInternalUserId).futureValue shouldBe List(record)

  "with FLE disabled the (internalUserId, agentApplicationId) compound find still matches" in:
    val record: IndividualProvidedDetails = tdAll.providedDetails.afterFinished
    repo.upsert(record).futureValue
    repo.find(record.getInternalUserId, record.agentApplicationId).futureValue.value shouldBe record
