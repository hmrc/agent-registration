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

class IndividualProvidedDetailsFleSpec
extends ISpec:

  override protected def configOverrides: Map[String, Any] = Map[String, Any](
    "field-level-encryption.enabled" -> true
  )

  private lazy val repo: IndividualProvidedDetailsRepo = app.injector.instanceOf[IndividualProvidedDetailsRepo]

  private def rawDocumentFor(record: IndividualProvidedDetails): Document =
    mongoComponent.database
      .getCollection("individual")
      .find(Filters.eq("_id", record._id.value))
      .first()
      .toFuture()
      .futureValue

  "with FLE enabled the PII fields are encrypted at rest" in:
    val record: IndividualProvidedDetails = tdAll.providedDetails.afterFinished
    repo.upsert(record).futureValue

    val rawJson: String = rawDocumentFor(record).toJson()

    rawJson should not include record.getInternalUserId.value withClue "internalUserId is encrypted"
    rawJson should not include record.individualName.value withClue "individualName is encrypted"
    rawJson should not include record.getTelephoneNumber.value withClue "telephoneNumber is encrypted"
    rawJson should not include record.getEmailAddress.emailAddress.value withClue "emailAddress is encrypted"
    rawJson should not include s"""\"${record.vrns.value.headOption.value.value}\"""" withClue "vrn is encrypted"
    rawJson should not include s"""\"${record.payeRefs.value.headOption.value.value}\"""" withClue "payeRef is encrypted"

    rawJson should include(record.agentApplicationId.value) withClue "agentApplicationId stays plaintext"
    rawJson should include(record.personReference.value) withClue "personReference stays plaintext"

  "with FLE enabled the round-trip via the repo returns plaintext" in:
    val record: IndividualProvidedDetails = tdAll.providedDetails.afterFinished
    repo.upsert(record).futureValue
    repo.findById(record._id).futureValue.value shouldBe record

  "with FLE enabled findByInternalUserId still matches (deterministic encryption)" in:
    val record: IndividualProvidedDetails = tdAll.providedDetails.afterFinished
    repo.upsert(record).futureValue
    repo.findByInternalUserId(record.getInternalUserId).futureValue shouldBe List(record)

  "with FLE enabled the (internalUserId, agentApplicationId) compound find still matches" in:
    val record: IndividualProvidedDetails = tdAll.providedDetails.afterFinished
    repo.upsert(record).futureValue
    repo
      .find(record.getInternalUserId, record.agentApplicationId)
      .futureValue
      .value shouldBe record
