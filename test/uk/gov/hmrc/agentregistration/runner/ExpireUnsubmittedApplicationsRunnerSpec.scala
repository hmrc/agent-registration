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

package uk.gov.hmrc.agentregistration.runner

import uk.gov.hmrc.agentregistration.repository.AgentApplicationRepo
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.testsupport.ISpec

import java.time.temporal.ChronoUnit

class ExpireUnsubmittedApplicationsRunnerSpec
extends ISpec:

  private lazy val runner: ExpireUnsubmittedApplicationsRunner = app.injector.instanceOf[ExpireUnsubmittedApplicationsRunner]
  private lazy val agentApplicationRepo: AgentApplicationRepo = app.injector.instanceOf[AgentApplicationRepo]

  "run marks an unsubmitted application whose applicationExpiresAt has passed as Expired, recording expiredAt and postExpiryGracePeriodEndsAt from the same instant" in:
    val agentApplication: AgentApplication = tdAll.agentApplicationLlp.afterStartedExpired
    val expiredAgentApplication: AgentApplication = tdAll.agentApplicationLlp.afterExpired
    agentApplication.applicationExpiresAt.value.isBefore(tdAll.nowAsInstant) shouldBe true withClue
      "precondition: applicationExpiresAt must be in the past"
    agentApplicationRepo.upsert(agentApplication).futureValue

    runner.run().futureValue

    agentApplicationRepo.findById(agentApplication.agentApplicationId).futureValue.value shouldBe expiredAgentApplication

  "run records a grace period of exactly post-expiry-grace-period days for an application created long before it expired, so the period never depends on createdAt" in:
    val agentApplication: AgentApplication = tdAll.agentApplicationLlp.afterStartedExpiredLongAfterCreation
    val expiredAgentApplication: AgentApplication = tdAll.agentApplicationLlp.afterExpiredLongAfterCreation
    ChronoUnit.DAYS.between(agentApplication.createdAt, tdAll.nowAsInstant) should be > 73L withClue
      "precondition: the application must have been created more than days-to-submit-application before the run"
    agentApplicationRepo.upsert(agentApplication).futureValue

    runner.run().futureValue

    val storedAgentApplication: AgentApplication = agentApplicationRepo.findById(agentApplication.agentApplicationId).futureValue.value
    storedAgentApplication shouldBe expiredAgentApplication
    ChronoUnit.DAYS.between(storedAgentApplication.expiredAt.value, storedAgentApplication.postExpiryGracePeriodEndsAt.value) shouldBe 45L

  "run leaves an unsubmitted application whose applicationExpiresAt is still in the future unchanged" in:
    val agentApplication: AgentApplication = tdAll.agentApplicationLlp.afterStarted
    agentApplication.applicationExpiresAt.value.isAfter(tdAll.nowAsInstant) shouldBe true withClue
      "precondition: applicationExpiresAt must be in the future"
    agentApplicationRepo.upsert(agentApplication).futureValue

    runner.run().futureValue

    agentApplicationRepo.findById(agentApplication.agentApplicationId).futureValue.value shouldBe agentApplication
