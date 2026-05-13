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

import org.mongodb.scala.SingleObservableFuture
import org.mongodb.scala.bson.collection.immutable.Document
import org.mongodb.scala.model.Filters
import uk.gov.hmrc.agentregistration.shared.AgentApplicationLlp
import uk.gov.hmrc.agentregistration.testsupport.ISpec

/** Regression for the local-dev path where field-level encryption is turned off.
  *
  * In deployed environments the flag is always on; locally it is off so engineers can read Mongo documents as plaintext. The whole encrypt/decrypt pipeline must
  * therefore be a no-op when the flag is false.
  */
class AgentApplicationFleDisabledSpec
extends ISpec:

  override protected def configOverrides: Map[String, Any] = Map[String, Any](
    "field-level-encryption.enabled" -> false
  )

  private lazy val repo: AgentApplicationRepo = app.injector.instanceOf[AgentApplicationRepo]

  private def rawDocumentFor(record: AgentApplicationLlp): Document =
    mongoComponent.database
      .getCollection("agent-application")
      .find(Filters.eq("_id", record._id.value))
      .first()
      .toFuture()
      .futureValue

  "with FLE disabled the PII fields are stored as plaintext on disk" in:
    val record: AgentApplicationLlp = tdAll.agentApplicationLlp.afterAgentDetailsComplete
    repo.upsert(record).futureValue

    val rawJson: String = rawDocumentFor(record).toJson()

    rawJson should include(record.internalUserId.value) withClue "internalUserId is plaintext"
    rawJson should include(record.groupId.value) withClue "groupId is plaintext"
    rawJson should include(record.applicantCredentials.providerId) withClue "providerId is plaintext"
    rawJson should include(record.getBusinessDetails.saUtr.value) withClue "saUtr is plaintext"
    rawJson should include(record.getBusinessDetails.companyProfile.companyName) withClue "companyName is plaintext"
    rawJson should include(record.getApplicantContactDetails.applicantName.value) withClue "applicantName is plaintext"
    rawJson should include(record.getAgentDetails.businessName.agentBusinessName) withClue "agentBusinessName is plaintext"

  "with FLE disabled the round-trip via the repo returns the model unchanged" in:
    val record: AgentApplicationLlp = tdAll.agentApplicationLlp.afterAgentDetailsComplete
    repo.upsert(record).futureValue
    repo.findById(record._id).futureValue.value shouldBe record

  "with FLE disabled findByInternalUserId still matches" in:
    val record: AgentApplicationLlp = tdAll.agentApplicationLlp.afterAgentDetailsComplete
    repo.upsert(record).futureValue
    repo.findByInternalUserId(record.internalUserId).futureValue.value shouldBe record

  "with FLE disabled findByLinkId still matches" in:
    val record: AgentApplicationLlp = tdAll.agentApplicationLlp.afterAgentDetailsComplete
    repo.upsert(record).futureValue
    repo.findByLinkId(record.linkId).futureValue.value shouldBe record

  "with FLE disabled findByApplicationReference still matches" in:
    val record: AgentApplicationLlp = tdAll.agentApplicationLlp.afterAgentDetailsComplete
    repo.upsert(record).futureValue
    repo.findByApplicationReference(record.applicationReference).futureValue.value shouldBe record
