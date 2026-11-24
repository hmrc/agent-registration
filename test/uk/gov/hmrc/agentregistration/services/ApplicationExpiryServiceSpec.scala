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

package uk.gov.hmrc.agentregistration.services

import play.api.mvc.RequestHeader
import uk.gov.hmrc.agentregistration.repository.AgentApplicationRepo
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.BusinessType
import uk.gov.hmrc.agentregistration.shared.testdata.agentapplication.ExpiryTransformations.*
import uk.gov.hmrc.agentregistration.testsupport.ISpec
import uk.gov.hmrc.agentregistration.testsupport.testdata.AgentApplicationPreconditions.*
import uk.gov.hmrc.agentregistration.testsupport.testdata.AgentApplicationStates

import java.time.Duration

class ApplicationExpiryServiceSpec
extends ISpec:

  private lazy val applicationExpiryService: ApplicationExpiryService = app.injector.instanceOf[ApplicationExpiryService]
  private lazy val agentApplicationRepo: AgentApplicationRepo = app.injector.instanceOf[AgentApplicationRepo]

  private given RequestHeader = tdAll.fakeBackendRequest

  BusinessType.values.foreach: businessType =>
    lazy val agentApplicationStates: AgentApplicationStates = AgentApplicationStates.forBusinessType(businessType)

    s"processExpiry expires an application whose expiry date has passed, starting the grace period now ($businessType)" in:
      agentApplicationStates.havingExpiryDate.foreach: havingExpiryDate =>
        dropDatabase()
        val overdue: AgentApplication = havingExpiryDate.makeOverdue
        overdue.shouldHaveExpiryDateInThePast
        agentApplicationRepo.upsert(overdue).futureValue

        applicationExpiryService.processExpiry().futureValue

        agentApplicationRepo.findById(overdue.agentApplicationId).futureValue.value shouldBe overdue.makeExpired withClue
          s"state=${havingExpiryDate.applicationState}"

    s"processExpiry gives a long overdue application the whole grace period of 45 days, counted from now and not from its expiry date ($businessType)" in:
      agentApplicationStates.havingExpiryDate.foreach: havingExpiryDate =>
        dropDatabase()
        val longOverdue: AgentApplication = havingExpiryDate.makeLongOverdue
        Duration.between(longOverdue.applicationExpiresAt.value, tdAll.now) should be > tdAll.postExpiryGracePeriod withClue
          "precondition: the expiry date must be further back than the grace period"
        agentApplicationRepo.upsert(longOverdue).futureValue

        applicationExpiryService.processExpiry().futureValue

        val expired: AgentApplication = agentApplicationRepo.findById(longOverdue.agentApplicationId).futureValue.value
        expired shouldBe longOverdue.makeExpired withClue s"state=${havingExpiryDate.applicationState}"
        Duration.between(tdAll.now, expired.postExpiryGracePeriodEndsAt.value) shouldBe Duration.ofDays(45)

    s"processExpiry does not expire an application whose expiry date is in the future ($businessType)" in:
      agentApplicationStates.havingExpiryDate.foreach: havingExpiryDate =>
        dropDatabase()
        havingExpiryDate.shouldHaveExpiryDateInTheFuture
        agentApplicationRepo.upsert(havingExpiryDate).futureValue

        applicationExpiryService.processExpiry().futureValue

        agentApplicationRepo.findById(havingExpiryDate.agentApplicationId).futureValue.value shouldBe havingExpiryDate withClue
          s"state=${havingExpiryDate.applicationState}"
