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
import uk.gov.hmrc.agentregistration.shared.*
import uk.gov.hmrc.agentregistration.shared.testdata.TdTime.now
import uk.gov.hmrc.agentregistration.shared.testdata.agentapplication.DataIntegrityAssertion.assertDataIntegrity
import uk.gov.hmrc.agentregistration.shared.testdata.agentapplication.TdAgentApplicationTime.daysToSubmitApplication
import uk.gov.hmrc.agentregistration.shared.testdata.agentapplication.TdAgentApplicationTime.postExpiryGracePeriod
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.*

/** Makes any application fixture overdue, expired or due for deletion:
  * {{{
  * agentApplicationLlp.afterStarted.makeLongOverdue.makeExpired.makeDueForDeletion
  * }}}
  */
object ExpiryTransformations:

  private val overdueBy: Duration = Duration.ofDays(7)

  private val postExpiryGracePeriodEndedBy: Duration = Duration.ofSeconds(60)

  extension [A <: AgentApplication](agentApplication: A)

    def makeOverdueBy(by: Duration): A =
      require(
        agentApplication.applicationExpiresAt.isDefined,
        s"only an application that still has a deadline can become overdue, and this one is ${agentApplication.applicationState} with none"
      )
      require(!by.isNegative && !by.isZero, s"an overdue application is past its deadline, so `by` has to be positive, not $by")
      val expiresAt: Instant = now.minus(by)
      agentApplication
        .withTimes(
          createdAt = expiresAt.minus(daysToSubmitApplication),
          applicationExpiresAt = Some(expiresAt)
        )
        .assertDataIntegrity()

    def makeOverdue: A = agentApplication.makeOverdueBy(overdueBy)

    /** Overdue beyond the grace period, as found when the expiry job was first deployed. */
    def makeLongOverdue: A = agentApplication.makeOverdueBy(postExpiryGracePeriod.plus(overdueBy))

    def makeExpired: A =
      require(
        agentApplication.applicationExpiresAt.exists(_.isBefore(now)),
        s"only an overdue application can be expired, and this one expires at ${agentApplication.applicationExpiresAt} which is not before ${now} - call makeOverdue first"
      )
      agentApplication
        .withTimes(
          applicationExpiresAt = None,
          expiredAt = Some(now),
          postExpiryGracePeriodEndsAt = Some(now.plus(postExpiryGracePeriod)),
          applicationState = ApplicationState.Expired
        )
        .assertDataIntegrity()

    def makeDueForDeletion: A =
      require(
        agentApplication.applicationState === ApplicationState.Expired,
        s"only an expired application can be due for deletion, and this one is ${agentApplication.applicationState} - call makeExpired first"
      )
      val expiredAt: Instant = now.minus(postExpiryGracePeriod).minus(postExpiryGracePeriodEndedBy)
      agentApplication
        .withTimes(
          createdAt = expiredAt.minus(daysToSubmitApplication),
          expiredAt = Some(expiredAt),
          postExpiryGracePeriodEndsAt = Some(expiredAt.plus(postExpiryGracePeriod))
        )
        .assertDataIntegrity()

    /** The cast is safe: each branch copies its matched class, preserving the input type. The compiler cannot see this because `copy` returns its own class. */
    @SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf"))
    private def withTimes(
      createdAt: Instant = agentApplication.createdAt,
      applicationExpiresAt: Option[Instant] = agentApplication.applicationExpiresAt,
      expiredAt: Option[Instant] = agentApplication.expiredAt,
      postExpiryGracePeriodEndsAt: Option[Instant] = agentApplication.postExpiryGracePeriodEndsAt,
      applicationState: ApplicationState = agentApplication.applicationState
    ): A =
      val withTimesChanged: AgentApplication =
        agentApplication match
          case a: AgentApplicationSoleTrader =>
            a.copy(
              createdAt = createdAt,
              applicationExpiresAt = applicationExpiresAt,
              expiredAt = expiredAt,
              postExpiryGracePeriodEndsAt = postExpiryGracePeriodEndsAt,
              applicationState = applicationState
            )
          case a: AgentApplicationLlp =>
            a.copy(
              createdAt = createdAt,
              applicationExpiresAt = applicationExpiresAt,
              expiredAt = expiredAt,
              postExpiryGracePeriodEndsAt = postExpiryGracePeriodEndsAt,
              applicationState = applicationState
            )
          case a: AgentApplicationLimitedCompany =>
            a.copy(
              createdAt = createdAt,
              applicationExpiresAt = applicationExpiresAt,
              expiredAt = expiredAt,
              postExpiryGracePeriodEndsAt = postExpiryGracePeriodEndsAt,
              applicationState = applicationState
            )
          case a: AgentApplicationGeneralPartnership =>
            a.copy(
              createdAt = createdAt,
              applicationExpiresAt = applicationExpiresAt,
              expiredAt = expiredAt,
              postExpiryGracePeriodEndsAt = postExpiryGracePeriodEndsAt,
              applicationState = applicationState
            )
          case a: AgentApplicationLimitedPartnership =>
            a.copy(
              createdAt = createdAt,
              applicationExpiresAt = applicationExpiresAt,
              expiredAt = expiredAt,
              postExpiryGracePeriodEndsAt = postExpiryGracePeriodEndsAt,
              applicationState = applicationState
            )
          case a: AgentApplicationScottishLimitedPartnership =>
            a.copy(
              createdAt = createdAt,
              applicationExpiresAt = applicationExpiresAt,
              expiredAt = expiredAt,
              postExpiryGracePeriodEndsAt = postExpiryGracePeriodEndsAt,
              applicationState = applicationState
            )
          case a: AgentApplicationScottishPartnership =>
            a.copy(
              createdAt = createdAt,
              applicationExpiresAt = applicationExpiresAt,
              expiredAt = expiredAt,
              postExpiryGracePeriodEndsAt = postExpiryGracePeriodEndsAt,
              applicationState = applicationState
            )
      withTimesChanged.asInstanceOf[A]
