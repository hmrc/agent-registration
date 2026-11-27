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

import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.agentdetails.AgentVerifiedEmailAddress
import uk.gov.hmrc.agentregistration.testsupport.UnitSpec
import uk.gov.hmrc.agentregistration.testsupport.testdata.TdAll.tdAll

class EmailRecipientSpec
extends UnitSpec:

  "resolve returns no recipient when there are no applicant contact details" in:
    EmailRecipient.resolve(tdAll.agentApplicationLlp.afterStarted) shouldBe None

  "resolve returns no recipient when the applicant email is not verified and there are no agent details" in:
    EmailRecipient.resolve(tdAll.agentApplicationLlp.afterContactDetailsWithUnverifiedEmail) shouldBe None

  "resolve returns the verified applicant email when there are no agent details" in:
    EmailRecipient.resolve(tdAll.agentApplicationLlp.afterContactDetailsComplete) shouldBe Some(
      EmailRecipient(tdAll.applicantName, tdAll.applicantEmailAddress)
    )

  "resolve returns the verified applicant email when the agent email is not verified" in:
    val agentApplication: AgentApplication = tdAll.agentApplicationLlp.afterAgentDetailsWithUnverifiedEmail
    val agentVerifiedEmailAddress: AgentVerifiedEmailAddress = agentApplication.agentDetails.value.agentEmailAddress.value
    agentVerifiedEmailAddress.isVerified shouldBe false withClue "precondition: the agent email must not be verified"
    agentVerifiedEmailAddress.getEmailAddress should not be tdAll.applicantEmailAddress withClue
      "precondition: the agent email must differ from the applicant email, or the result could not tell them apart"

    EmailRecipient.resolve(agentApplication) shouldBe Some(EmailRecipient(tdAll.applicantName, tdAll.applicantEmailAddress))

  "resolve returns the verified agent email when it is the same as the applicant email" in:
    val agentApplication: AgentApplication = tdAll.agentApplicationLlp.afterAgentDetailsComplete
    val agentVerifiedEmailAddress: AgentVerifiedEmailAddress = agentApplication.agentDetails.value.agentEmailAddress.value
    agentVerifiedEmailAddress.isVerified shouldBe true withClue "precondition: the agent email must be verified"
    agentVerifiedEmailAddress.getEmailAddress shouldBe tdAll.applicantEmailAddress withClue
      "precondition: the agent must use the applicant email"

    EmailRecipient.resolve(agentApplication) shouldBe Some(EmailRecipient(tdAll.applicantName, tdAll.applicantEmailAddress))

  "resolve prefers the verified agent email over the verified applicant email when they differ" in:
    val agentApplication: AgentApplication = tdAll.agentApplicationLlp.afterAgentDetailsCompleteWithOtherAgentEmail
    val agentVerifiedEmailAddress: AgentVerifiedEmailAddress = agentApplication.agentDetails.value.agentEmailAddress.value
    agentVerifiedEmailAddress.isVerified shouldBe true withClue "precondition: the agent email must be verified"
    tdAll.otherAgentEmailAddress should not be tdAll.applicantEmailAddress withClue
      "precondition: the agent email must differ from the applicant email, or the result could not tell them apart"

    EmailRecipient.resolve(agentApplication) shouldBe Some(EmailRecipient(tdAll.applicantName, tdAll.otherAgentEmailAddress))
