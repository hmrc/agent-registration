/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.agentregistration.testsupport.testdata

import java.time.Instant
import org.scalatest.Assertion
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.testdata.TdTime.now
import uk.gov.hmrc.agentregistration.shared.testdata.agentapplication.TdAgentApplicationTime.expiryReminderPeriod
import uk.gov.hmrc.agentregistration.testsupport.RichMatchers.*

/** What a test states about the application it is about to use, so a changed fixture fails here and not for an unclear reason later. */
object AgentApplicationPreconditions:

  private val expiryReminderPeriodEndsAt: Instant = now.plus(expiryReminderPeriod)

  extension (agentApplication: AgentApplication)

    def shouldHaveExpiryDateInTheFuture: Assertion =
      agentApplication.applicationExpiresAt.value.isAfter(now) shouldBe true withClue
        "precondition: applicationExpiresAt must be in the future"

    def shouldHaveExpiryDateInThePast: Assertion =
      agentApplication.applicationExpiresAt.value.isBefore(now) shouldBe true withClue
        "precondition: applicationExpiresAt must be in the past"

    def shouldHaveExpiryDateWithinReminderPeriod: Assertion =
      agentApplication.applicationExpiresAt.value.isBefore(now) shouldBe false withClue
        "precondition: applicationExpiresAt must not be in the past"
      agentApplication.applicationExpiresAt.value.isAfter(expiryReminderPeriodEndsAt) shouldBe false withClue
        "precondition: applicationExpiresAt must be within the reminder period"

    def shouldHaveExpiryDateAfterReminderPeriod: Assertion =
      agentApplication.applicationExpiresAt.value.isAfter(expiryReminderPeriodEndsAt) shouldBe true withClue
        "precondition: applicationExpiresAt must be after the reminder period"

    def shouldHaveGracePeriodEndInTheFuture: Assertion =
      agentApplication.postExpiryGracePeriodEndsAt.value.isAfter(now) shouldBe true withClue
        "precondition: postExpiryGracePeriodEndsAt must be in the future"

    def shouldHaveGracePeriodEndInThePast: Assertion =
      agentApplication.postExpiryGracePeriodEndsAt.value.isBefore(now) shouldBe true withClue
        "precondition: postExpiryGracePeriodEndsAt must be in the past"
