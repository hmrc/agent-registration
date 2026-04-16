/*
 * Copyright 2025 HM Revenue & Customs
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

import uk.gov.hmrc.agentregistration.shared.ApplicationState.GrsDataReceived
import uk.gov.hmrc.agentregistration.shared.*
import uk.gov.hmrc.agentregistration.shared.lists.FiveOrLess
import uk.gov.hmrc.agentregistration.shared.lists.SixOrMore
import uk.gov.hmrc.agentregistration.shared.testdata.TdBase
import uk.gov.hmrc.agentregistration.shared.testdata.TdGrsBusinessDetails

trait TdAgentApplicationScottishPartnership { dependencies: (TdBase & TdGrsBusinessDetails) =>

  object agentApplicationScottishPartnership:

    val afterStarted: AgentApplicationScottishPartnership = AgentApplicationScottishPartnership(
      _id = dependencies.agentApplicationId,
      applicationReference = dependencies.applicationReference,
      internalUserId = dependencies.internalUserId,
      applicantCredentials = dependencies.credentials,
      linkId = dependencies.linkId,
      groupId = dependencies.groupId,
      createdAt = dependencies.nowAsInstant,
      submittedAt = None,
      applicationState = ApplicationState.Started,
      userRole = Some(UserRole.Authorised),
      businessDetails = None,
      applicantContactDetails = None,
      amlsDetails = None,
      agentDetails = None,
      refusalToDealWithCheckResult = None,
      hmrcStandardForAgentsAgreed = StateOfAgreement.NotSet,
      numberOfIndividuals = None,
      hasOtherRelevantIndividuals = None,
      vrns = None,
      payeRefs = None
    )

    val afterGrsDataReceived: AgentApplicationScottishPartnership = afterStarted.copy(
      businessDetails = Some(
        dependencies.grsBusinessDetails.scottishPartnership.businessDetails
      ),
      applicationState = GrsDataReceived
    )

    val afterRefusalToDealWithCheckPass: AgentApplicationScottishPartnership = afterGrsDataReceived.copy(
      refusalToDealWithCheckResult = Some(CheckResult.Pass)
    )

    val afterRefusalToDealWithCheckFail: AgentApplicationScottishPartnership = afterGrsDataReceived.copy(
      refusalToDealWithCheckResult = Some(CheckResult.Fail)
    )

    val afterContactDetailsComplete: AgentApplicationScottishPartnership = afterRefusalToDealWithCheckPass.copy(
      applicantContactDetails = Some(dependencies.applicantContactDetails),
      agentDetails = None
    )

    val afterAgentDetailsComplete: AgentApplicationScottishPartnership = afterContactDetailsComplete.copy(
      agentDetails = Some(dependencies.completeAgentDetails)
    )

    val afterAmlsComplete: AgentApplicationScottishPartnership = afterAgentDetailsComplete.copy(
      amlsDetails = Some(dependencies.completeAmlsDetails)
    )

    val afterHmrcStandardForAgentsAgreed: AgentApplicationScottishPartnership = afterAmlsComplete.copy(
      hmrcStandardForAgentsAgreed = StateOfAgreement.Agreed
    )

    val afterHowManyKeyIndividuals: AgentApplicationScottishPartnership = afterHmrcStandardForAgentsAgreed.copy(
      numberOfIndividuals = Some(
        FiveOrLess(
          numberOfKeyIndividuals = 3
        )
      )
    )

    val afterOnlyOneKeyIndividual: AgentApplicationScottishPartnership = afterHmrcStandardForAgentsAgreed.copy(
      numberOfIndividuals = Some(
        FiveOrLess(
          numberOfKeyIndividuals = 1
        )
      )
    )

    val afterHowManyKeyIndividualsNeedsNoPadding: AgentApplicationScottishPartnership = afterHmrcStandardForAgentsAgreed.copy(
      numberOfIndividuals = Some(
        SixOrMore(
          numberOfKeyIndividualsResponsibleForTaxMatters = 6
        )
      )
    )

    // when the number of key individuals is of type SixOrMore and padding is required because the number of
    // key individuals responsible for tax matters is less than minimum list size(5)
    val afterHowManyKeyIndividualsNeedsPadding: AgentApplicationScottishPartnership = afterHmrcStandardForAgentsAgreed.copy(
      numberOfIndividuals = Some(dependencies.sixOrMoreKeyIndividuals)
    )

    val afterDeclarationSubmitted: AgentApplicationScottishPartnership = afterHmrcStandardForAgentsAgreed.copy(
      applicationState = ApplicationState.SentForRisking,
      submittedAt = Some(dependencies.nowAsInstant)
    )

}
