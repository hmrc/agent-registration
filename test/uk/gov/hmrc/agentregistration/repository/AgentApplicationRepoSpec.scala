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

import uk.gov.hmrc.agentregistration.repository.providedetails.llp.IndividualProvidedDetailsRepo
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.testsupport.ISpec
import uk.gov.hmrc.agentregistration.testsupport.testdata.TdAgentApplicationLlpInStates

class AgentApplicationRepoSpec
extends ISpec:

  override protected def configOverrides: Map[String, Any] = Map[String, Any](
    "field-level-encryption.enabled" -> false
  )

  private lazy val applicationRepo: AgentApplicationRepo = app.injector.instanceOf[AgentApplicationRepo]
  private lazy val individualRepo: IndividualProvidedDetailsRepo = app.injector.instanceOf[IndividualProvidedDetailsRepo]

  override def beforeEach(): Unit =
    super.beforeEach()
    TdAgentApplicationLlpInStates.all.foreach: scenario =>
      applicationRepo.upsert(scenario.agentApplicationLlp).futureValue
      scenario.individuals.foreach(individualRepo.upsert(_).futureValue)

  "findReadyForReadyToSubmitEmail returns only applications in GrsDataReceived with no decision yet AND all individuals Finished" in:
    val agentApplications: Seq[AgentApplication] = applicationRepo.findReadyForReadyToSubmitEmail().futureValue
    agentApplications.map(_.agentApplicationId).toSet shouldBe Set(
      TdAgentApplicationLlpInStates.readyForEmail.agentApplicationLlp.agentApplicationId
    )
