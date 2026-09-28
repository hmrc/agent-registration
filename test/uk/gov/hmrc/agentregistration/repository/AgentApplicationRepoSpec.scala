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
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.AgentApplicationId
import uk.gov.hmrc.agentregistration.shared.ApplicationReference
import uk.gov.hmrc.agentregistration.shared.ApplicationState
import uk.gov.hmrc.agentregistration.shared.ApplicationState.Expired
import uk.gov.hmrc.agentregistration.shared.ApplicationState.GrsDataReceived
import uk.gov.hmrc.agentregistration.shared.ApplicationState.RiskingCompleted
import uk.gov.hmrc.agentregistration.shared.ApplicationState.SentForRisking
import uk.gov.hmrc.agentregistration.shared.ApplicationState.SentToMinerva
import uk.gov.hmrc.agentregistration.shared.ApplicationState.Started
import uk.gov.hmrc.agentregistration.shared.InternalUserId
import uk.gov.hmrc.agentregistration.shared.LinkId
import uk.gov.hmrc.agentregistration.testsupport.ISpec

import java.time.Duration
import java.time.Instant

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

  "applicationExpiresAt should round-trip through the repository for pre-submission applications" in:
    val started: AgentApplication = tdAll.agentApplicationLlp.afterStarted
    started.applicationExpiresAt shouldBe defined withClue "sanity: pre-submission applications must carry applicationExpiresAt"

    repo.upsert(started).futureValue

    val loaded: AgentApplication = repo.findById(started.agentApplicationId).futureValue.value
    loaded.applicationExpiresAt shouldBe started.applicationExpiresAt withClue "the field must survive the round-trip through Mongo"

  private val gracePeriodEndsAt: Instant = tdAll.instant.plus(Duration.ofDays(45))

  private def applicationInState(state: ApplicationState): AgentApplication =
    state match
      case Started => tdAll.agentApplicationLlp.afterStartedExpired
      case GrsDataReceived => tdAll.agentApplicationLlp.afterGrsDataReceivedExpired
      case SentForRisking => tdAll.agentApplicationLlp.afterSentForRisking
      case SentToMinerva => tdAll.agentApplicationLlp.afterSentToMinerva
      case RiskingCompleted => tdAll.agentApplicationLlp.afterRiskingCompletedApproved
      case Expired => tdAll.agentApplicationLlp.afterExpired

  "updateManyToExpiredForUnsubmitted has an explicitly defined outcome for every ApplicationState — a new state MUST update both pattern matches below" in:
    ApplicationState.values.foreach: state =>
      dropDatabase()
      val record: AgentApplication = applicationInState(state)
      repo.upsert(record).futureValue
      val modifiedCount: Long = repo.updateManyToExpiredForUnsubmitted(tdAll.instant, gracePeriodEndsAt).futureValue

      state match
        case Started | GrsDataReceived =>
          modifiedCount shouldBe 1L withClue s"state=$state should have been moved to Expired"
          val loaded: AgentApplication = repo.findById(record.agentApplicationId).futureValue.value
          loaded.applicationState shouldBe Expired
          loaded.gracePeriodEndsAt shouldBe Some(gracePeriodEndsAt)
          loaded.applicationExpiresAt shouldBe None
        case SentForRisking | SentToMinerva | RiskingCompleted | Expired =>
          modifiedCount shouldBe 0L withClue s"state=$state must NOT be moved to Expired"
          repo.findById(record.agentApplicationId).futureValue.value shouldBe record withClue
            s"state=$state record must be untouched"

  "updateManyToExpiredForUnsubmitted leaves a Started application whose applicationExpiresAt is still in the future unchanged" in:
    val record: AgentApplication = tdAll.agentApplicationLlp.afterStarted
    record.applicationExpiresAt.value.isAfter(tdAll.instant) shouldBe true withClue
      "sanity: afterStarted must have applicationExpiresAt in the future relative to the test clock for this test to be meaningful"
    repo.upsert(record).futureValue

    repo.updateManyToExpiredForUnsubmitted(tdAll.instant, gracePeriodEndsAt).futureValue shouldBe 0L

    repo.findById(record.agentApplicationId).futureValue.value shouldBe record

  "updateManyToExpiredForUnsubmitted picks up a legacy record whose applicationExpiresAt is still stored as a BSON String (pre-migration shape)" in:
    val record: AgentApplication = tdAll.agentApplicationLlp.afterStartedExpired
    repo.upsert(record).futureValue

    val legacyApplicationExpiresAt: String = tdAll.instant.minusSeconds(60).toString
    repo
      .collection
      .updateOne(
        filter = Filters.eq("_id", record.agentApplicationId.value),
        update = Updates.set("applicationExpiresAt", legacyApplicationExpiresAt)
      )
      .toFuture()
      .futureValue

    repo.updateManyToExpiredForUnsubmitted(tdAll.instant, gracePeriodEndsAt).futureValue shouldBe 1L

    val loaded: AgentApplication = repo.findById(record.agentApplicationId).futureValue.value
    loaded.applicationState shouldBe Expired
    loaded.gracePeriodEndsAt shouldBe Some(gracePeriodEndsAt)
    loaded.applicationExpiresAt shouldBe None

  "findById reconstructs applicationExpiresAt correctly when it is stored as a legacy BSON String (pre-migration shape)" in:
    val record: AgentApplication = tdAll.agentApplicationLlp.afterStartedExpired
    repo.upsert(record).futureValue

    val legacyApplicationExpiresAt: String = record.applicationExpiresAt.value.toString
    repo
      .collection
      .updateOne(
        filter = Filters.eq("_id", record.agentApplicationId.value),
        update = Updates.set("applicationExpiresAt", legacyApplicationExpiresAt)
      )
      .toFuture()
      .futureValue

    val loaded: AgentApplication = repo.findById(record.agentApplicationId).futureValue.value
    loaded.applicationExpiresAt shouldBe record.applicationExpiresAt withClue
      "dual-read must reconstruct a legacy ISO string field back into the same Instant"

  "upsert stores applicationExpiresAt as a BSON Date, not as a string" in:
    val record: AgentApplication = tdAll.agentApplicationLlp.afterStartedExpired
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
      "applicationExpiresAt must persist as a BSON Date so scheduler comparisons stay type-correct"
