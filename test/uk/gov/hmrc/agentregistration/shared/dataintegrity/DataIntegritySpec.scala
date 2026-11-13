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

package uk.gov.hmrc.agentregistration.shared.dataintegrity

import java.time.Duration
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.AgentApplicationId
import uk.gov.hmrc.agentregistration.shared.AgentApplicationLlp
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetails
import uk.gov.hmrc.agentregistration.shared.testdata.agentapplication.ExpiryTransformations.*
import uk.gov.hmrc.agentregistration.testsupport.UnitSpec
import uk.gov.hmrc.agentregistration.testsupport.testdata.TdAll.tdAll

class DataIntegritySpec
extends UnitSpec:

  "accepts the test data, which is what makes the rules worth having" in:
    DataIntegrity.violations(tdAll.agentApplicationLlp.afterResubmitted) shouldBe Seq.empty
    DataIntegrity.violations(tdAll.agentApplicationLlp.afterStarted.makeOverdue.makeExpired.makeDueForDeletion) shouldBe Seq.empty

  "rejects an application that expires before it was created" in:
    val impossible: AgentApplication = tdAll
      .agentApplicationLlp
      .afterStarted
      .copy(applicationExpiresAt = Some(tdAll.createdAt.minus(Duration.ofDays(1))))
    DataIntegrity.violations(impossible) should have size 1
    DataIntegrity.violations(impossible).mkString should include("an application expires after it is created")

  "rejects an application submitted before it was created" in:
    val impossible: AgentApplication = tdAll
      .agentApplicationLlp
      .afterDeclarationSubmitted
      .copy(submittedAt = Some(tdAll.createdAt.minusSeconds(1)))
    DataIntegrity.violations(impossible).mkString should include("an application is submitted after it is created")

  "rejects a grace period that ends before the application expired" in:
    val impossible: AgentApplication = tdAll
      .agentApplicationLlp
      .afterStarted
      .makeOverdue
      .makeExpired
      .copy(postExpiryGracePeriodEndsAt = Some(tdAll.expiredAt.minusSeconds(1)))
    DataIntegrity.violations(impossible).mkString should include("the grace period ends after the application expired")

  "accepts an individual created after its application" in:
    DataIntegrity.violations(individual, application) shouldBe Seq.empty

  "rejects an individual created before its application" in:
    val impossible: IndividualProvidedDetails = individual.copy(createdAt = application.createdAt.minusSeconds(1))
    DataIntegrity.violations(impossible, application).mkString should include("should be after the application was created")

  "rejects an individual that belongs to a different application" in:
    val other: AgentApplicationLlp = application.copy(_id = AgentApplicationId("another-application"))
    DataIntegrity.violations(individual, other).mkString should include("belongs to")

  private def application: AgentApplicationLlp = tdAll.agentApplicationLlp.afterDeclarationSubmitted

  private def individual: IndividualProvidedDetails = tdAll.providedDetails.afterFinished
