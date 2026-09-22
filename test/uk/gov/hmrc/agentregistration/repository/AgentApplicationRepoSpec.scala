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

import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.AgentApplicationId
import uk.gov.hmrc.agentregistration.shared.ApplicationReference
import uk.gov.hmrc.agentregistration.shared.ApplicationState.Expired
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

  "updateManyToExpiredForUnsubmitted should flip Started and GrsDataReceived applications whose applicationExpiresAt has passed and stamp gracePeriodEndsAt" in:
    val pastExpiry: Instant = tdAll.instant.minusSeconds(60)
    val futureExpiry: Instant = tdAll.instant.plusSeconds(3600)
    val expectedGracePeriodEndsAt: Instant = tdAll.instant.plus(Duration.ofDays(45))

    val expiredStarted: AgentApplication = tdAll.agentApplicationLlp.afterStarted.copy(
      _id = AgentApplicationId("expired-started-id"),
      applicationReference = ApplicationReference("EXPSTART1"),
      internalUserId = InternalUserId("expired-started-user"),
      linkId = LinkId("expired-started-link"),
      applicationExpiresAt = Some(pastExpiry)
    )
    val expiredGrs: AgentApplication = tdAll.agentApplicationLlp.afterGrsDataReceived.copy(
      _id = AgentApplicationId("expired-grs-id"),
      applicationReference = ApplicationReference("EXPGRS001"),
      internalUserId = InternalUserId("expired-grs-user"),
      linkId = LinkId("expired-grs-link"),
      applicationExpiresAt = Some(pastExpiry)
    )
    val notYetExpired: AgentApplication = tdAll.agentApplicationLlp.afterStarted.copy(
      _id = AgentApplicationId("not-yet-expired-id"),
      applicationReference = ApplicationReference("NOTEXP001"),
      internalUserId = InternalUserId("not-yet-expired-user"),
      linkId = LinkId("not-yet-expired-link"),
      applicationExpiresAt = Some(futureExpiry)
    )
    val alreadySubmitted: AgentApplication = tdAll.agentApplicationLlp.afterSentForRisking.copy(
      _id = AgentApplicationId("submitted-id"),
      applicationReference = ApplicationReference("SUBMITT01"),
      internalUserId = InternalUserId("submitted-user"),
      linkId = LinkId("submitted-link")
    )

    repo.upsert(expiredStarted).futureValue
    repo.upsert(expiredGrs).futureValue
    repo.upsert(notYetExpired).futureValue
    repo.upsert(alreadySubmitted).futureValue

    val modifiedCount: Long = repo.updateManyToExpiredForUnsubmitted(tdAll.instant, expectedGracePeriodEndsAt).futureValue
    modifiedCount shouldBe 2L withClue "only the two past-expiry pre-submission applications should be updated"

    val loadedExpiredStarted = repo.findById(expiredStarted.agentApplicationId).futureValue.value
    loadedExpiredStarted.applicationState shouldBe Expired withClue "Started + past expiry -> Expired"
    loadedExpiredStarted.gracePeriodEndsAt shouldBe Some(expectedGracePeriodEndsAt) withClue "gracePeriodEndsAt stamped on flipped Started record"

    val loadedExpiredGrs = repo.findById(expiredGrs.agentApplicationId).futureValue.value
    loadedExpiredGrs.applicationState shouldBe Expired withClue "GrsDataReceived + past expiry -> Expired"
    loadedExpiredGrs.gracePeriodEndsAt shouldBe Some(expectedGracePeriodEndsAt) withClue "gracePeriodEndsAt stamped on flipped GrsDataReceived record"

    val loadedNotYetExpired = repo.findById(notYetExpired.agentApplicationId).futureValue.value
    loadedNotYetExpired.applicationState shouldBe Started withClue "future expiry -> unchanged"
    loadedNotYetExpired.gracePeriodEndsAt shouldBe None withClue "gracePeriodEndsAt not stamped on untouched record"

    val loadedAlreadySubmitted = repo.findById(alreadySubmitted.agentApplicationId).futureValue.value
    loadedAlreadySubmitted.applicationState shouldBe SentForRisking withClue "post-submission state -> unchanged"
    loadedAlreadySubmitted.gracePeriodEndsAt shouldBe None withClue "gracePeriodEndsAt not stamped on untouched record"
