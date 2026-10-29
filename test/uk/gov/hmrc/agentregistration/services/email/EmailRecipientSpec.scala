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

package uk.gov.hmrc.agentregistration.services.email

import uk.gov.hmrc.agentregistration.testsupport.UnitSpec
import uk.gov.hmrc.agentregistration.testsupport.testdata.TdAll.tdAll

class EmailRecipientSpec
extends UnitSpec:

  "resolve" - {
    "returns None when there are no applicant contact details" in:
      EmailRecipient.resolve(tdAll.agentApplicationLlp.afterStarted) shouldBe None

    "returns None when the applicant email is not verified and there are no agent details" in:
      EmailRecipient.resolve(tdAll.agentApplicationLlp.afterContactDetailsWithUnverifiedEmail) shouldBe None

    "returns the verified applicant email when there are no agent details" in:
      EmailRecipient.resolve(tdAll.agentApplicationLlp.afterContactDetailsComplete) shouldBe Some(
        EmailRecipient(tdAll.applicantName, tdAll.applicantEmailAddress)
      )

    "returns the verified applicant email when the agent email is not verified" in:
      EmailRecipient.resolve(tdAll.agentApplicationLlp.afterAgentDetailsWithUnverifiedEmail) shouldBe Some(
        EmailRecipient(tdAll.applicantName, tdAll.applicantEmailAddress)
      )

    "returns the verified agent email when agent details are complete" in:
      EmailRecipient.resolve(tdAll.agentApplicationLlp.afterAgentDetailsComplete) shouldBe Some(
        EmailRecipient(tdAll.applicantName, tdAll.agentVerifiedEmailAddress.getEmailAddress)
      )
  }
