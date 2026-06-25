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

    repo.updateManyApplicationStateByReference(List(record.applicationReference, record2.applicationReference), SentToMinerva).futureValue

    val updatedRecord = repo.findByApplicationReference(record.applicationReference).futureValue.value
    updatedRecord.applicationState shouldBe SentToMinerva withClue "application state for record 1 should be updated"
    val updatedRecord2 = repo.findByApplicationReference(record2.applicationReference).futureValue.value
    updatedRecord2.applicationState shouldBe SentToMinerva withClue "application state for record 2 should be updated"
    val updatedRecord3 = repo.findByApplicationReference(record3.applicationReference).futureValue.value
    updatedRecord3 shouldBe record3 withClue "application state for record 3 should be untouched"
