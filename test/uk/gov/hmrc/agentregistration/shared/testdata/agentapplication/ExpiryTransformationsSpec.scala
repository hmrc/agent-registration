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
import uk.gov.hmrc.agentregistration.shared.ApplicationState.*
import uk.gov.hmrc.agentregistration.shared.BusinessType
import uk.gov.hmrc.agentregistration.shared.testdata.agentapplication.ExpiryTransformations.*
import uk.gov.hmrc.agentregistration.testsupport.UnitSpec
import uk.gov.hmrc.agentregistration.testsupport.testdata.AgentApplicationStates
import uk.gov.hmrc.agentregistration.testsupport.testdata.TdAll

class ExpiryTransformationsSpec
extends UnitSpec:

  BusinessType.values.foreach: businessType =>
    s"for a $businessType application" - {

      lazy val agentApplicationStates: AgentApplicationStates = AgentApplicationStates.forBusinessType(businessType)

      "AgentApplicationStates gives an application of this business type in each state" in:
        agentApplicationStates.started.applicationState shouldBe Started
        agentApplicationStates.grsDataReceived.applicationState shouldBe GrsDataReceived
        agentApplicationStates.sentForRisking.applicationState shouldBe SentForRisking
        agentApplicationStates.sentToMinerva.applicationState shouldBe SentToMinerva
        agentApplicationStates.riskingCompleted.applicationState shouldBe RiskingCompleted
        agentApplicationStates.expired.applicationState shouldBe Expired
        agentApplicationStates.all.map(_.businessType).distinct shouldBe Seq(businessType)

      "makeDueForExpiry sets the expiry date the reminder period ahead, so the reminder job picks the application up" in:
        agentApplicationStates.havingExpiryDate.foreach: havingExpiryDate =>
          withClue(s"state=${havingExpiryDate.applicationState}: "):
            val dueForExpiry: AgentApplication = havingExpiryDate.makeDueForExpiry
            val expiresAt: Instant = now.plus(expiryReminderPeriod)
            dueForExpiry.applicationExpiresAt.value shouldBe expiresAt
            dueForExpiry.createdAt shouldBe expiresAt.minus(daysToSubmitApplication)
            dueForExpiry.applicationState shouldBe havingExpiryDate.applicationState

      "makeDueForExpiry works only for Started and GrsDataReceived applications, the ones having an expiry date" in:
        val refusal: String = "still has a deadline"

        ApplicationState.values.foreach: state =>
          withClue(s"state=$state: "):
            state match
              case Started => agentApplicationStates.started.makeDueForExpiry.applicationState shouldBe Started
              case GrsDataReceived => agentApplicationStates.grsDataReceived.makeDueForExpiry.applicationState shouldBe GrsDataReceived
              case SentForRisking => refusalMessage(agentApplicationStates.sentForRisking.makeDueForExpiry) should include(refusal)
              case SentToMinerva => refusalMessage(agentApplicationStates.sentToMinerva.makeDueForExpiry) should include(refusal)
              case RiskingCompleted => refusalMessage(agentApplicationStates.riskingCompleted.makeDueForExpiry) should include(refusal)
              case Expired => refusalMessage(agentApplicationStates.expired.makeDueForExpiry) should include(refusal)

      "makeExpiryReminderEmailSent is due for expiry with the reminder sent now" in:
        agentApplicationStates.havingExpiryDate.foreach: havingExpiryDate =>
          withClue(s"state=${havingExpiryDate.applicationState}: "):
            val expiryReminderEmailSent: AgentApplication = havingExpiryDate.makeExpiryReminderEmailSent
            val expiresAt: Instant = now.plus(expiryReminderPeriod)
            expiryReminderEmailSent.applicationExpiresAt.value shouldBe expiresAt
            expiryReminderEmailSent.createdAt shouldBe expiresAt.minus(daysToSubmitApplication)
            expiryReminderEmailSent.expiryReminderEmailSentAt.value shouldBe now
            expiryReminderEmailSent.applicationState shouldBe havingExpiryDate.applicationState

      "makeExpiryReminderEmailSent works only for Started and GrsDataReceived applications, the ones having an expiry date" in:
        val refusal: String = "still has a deadline"

        ApplicationState.values.foreach: state =>
          withClue(s"state=$state: "):
            state match
              case Started => agentApplicationStates.started.makeExpiryReminderEmailSent.applicationState shouldBe Started
              case GrsDataReceived => agentApplicationStates.grsDataReceived.makeExpiryReminderEmailSent.applicationState shouldBe GrsDataReceived
              case SentForRisking => refusalMessage(agentApplicationStates.sentForRisking.makeExpiryReminderEmailSent) should include(refusal)
              case SentToMinerva => refusalMessage(agentApplicationStates.sentToMinerva.makeExpiryReminderEmailSent) should include(refusal)
              case RiskingCompleted => refusalMessage(agentApplicationStates.riskingCompleted.makeExpiryReminderEmailSent) should include(refusal)
              case Expired => refusalMessage(agentApplicationStates.expired.makeExpiryReminderEmailSent) should include(refusal)

      "makeOverdue sets the expiry date in the past, so the expiry job picks the application up, with the reminder already sent" in:
        agentApplicationStates.havingExpiryDate.foreach: havingExpiryDate =>
          withClue(s"state=${havingExpiryDate.applicationState}: "):
            val overdue: AgentApplication = havingExpiryDate.makeOverdue
            val expiresAt: Instant = overdue.applicationExpiresAt.value
            expiresAt should be < now
            overdue.createdAt shouldBe expiresAt.minus(daysToSubmitApplication)
            overdue.expiryReminderEmailSentAt.value shouldBe expiresAt.minus(expiryReminderPeriod)
            overdue.applicationState shouldBe havingExpiryDate.applicationState

      "makeOverdue works only for Started and GrsDataReceived applications, the ones having an expiry date" in:
        val refusal: String = "still has a deadline"

        ApplicationState.values.foreach: state =>
          withClue(s"state=$state: "):
            state match
              case Started => agentApplicationStates.started.makeOverdue.applicationState shouldBe Started
              case GrsDataReceived => agentApplicationStates.grsDataReceived.makeOverdue.applicationState shouldBe GrsDataReceived
              case SentForRisking => refusalMessage(agentApplicationStates.sentForRisking.makeOverdue) should include(refusal)
              case SentToMinerva => refusalMessage(agentApplicationStates.sentToMinerva.makeOverdue) should include(refusal)
              case RiskingCompleted => refusalMessage(agentApplicationStates.riskingCompleted.makeOverdue) should include(refusal)
              case Expired => refusalMessage(agentApplicationStates.expired.makeOverdue) should include(refusal)

      "makeLongOverdue sets the expiry date further back than the grace period, with no reminder sent" in:
        agentApplicationStates.havingExpiryDate.foreach: havingExpiryDate =>
          withClue(s"state=${havingExpiryDate.applicationState}: "):
            val longOverdue: AgentApplication = havingExpiryDate.makeLongOverdue
            val expiresAt: Instant = longOverdue.applicationExpiresAt.value
            Duration.between(expiresAt, now) should be > postExpiryGracePeriod
            longOverdue.createdAt shouldBe expiresAt.minus(daysToSubmitApplication)
            longOverdue.expiryReminderEmailSentAt shouldBe None
            longOverdue.applicationState shouldBe havingExpiryDate.applicationState

      "makeLongOverdue works only for Started and GrsDataReceived applications, the ones having an expiry date" in:
        val refusal: String = "still has a deadline"

        ApplicationState.values.foreach: state =>
          withClue(s"state=$state: "):
            state match
              case Started => agentApplicationStates.started.makeLongOverdue.applicationState shouldBe Started
              case GrsDataReceived => agentApplicationStates.grsDataReceived.makeLongOverdue.applicationState shouldBe GrsDataReceived
              case SentForRisking => refusalMessage(agentApplicationStates.sentForRisking.makeLongOverdue) should include(refusal)
              case SentToMinerva => refusalMessage(agentApplicationStates.sentToMinerva.makeLongOverdue) should include(refusal)
              case RiskingCompleted => refusalMessage(agentApplicationStates.riskingCompleted.makeLongOverdue) should include(refusal)
              case Expired => refusalMessage(agentApplicationStates.expired.makeLongOverdue) should include(refusal)

      "makeExpired clears the expiry date, sets expiredAt to now and starts the grace period, keeping the reminder" in:
        agentApplicationStates.havingExpiryDate.foreach: havingExpiryDate =>
          withClue(s"state=${havingExpiryDate.applicationState}: "):
            val expired: AgentApplication = havingExpiryDate.makeOverdue.makeExpired
            val deadline: Instant = expired.createdAt.plus(daysToSubmitApplication)
            expired.applicationState shouldBe ApplicationState.Expired
            expired.applicationExpiresAt shouldBe None
            expired.expiredAt.value shouldBe now
            expired.postExpiryGracePeriodEndsAt.value shouldBe now.plus(postExpiryGracePeriod)
            expired.expiryReminderEmailSentAt.value shouldBe deadline.minus(expiryReminderPeriod)

      "makeExpired gives a long overdue application the whole grace period" in:
        agentApplicationStates.havingExpiryDate.foreach: havingExpiryDate =>
          withClue(s"state=${havingExpiryDate.applicationState}: "):
            val expired: AgentApplication = havingExpiryDate.makeLongOverdue.makeExpired
            expired.postExpiryGracePeriodEndsAt.value shouldBe now.plus(postExpiryGracePeriod)
            expired.postExpiryGracePeriodEndsAt.value should be > now

      "makeExpired refuses an application that is not overdue" in:
        agentApplicationStates.havingExpiryDate.foreach: havingExpiryDate =>
          withClue(s"state=${havingExpiryDate.applicationState}: "):
            refusalMessage(havingExpiryDate.makeExpired) should include("only an overdue application can be expired")

      "makeExpired works only for overdue Started and GrsDataReceived applications" in:
        val refusal: String = "only an overdue application can be expired"

        ApplicationState.values.foreach: state =>
          withClue(s"state=$state: "):
            state match
              case Started => agentApplicationStates.started.makeOverdue.makeExpired.applicationState shouldBe Expired
              case GrsDataReceived => agentApplicationStates.grsDataReceived.makeOverdue.makeExpired.applicationState shouldBe Expired
              case SentForRisking => refusalMessage(agentApplicationStates.sentForRisking.makeExpired) should include(refusal)
              case SentToMinerva => refusalMessage(agentApplicationStates.sentToMinerva.makeExpired) should include(refusal)
              case RiskingCompleted => refusalMessage(agentApplicationStates.riskingCompleted.makeExpired) should include(refusal)
              case Expired => refusalMessage(agentApplicationStates.expired.makeExpired) should include(refusal)

      "makeExpiredEmailSent is expired with the expired email sent now" in:
        agentApplicationStates.havingExpiryDate.foreach: havingExpiryDate =>
          withClue(s"state=${havingExpiryDate.applicationState}: "):
            val expiredEmailSent: AgentApplication = havingExpiryDate.makeOverdue.makeExpiredEmailSent
            val deadline: Instant = expiredEmailSent.createdAt.plus(daysToSubmitApplication)
            expiredEmailSent.applicationState shouldBe ApplicationState.Expired
            expiredEmailSent.applicationExpiresAt shouldBe None
            expiredEmailSent.expiredAt.value shouldBe now
            expiredEmailSent.postExpiryGracePeriodEndsAt.value shouldBe now.plus(postExpiryGracePeriod)
            expiredEmailSent.expiryReminderEmailSentAt.value shouldBe deadline.minus(expiryReminderPeriod)
            expiredEmailSent.expiredEmailSentAt.value shouldBe now

      "makeExpiredEmailSent refuses an application that is not overdue" in:
        agentApplicationStates.havingExpiryDate.foreach: havingExpiryDate =>
          withClue(s"state=${havingExpiryDate.applicationState}: "):
            refusalMessage(havingExpiryDate.makeExpiredEmailSent) should include("only an overdue application can be expired")

      "makeExpiredEmailSent works only for overdue Started and GrsDataReceived applications" in:
        val refusal: String = "only an overdue application can be expired"

        ApplicationState.values.foreach: state =>
          withClue(s"state=$state: "):
            state match
              case Started => agentApplicationStates.started.makeOverdue.makeExpiredEmailSent.applicationState shouldBe Expired
              case GrsDataReceived => agentApplicationStates.grsDataReceived.makeOverdue.makeExpiredEmailSent.applicationState shouldBe Expired
              case SentForRisking => refusalMessage(agentApplicationStates.sentForRisking.makeExpiredEmailSent) should include(refusal)
              case SentToMinerva => refusalMessage(agentApplicationStates.sentToMinerva.makeExpiredEmailSent) should include(refusal)
              case RiskingCompleted => refusalMessage(agentApplicationStates.riskingCompleted.makeExpiredEmailSent) should include(refusal)
              case Expired => refusalMessage(agentApplicationStates.expired.makeExpiredEmailSent) should include(refusal)

      "makeDueForDeletion puts the end of the grace period in the past, by moving the sweep rather than the end" in:
        agentApplicationStates.havingExpiryDate.foreach: havingExpiryDate =>
          withClue(s"state=${havingExpiryDate.applicationState}: "):
            val dueForDeletion: AgentApplication = havingExpiryDate.makeOverdue.makeExpired.makeDueForDeletion
            dueForDeletion.postExpiryGracePeriodEndsAt.value should be < now
            dueForDeletion.expiredAt.value shouldBe dueForDeletion.postExpiryGracePeriodEndsAt.value.minus(postExpiryGracePeriod)
            dueForDeletion.applicationState shouldBe ApplicationState.Expired

      "makeDueForDeletion leaves the application a full submission window older than its sweep" in:
        agentApplicationStates.havingExpiryDate.foreach: havingExpiryDate =>
          withClue(s"state=${havingExpiryDate.applicationState}: "):
            val dueForDeletion: AgentApplication = havingExpiryDate.makeOverdue.makeExpired.makeDueForDeletion
            dueForDeletion.createdAt shouldBe dueForDeletion.expiredAt.value.minus(daysToSubmitApplication)

      "makeDueForDeletion moves the emails already sent back with expiredAt" in:
        agentApplicationStates.havingExpiryDate.foreach: havingExpiryDate =>
          withClue(s"state=${havingExpiryDate.applicationState}: "):
            val dueForDeletion: AgentApplication = havingExpiryDate.makeOverdue.makeExpiredEmailSent.makeDueForDeletion
            val expiredAt: Instant = dueForDeletion.expiredAt.value
            val deadline: Instant = dueForDeletion.createdAt.plus(daysToSubmitApplication)
            dueForDeletion.expiryReminderEmailSentAt.value shouldBe deadline.minus(expiryReminderPeriod)
            dueForDeletion.expiredEmailSentAt.value shouldBe expiredAt
            dueForDeletion.deletionEmailSentAt shouldBe None
            dueForDeletion.applicationState shouldBe ApplicationState.Expired

      "makeDueForDeletion works only for Expired applications, and refuses even an overdue one that has not expired" in:
        val refusal: String = "only an expired application can be due for deletion"

        ApplicationState.values.foreach: state =>
          withClue(s"state=$state: "):
            state match
              case Started => refusalMessage(agentApplicationStates.started.makeOverdue.makeDueForDeletion) should include(refusal)
              case GrsDataReceived => refusalMessage(agentApplicationStates.grsDataReceived.makeOverdue.makeDueForDeletion) should include(refusal)
              case SentForRisking => refusalMessage(agentApplicationStates.sentForRisking.makeDueForDeletion) should include(refusal)
              case SentToMinerva => refusalMessage(agentApplicationStates.sentToMinerva.makeDueForDeletion) should include(refusal)
              case RiskingCompleted => refusalMessage(agentApplicationStates.riskingCompleted.makeDueForDeletion) should include(refusal)
              case Expired => agentApplicationStates.expired.makeDueForDeletion.applicationState shouldBe Expired

      "makeDeletionEmailSent is due for deletion with the deletion email sent now" in:
        agentApplicationStates.havingExpiryDate.foreach: havingExpiryDate =>
          withClue(s"state=${havingExpiryDate.applicationState}: "):
            val deletionEmailSent: AgentApplication = havingExpiryDate.makeOverdue.makeExpiredEmailSent.makeDeletionEmailSent
            val expiredAt: Instant = deletionEmailSent.expiredAt.value
            val deadline: Instant = deletionEmailSent.createdAt.plus(daysToSubmitApplication)
            deletionEmailSent.postExpiryGracePeriodEndsAt.value should be < now
            deletionEmailSent.postExpiryGracePeriodEndsAt.value shouldBe expiredAt.plus(postExpiryGracePeriod)
            deletionEmailSent.createdAt shouldBe expiredAt.minus(daysToSubmitApplication)
            deletionEmailSent.expiryReminderEmailSentAt.value shouldBe deadline.minus(expiryReminderPeriod)
            deletionEmailSent.expiredEmailSentAt.value shouldBe expiredAt
            deletionEmailSent.deletionEmailSentAt.value shouldBe now
            deletionEmailSent.applicationState shouldBe ApplicationState.Expired

      "makeDeletionEmailSent works only for Expired applications, and refuses even an overdue one that has not expired" in:
        val refusal: String = "only an expired application can be due for deletion"

        ApplicationState.values.foreach: state =>
          withClue(s"state=$state: "):
            state match
              case Started => refusalMessage(agentApplicationStates.started.makeOverdue.makeDeletionEmailSent) should include(refusal)
              case GrsDataReceived => refusalMessage(agentApplicationStates.grsDataReceived.makeOverdue.makeDeletionEmailSent) should include(refusal)
              case SentForRisking => refusalMessage(agentApplicationStates.sentForRisking.makeDeletionEmailSent) should include(refusal)
              case SentToMinerva => refusalMessage(agentApplicationStates.sentToMinerva.makeDeletionEmailSent) should include(refusal)
              case RiskingCompleted => refusalMessage(agentApplicationStates.riskingCompleted.makeDeletionEmailSent) should include(refusal)
              case Expired => agentApplicationStates.expired.makeDeletionEmailSent.applicationState shouldBe Expired
    }

  "gives back the type it was given, so a transformed Llp is still an Llp" in:
    // the ascriptions are the test: were the type widened to AgentApplication, this would not compile
    val overdue: AgentApplicationLlp = tdAll.agentApplicationLlp.afterStarted.makeOverdue
    val expired: AgentApplicationLlp = overdue.makeExpired
    val dueForDeletion: AgentApplicationLlp = expired.makeDueForDeletion
    val soleTrader: AgentApplicationSoleTrader = tdAll.agentApplicationSoleTrader.afterStarted.makeLongOverdue
    val expiryReminderEmailSent: AgentApplicationLlp = tdAll.agentApplicationLlp.afterStarted.makeExpiryReminderEmailSent
    val expiredEmailSent: AgentApplicationLlp = overdue.makeExpiredEmailSent
    val deletionEmailSent: AgentApplicationLlp = expired.makeDeletionEmailSent
    Seq[AgentApplication](
      overdue,
      expired,
      dueForDeletion,
      soleTrader,
      expiryReminderEmailSent,
      expiredEmailSent,
      deletionEmailSent
    ).map(_.applicationState) shouldBe Seq(
      ApplicationState.Started,
      ApplicationState.Expired,
      ApplicationState.Expired,
      ApplicationState.Started,
      ApplicationState.Started,
      ApplicationState.Expired,
      ApplicationState.Expired
    )

  private lazy val tdAll: TdAll = TdAll.tdAll

  private def now: Instant = tdAll.now

  private def daysToSubmitApplication: Duration = tdAll.daysToSubmitApplication

  private def postExpiryGracePeriod: Duration = tdAll.postExpiryGracePeriod

  private def expiryReminderPeriod: Duration = tdAll.expiryReminderPeriod

  private def refusalMessage(transformed: => AgentApplication): String = intercept[IllegalArgumentException](transformed).getMessage
