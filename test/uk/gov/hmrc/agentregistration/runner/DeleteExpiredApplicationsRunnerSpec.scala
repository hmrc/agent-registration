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
import uk.gov.hmrc.agentregistration.repository.providedetails.llp.IndividualProvidedDetailsRepo
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetails
import uk.gov.hmrc.agentregistration.testsupport.ISpec

class DeleteExpiredApplicationsRunnerSpec
extends ISpec:

  private lazy val runner: DeleteExpiredApplicationsRunner = app.injector.instanceOf[DeleteExpiredApplicationsRunner]
  private lazy val agentApplicationRepo: AgentApplicationRepo = app.injector.instanceOf[AgentApplicationRepo]
  private lazy val individualProvidedDetailsRepo: IndividualProvidedDetailsRepo = app.injector.instanceOf[IndividualProvidedDetailsRepo]

  "run deletes an Expired application whose postExpiryGracePeriodEndsAt has passed, together with its individuals" in:
    val agentApplication: AgentApplication = tdAll.agentApplicationLlp.afterExpiredPostExpiryGracePeriodEnded
    val individualProvidedDetails: IndividualProvidedDetails = tdAll.providedDetails.afterStarted
    agentApplication.postExpiryGracePeriodEndsAt.value.isBefore(tdAll.nowAsInstant) shouldBe true withClue
      "precondition: postExpiryGracePeriodEndsAt must be in the past"
    individualProvidedDetails.agentApplicationId shouldBe agentApplication.agentApplicationId withClue
      "precondition: the individual must belong to the application"
    agentApplicationRepo.upsert(agentApplication).futureValue
    individualProvidedDetailsRepo.upsert(individualProvidedDetails).futureValue

    runner.run().futureValue

    agentApplicationRepo.findById(agentApplication.agentApplicationId).futureValue shouldBe None
    individualProvidedDetailsRepo.findForApplication(agentApplication.agentApplicationId).futureValue shouldBe empty

  "run keeps an Expired application whose postExpiryGracePeriodEndsAt is still in the future, together with its individuals" in:
    val agentApplication: AgentApplication = tdAll.agentApplicationLlp.afterExpired
    val individualProvidedDetails: IndividualProvidedDetails = tdAll.providedDetails.afterStarted
    agentApplication.postExpiryGracePeriodEndsAt.value.isAfter(tdAll.nowAsInstant) shouldBe true withClue
      "precondition: postExpiryGracePeriodEndsAt must be in the future"
    individualProvidedDetails.agentApplicationId shouldBe agentApplication.agentApplicationId withClue
      "precondition: the individual must belong to the application"
    agentApplicationRepo.upsert(agentApplication).futureValue
    individualProvidedDetailsRepo.upsert(individualProvidedDetails).futureValue

    runner.run().futureValue

    agentApplicationRepo.findById(agentApplication.agentApplicationId).futureValue shouldBe Some(agentApplication)
    individualProvidedDetailsRepo.findForApplication(agentApplication.agentApplicationId).futureValue shouldBe List(individualProvidedDetails)
