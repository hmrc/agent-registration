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

package uk.gov.hmrc.agentregistration.shared.testdata.agentapplication

import java.time.Duration
import java.time.Instant
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.AgentApplicationLlp
import uk.gov.hmrc.agentregistration.shared.AgentApplicationSoleTrader
import uk.gov.hmrc.agentregistration.shared.ApplicationState
import uk.gov.hmrc.agentregistration.shared.testdata.agentapplication.ExpiryTransformations.*
import uk.gov.hmrc.agentregistration.testsupport.UnitSpec
import uk.gov.hmrc.agentregistration.testsupport.testdata.TdAll

class ExpiryTransformationsSpec
extends UnitSpec:

  "makeOverdue puts the deadline in the past and leaves the state alone, because nothing has acted on it yet" in:
    val overdue: AgentApplication = afterStarted.makeOverdue
    overdue.applicationExpiresAt.value should be < now
    overdue.applicationState shouldBe ApplicationState.Started
    overdue.expiredAt shouldBe None
    overdue.postExpiryGracePeriodEndsAt shouldBe None

  "makeOverdue moves createdAt too, so the deadline is still the creation plus the submission window" in:
    val overdue: AgentApplication = afterStarted.makeOverdue
    overdue.applicationExpiresAt.value shouldBe overdue.createdAt.plus(daysToSubmitApplication)

  "makeOverdue works from any state that still has a deadline, which is what the test is about" in:
    afterGrsDataReceived.makeOverdue.applicationState shouldBe ApplicationState.GrsDataReceived

  "makeLongOverdue is overdue by longer than the grace period, which is the whole point of it" in:
    val deadline: Instant = afterStarted.makeLongOverdue.applicationExpiresAt.value
    Duration.between(deadline, now) should be > postExpiryGracePeriod

  "makeOverdue is overdue by less than the grace period, so the two cases really differ" in:
    val deadline: Instant = afterStarted.makeOverdue.applicationExpiresAt.value
    Duration.between(deadline, now) should be < postExpiryGracePeriod

  "makeExpired clears the deadline, stamps the sweep and starts the grace period from it" in:
    val expired: AgentApplication = afterStarted.makeOverdue.makeExpired
    expired.applicationState shouldBe ApplicationState.Expired
    expired.applicationExpiresAt shouldBe None
    expired.expiredAt.value shouldBe now
    expired.postExpiryGracePeriodEndsAt.value shouldBe now.plus(postExpiryGracePeriod)

  "a long overdue application still gets the whole grace period, because it runs from the sweep and not from the deadline" in:
    val expired: AgentApplication = afterStarted.makeLongOverdue.makeExpired
    expired.postExpiryGracePeriodEndsAt.value shouldBe now.plus(postExpiryGracePeriod)
    expired.postExpiryGracePeriodEndsAt.value should be > now

  "makeDueForDeletion puts the end of the grace period in the past, by moving the sweep rather than the end" in:
    val due: AgentApplication = afterStarted.makeOverdue.makeExpired.makeDueForDeletion
    due.postExpiryGracePeriodEndsAt.value should be < now
    due.expiredAt.value shouldBe due.postExpiryGracePeriodEndsAt.value.minus(postExpiryGracePeriod)
    due.applicationState shouldBe ApplicationState.Expired

  "makeDueForDeletion leaves the application a full submission window older than its sweep" in:
    val due: AgentApplication = afterStarted.makeOverdue.makeExpired.makeDueForDeletion
    due.createdAt shouldBe due.expiredAt.value.minus(daysToSubmitApplication)

  "refuses to expire an application that is not overdue, rather than building one that could not exist" in:
    val thrown = intercept[IllegalArgumentException](afterStarted.makeExpired)
    thrown.getMessage should include("only an overdue application can be expired")

  "refuses to make an application overdue once it has no deadline left" in:
    val expired: AgentApplication = afterStarted.makeOverdue.makeExpired
    val thrown = intercept[IllegalArgumentException](expired.makeOverdue)
    thrown.getMessage should include("still has a deadline")

  "refuses to make an application due for deletion before it has expired" in:
    val thrown = intercept[IllegalArgumentException](afterStarted.makeOverdue.makeDueForDeletion)
    thrown.getMessage should include("only an expired application can be due for deletion")

  "gives back the type it was given, so a transformed Llp is still an Llp" in:
    // the ascriptions are the test: were the type widened to AgentApplication, this would not compile
    val overdue: AgentApplicationLlp = tdAll.agentApplicationLlp.afterStarted.makeOverdue
    val expired: AgentApplicationLlp = overdue.makeExpired
    val due: AgentApplicationLlp = expired.makeDueForDeletion
    val soleTrader: AgentApplicationSoleTrader = tdAll.agentApplicationSoleTrader.afterStarted.makeLongOverdue
    Seq[AgentApplication](
      overdue,
      expired,
      due,
      soleTrader
    ).map(_.applicationState) shouldBe Seq(
      ApplicationState.Started,
      ApplicationState.Expired,
      ApplicationState.Expired,
      ApplicationState.Started
    )

  "works on every business type, so a test is not limited to the one the fixtures happened to cover" in:
    val expired: Seq[ApplicationState] = Seq(
      tdAll.agentApplicationLlp.afterStarted,
      tdAll.agentApplicationSoleTrader.afterStarted,
      tdAll.agentApplicationLimitedCompany.afterStarted,
      tdAll.agentApplicationGeneralPartnership.afterStarted,
      tdAll.agentApplicationLimitedPartnership.afterStarted,
      tdAll.agentApplicationScottishLimitedPartnership.afterStarted,
      tdAll.agentApplicationScottishPartnership.afterStarted
    ).map(_.makeOverdue.makeExpired.applicationState)
    expired.distinct shouldBe Seq(ApplicationState.Expired)

  private val tdAll: TdAll = TdAll.tdAll

  private def now: Instant = tdAll.now

  private def daysToSubmitApplication: Duration = tdAll.daysToSubmitApplication

  private def postExpiryGracePeriod: Duration = tdAll.postExpiryGracePeriod

  private def afterStarted: AgentApplication = tdAll.agentApplicationLlp.afterStarted

  private def afterGrsDataReceived: AgentApplication = tdAll.agentApplicationLlp.afterGrsDataReceived
