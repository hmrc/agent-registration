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
import java.time.LocalDate
import uk.gov.hmrc.agentregistration.shared.testdata.TdTime

/** Both mirror production config. */
object TdAgentApplicationTime:

  val daysToSubmitApplication: Duration = Duration.ofDays(73)

  val postExpiryGracePeriod: Duration = Duration.ofDays(45)

/** Application timings derived from one `createdAt`. `ExpiryTransformations` calculates overdue and expired timings. */
trait TdAgentApplicationTime { dependencies: TdTime =>

  export TdAgentApplicationTime.*

  /** An application is created once, so this stays fixed throughout a fixture chain. */
  def createdAt: Instant = dependencies.now.minus(Duration.ofDays(10))

  def submittedAt: Instant = createdAt.plus(Duration.ofDays(3))
  def reSubmittedAt: Instant = submittedAt.plus(Duration.ofDays(1))

  def applicationExpiresAt: Instant = createdAt.plus(daysToSubmitApplication)

  /** Set when the expiry job runs, not when the submission window ends. The grace period starts here. */
  def expiredAt: Instant = dependencies.now
  def postExpiryGracePeriodEndsAt: Instant = expiredAt.plus(postExpiryGracePeriod)

  def newApplicationCreatedAt: Instant = dependencies.now.plusSeconds(20)

  def riskingCompletedDate: LocalDate = dependencies.nowAsLocalDate.minusDays(1)
  def correctiveActionExpiryDate: LocalDate = dependencies.nowAsLocalDate.plusDays(45)

}
