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

import uk.gov.hmrc.agentregistration.shared.AgentApplicationGeneralPartnership
import uk.gov.hmrc.agentregistration.shared.ApplicationState
import uk.gov.hmrc.agentregistration.shared.ApplicationState.GrsDataReceived
import uk.gov.hmrc.agentregistration.shared.CheckResult
import uk.gov.hmrc.agentregistration.shared.StateOfAgreement
import uk.gov.hmrc.agentregistration.shared.UserRole
import uk.gov.hmrc.agentregistration.shared.lists.FiveOrLess
import uk.gov.hmrc.agentregistration.shared.lists.SixOrMore
import uk.gov.hmrc.agentregistration.shared.testdata.TdBase
import uk.gov.hmrc.agentregistration.shared.testdata.TdGrsBusinessDetails

trait TdAgentApplicationGeneralPartnership { dependencies: (TdBase & TdGrsBusinessDetails) =>

  object agentApplicationGeneralPartnership:

    val afterStarted: AgentApplicationGeneralPartnership = AgentApplicationGeneralPartnership(
      _id = dependencies.agentApplicationId,
      cachedSessionId = dependencies.cachedSessionId,
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

    val afterGrsDataReceived: AgentApplicationGeneralPartnership = afterStarted.copy(
      businessDetails = Some(
        dependencies.grsBusinessDetails.generalPartnership.businessDetails
      ),
      applicationState = GrsDataReceived
    )

    val afterRefusalToDealWithCheckPass: AgentApplicationGeneralPartnership = afterGrsDataReceived.copy(
      refusalToDealWithCheckResult = Some(CheckResult.Pass)
    )

    val afterRefusalToDealWithCheckFail: AgentApplicationGeneralPartnership = afterGrsDataReceived.copy(
      refusalToDealWithCheckResult = Some(CheckResult.Fail)
    )

    val afterContactDetailsComplete: AgentApplicationGeneralPartnership = afterRefusalToDealWithCheckPass.copy(
      applicantContactDetails = Some(dependencies.applicantContactDetails),
      agentDetails = None
    )

    val afterAgentDetailsComplete: AgentApplicationGeneralPartnership = afterContactDetailsComplete.copy(
      agentDetails = Some(dependencies.completeAgentDetails)
    )

    val afterAmlsComplete: AgentApplicationGeneralPartnership = afterAgentDetailsComplete.copy(
      amlsDetails = Some(dependencies.completeAmlsDetails)
    )

    val afterHmrcStandardForAgentsAgreed: AgentApplicationGeneralPartnership = afterAmlsComplete.copy(
      hmrcStandardForAgentsAgreed = StateOfAgreement.Agreed
    )

    val afterHowManyKeyIndividuals: AgentApplicationGeneralPartnership = afterHmrcStandardForAgentsAgreed.copy(
      numberOfIndividuals = Some(
        FiveOrLess(
          numberOfKeyIndividuals = 3
        )
      )
    )

    val afterOnlyOneKeyIndividual: AgentApplicationGeneralPartnership = afterHmrcStandardForAgentsAgreed.copy(
      numberOfIndividuals = Some(
        FiveOrLess(
          numberOfKeyIndividuals = 1
        )
      )
    )

    val afterHowManyKeyIndividualsNeedsNoPadding: AgentApplicationGeneralPartnership = afterHmrcStandardForAgentsAgreed.copy(
      numberOfIndividuals = Some(
        SixOrMore(
          numberOfKeyIndividualsResponsibleForTaxMatters = 6
        )
      )
    )

    // when the number of key individuals is of type SixOrMore and padding is required because the number of
    // key individuals responsible for tax matters is less than minimum list size(5)
    val afterHowManyKeyIndividualsNeedsPadding: AgentApplicationGeneralPartnership = afterHmrcStandardForAgentsAgreed.copy(
      numberOfIndividuals = Some(dependencies.sixOrMoreKeyIndividuals)
    )

    val afterConfirmOtherRelevantIndividualsYes: AgentApplicationGeneralPartnership = afterHowManyKeyIndividuals
      .copy(
        hasOtherRelevantIndividuals = Some(true)
      )

    val afterConfirmOtherRelevantIndividualsNo: AgentApplicationGeneralPartnership = afterHowManyKeyIndividuals
      .copy(
        hasOtherRelevantIndividuals = Some(false)
      )

    val afterConfirmTwoOtherRelevantIndividualsNo: AgentApplicationGeneralPartnership = afterHowManyKeyIndividuals
      .copy(
        numberOfIndividuals = Some(
          SixOrMore(
            numberOfKeyIndividualsResponsibleForTaxMatters = 2
          )
        ),
        hasOtherRelevantIndividuals = Some(false)
      )

    val afterConfirmTwoIndividuals: AgentApplicationGeneralPartnership = afterHowManyKeyIndividuals
      .copy(
        numberOfIndividuals = Some(
          FiveOrLess(
            numberOfKeyIndividuals = 2
          )
        ),
        hasOtherRelevantIndividuals = Some(false)
      )

    val afterConfirmSixIndividuals: AgentApplicationGeneralPartnership = afterHowManyKeyIndividuals
      .copy(
        numberOfIndividuals = Some(
          SixOrMore(
            numberOfKeyIndividualsResponsibleForTaxMatters = 6
          )
        ),
        hasOtherRelevantIndividuals = Some(false)
      )

    val afterDeclarationSubmitted: AgentApplicationGeneralPartnership = afterHmrcStandardForAgentsAgreed.copy(
      applicationState = ApplicationState.SentForRisking,
      submittedAt = Some(dependencies.nowAsInstant)
    )

    val afterDeclarationSubmittedAndTwoIndividualFinished: AgentApplicationGeneralPartnership = afterConfirmTwoIndividuals.copy(
      applicationState = ApplicationState.SentForRisking,
      submittedAt = Some(dependencies.nowAsInstant)
    )

}
