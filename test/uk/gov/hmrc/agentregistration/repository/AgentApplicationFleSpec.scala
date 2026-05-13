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

class AgentApplicationFleSpec
extends ISpec:

  override protected def configOverrides: Map[String, Any] = Map[String, Any](
    "field-level-encryption.enabled" -> true
  )

  private lazy val repo: AgentApplicationRepo = app.injector.instanceOf[AgentApplicationRepo]

  private def rawDocumentFor(record: AgentApplicationLlp): Document =
    mongoComponent.database
      .getCollection("agent-application")
      .find(Filters.eq("_id", record._id.value))
      .first()
      .toFuture()
      .futureValue

  "with FLE enabled the PII fields are encrypted at rest" in:
    val record: AgentApplicationLlp = tdAll.agentApplicationLlp.afterAgentDetailsComplete
    repo.upsert(record).futureValue

    val rawJson: String = rawDocumentFor(record).toJson()

    rawJson should not include record.internalUserId.value withClue "internalUserId is encrypted"
    rawJson should not include record.groupId.value withClue "groupId is encrypted"
    rawJson should not include record.applicantCredentials.providerId withClue "applicantCredentials.providerId is encrypted"
    rawJson should not include record.getBusinessDetails.saUtr.value withClue "saUtr is encrypted"
    rawJson should not include record.getBusinessDetails.companyProfile.companyNumber.value withClue "companyNumber is encrypted"
    rawJson should not include s"""\"${record.getBusinessDetails.companyProfile.companyName}\"""" withClue "companyName is encrypted"
    rawJson should not include record.getApplicantContactDetails.applicantName.value withClue "applicantName is encrypted"
    rawJson should not include record.getApplicantContactDetails.getTelephoneNumber.value withClue "applicant telephoneNumber is encrypted"
    rawJson should not include record.getApplicantContactDetails.getApplicantEmailAddress.emailAddress.value withClue "applicant email is encrypted"
    rawJson should not include s"""\"${record.getAgentDetails.businessName.agentBusinessName}\"""" withClue "agentBusinessName is encrypted"
    rawJson should not include record.getAgentDetails.getTelephoneNumber.agentTelephoneNumber withClue "agentTelephoneNumber is encrypted"
    rawJson should not include record.getAgentDetails.getAgentEmailAddress.emailAddress.agentEmailAddress withClue "agentEmailAddress is encrypted"
    rawJson should not include s"""\"${record.getAgentDetails.getAgentCorrespondenceAddress.addressLine1}\"""" withClue "agent addressLine1 is encrypted"
    record.vrns.value.foreach { vrn =>
      rawJson should not include s"""\"${vrn.value}\"""" withClue "vrn is encrypted"
    }
    record.payeRefs.value.foreach { payeRef =>
      rawJson should not include s"""\"${payeRef.value}\"""" withClue "payeRef is encrypted"
    }

    rawJson should include(record.applicationReference.value) withClue "applicationReference stays plaintext"
    rawJson should include(record.linkId.value) withClue "linkId stays plaintext"
    rawJson should include(record.applicantCredentials.providerType) withClue "providerType stays plaintext"

  "with FLE enabled the round-trip via the repo returns plaintext" in:
    val record: AgentApplicationLlp = tdAll.agentApplicationLlp.afterAgentDetailsComplete
    repo.upsert(record).futureValue
    repo.findById(record._id).futureValue.value shouldBe record

  "with FLE enabled findByInternalUserId still matches (deterministic encryption)" in:
    val record: AgentApplicationLlp = tdAll.agentApplicationLlp.afterAgentDetailsComplete
    repo.upsert(record).futureValue
    repo.findByInternalUserId(record.internalUserId).futureValue.value shouldBe record

  "with FLE enabled findByLinkId still matches (linkId stays plaintext)" in:
    val record: AgentApplicationLlp = tdAll.agentApplicationLlp.afterAgentDetailsComplete
    repo.upsert(record).futureValue
    repo.findByLinkId(record.linkId).futureValue.value shouldBe record

  "with FLE enabled findByApplicationReference still matches" in:
    val record: AgentApplicationLlp = tdAll.agentApplicationLlp.afterAgentDetailsComplete
    repo.upsert(record).futureValue
    repo.findByApplicationReference(record.applicationReference).futureValue.value shouldBe record
