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
import org.mongodb.scala.SingleObservableFuture
import org.mongodb.scala.model.Filters
import org.mongodb.scala.model.Updates
import uk.gov.hmrc.agentregistration.shared.ApplicationState.SentForRisking
import uk.gov.hmrc.agentregistration.shared.ApplicationState.SentToMinerva
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.AgentApplicationId
import uk.gov.hmrc.agentregistration.shared.ApplicationReference
import uk.gov.hmrc.agentregistration.shared.InternalUserId
import uk.gov.hmrc.agentregistration.shared.LinkId
import uk.gov.hmrc.agentregistration.testsupport.ISpec

class AgentApplicationRepoSpec
extends ISpec:

  private lazy val repo: AgentApplicationRepo = app.injector.instanceOf[AgentApplicationRepo]

  "updateManyApplicationStateByReference should set the state in all selected applications and leave others untouched" in:
    val record: AgentApplication = tdAll.agentApplicationLlp.afterSentForRisking
    val record2: AgentApplication = tdAll.agentApplicationLlp.afterSentForRisking.copy(
      _id = AgentApplicationId("agent-application-id-23456"),
      applicationReference = ApplicationReference("APPREF234"),
      internalUserId = InternalUserId("internal-user-id-23456"),
      linkId = LinkId("link-id-23456")
    )
    val record3: AgentApplication = tdAll.agentApplicationLlp.afterSentForRisking.copy(
      _id = AgentApplicationId("agent-application-id-34567"),
      applicationReference = ApplicationReference("APPREF345"),
      internalUserId = InternalUserId("internal-user-id-34567"),
      linkId = LinkId("link-id-34567")
    )
    repo.upsert(record).futureValue
    repo.upsert(record2).futureValue
    repo.upsert(record3).futureValue

    repo.findById(record.agentApplicationId).futureValue.value.applicationState shouldBe SentForRisking withClue "sanity check"

    repo.updateManyApplicationStateByReference(Seq(record.applicationReference, record2.applicationReference), SentToMinerva).futureValue

    val updatedRecord = repo.findByApplicationReference(record.applicationReference).futureValue.value
    updatedRecord.applicationState shouldBe SentToMinerva withClue "application state for record 1 should be updated"
    val updatedRecord2 = repo.findByApplicationReference(record2.applicationReference).futureValue.value
    updatedRecord2.applicationState shouldBe SentToMinerva withClue "application state for record 2 should be updated"
    val updatedRecord3 = repo.findByApplicationReference(record3.applicationReference).futureValue.value
    updatedRecord3 shouldBe record3 withClue "application state for record 3 should be untouched"

  "applicationExpiresAt round-trips through the repository for pre-submission applications" in:
    val record: AgentApplication = tdAll.agentApplicationLlp.afterStarted
    record.applicationExpiresAt shouldBe defined withClue "sanity: pre-submission applications must carry applicationExpiresAt"
    repo.upsert(record).futureValue

    repo.findById(record.agentApplicationId).futureValue.value shouldBe record

  "upsert stores applicationExpiresAt as a BSON Date, not as a string" in:
    val record: AgentApplication = tdAll.agentApplicationLlp.afterStarted
    repo.upsert(record).futureValue

    val rawDocument: BsonDocument =
      repo
        .collection
        .withDocumentClass[BsonDocument]()
        .find(Filters.eq("_id", record.agentApplicationId.value))
        .headOption()
        .futureValue
        .value

    rawDocument.get("applicationExpiresAt").getBsonType shouldBe BsonType.DATE_TIME withClue
      "applicationExpiresAt must persist as a BSON Date so it can be compared as a date, not a string"

  "findById reconstructs applicationExpiresAt correctly when it is still stored as a legacy ISO string (pre-migration shape)" in:
    val record: AgentApplication = tdAll.agentApplicationLlp.afterStarted
    repo.upsert(record).futureValue
    repo
      .collection
      .updateOne(
        filter = Filters.eq("_id", record.agentApplicationId.value),
        update = Updates.set("applicationExpiresAt", record.applicationExpiresAt.value.toString)
      )
      .toFuture()
      .futureValue

    repo.findById(record.agentApplicationId).futureValue.value shouldBe record withClue
      "dual-read must reconstruct a legacy ISO string field back into the same Instant"
