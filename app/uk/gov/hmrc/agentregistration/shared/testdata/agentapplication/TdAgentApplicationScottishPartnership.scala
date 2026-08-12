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
import uk.gov.hmrc.agentregistration.shared.testdata.agentapplication.DataIntegrityAssertion.assertDataIntegrity

trait TdAgentApplicationScottishPartnership { dependencies: (TdBase & TdGrsBusinessDetails) =>

  object agentApplicationScottishPartnership:

    val afterStarted: AgentApplicationScottishPartnership = AgentApplicationScottishPartnership(
      _id = dependencies.agentApplicationId,
      cachedSessionId = dependencies.cachedSessionId,
      applicationReference = dependencies.applicationReference,
      internalUserId = dependencies.internalUserId,
      applicantCredentials = dependencies.credentials,
      linkId = dependencies.linkId,
      groupId = dependencies.groupId,
      createdAt = dependencies.nowAsInstant,
      applicationExpiresAt = Some(dependencies.applicationExpiresAtAsInstant),
      submittedAt = None,
      applicationState = ApplicationState.Started,
      userRole = Some(UserRole.Authorised),
      businessDetails = None,
      applicantContactDetails = None,
      amlsDetails = None,
      agentDetails = None,
      refusalToDealWithCheckResult = None,
      globalAsaEnrolmentCheckResult = None,
      hmrcStandardForAgentsAgreed = StateOfAgreement.NotSet,
      numberOfIndividuals = None,
      hasOtherRelevantIndividuals = None,
      vrns = None,
      payeRefs = None,
      riskingOutcomeApplication = None,
      riskingOutcomeEntity = None
    ).assertDataIntegrity()

    val afterGrsDataReceived: AgentApplicationScottishPartnership = afterStarted.copy(
      businessDetails = Some(
        dependencies.grsBusinessDetails.scottishPartnership.businessDetails
      ),
      applicationState = GrsDataReceived
    ).assertDataIntegrity()

    val afterRefusalToDealWithCheckPass: AgentApplicationScottishPartnership = afterGrsDataReceived.copy(
      refusalToDealWithCheckResult = Some(CheckResult.Pass)
    ).assertDataIntegrity()

    val afterRefusalToDealWithCheckFail: AgentApplicationScottishPartnership = afterGrsDataReceived.copy(
      refusalToDealWithCheckResult = Some(CheckResult.Fail)
    ).assertDataIntegrity()

    val afterUnifiedCustomerRegistryUpdateIdentifiers: AgentApplicationScottishPartnership = afterRefusalToDealWithCheckPass.copy(
      vrns = Some(List(dependencies.vrn)),
      payeRefs = Some(List(dependencies.payeRef))
    ).assertDataIntegrity()

    val afterGlobalAsaEnrolmentCheckPass: AgentApplicationScottishPartnership = afterUnifiedCustomerRegistryUpdateIdentifiers.copy(
      globalAsaEnrolmentCheckResult = Some(CheckResult.Pass)
    ).assertDataIntegrity()

    val afterGlobalAsaEnrolmentCheckFail: AgentApplicationScottishPartnership = afterUnifiedCustomerRegistryUpdateIdentifiers.copy(
      globalAsaEnrolmentCheckResult = Some(CheckResult.Fail)
    ).assertDataIntegrity()

    val afterContactDetailsComplete: AgentApplicationScottishPartnership = afterGlobalAsaEnrolmentCheckPass.copy(
      applicantContactDetails = Some(dependencies.applicantContactDetails),
      agentDetails = None
    ).assertDataIntegrity()

    val afterAgentDetailsComplete: AgentApplicationScottishPartnership = afterContactDetailsComplete.copy(
      agentDetails = Some(dependencies.completeAgentDetails)
    ).assertDataIntegrity()

    val afterAmlsComplete: AgentApplicationScottishPartnership = afterAgentDetailsComplete.copy(
      amlsDetails = Some(dependencies.completeAmlsDetails)
    ).assertDataIntegrity()

    val afterHmrcStandardForAgentsAgreed: AgentApplicationScottishPartnership = afterAmlsComplete.copy(
      hmrcStandardForAgentsAgreed = StateOfAgreement.Agreed
    ).assertDataIntegrity()

    val afterHowManyKeyIndividuals: AgentApplicationScottishPartnership = afterHmrcStandardForAgentsAgreed.copy(
      numberOfIndividuals = Some(
        FiveOrLess(
          numberOfKeyIndividuals = 3
        )
      )
    ).assertDataIntegrity()

    val afterConfirmOtherRelevantIndividualsNo: AgentApplicationScottishPartnership = afterHowManyKeyIndividuals
      .copy(
        hasOtherRelevantIndividuals = Some(false)
      ).assertDataIntegrity()

    val afterOnlyOneKeyIndividual: AgentApplicationScottishPartnership = afterHmrcStandardForAgentsAgreed.copy(
      numberOfIndividuals = Some(
        FiveOrLess(
          numberOfKeyIndividuals = 1
        )
      )
    ).assertDataIntegrity()

    val afterHowManyKeyIndividualsNeedsNoPadding: AgentApplicationScottishPartnership = afterHmrcStandardForAgentsAgreed.copy(
      numberOfIndividuals = Some(
        SixOrMore(
          numberOfKeyIndividualsResponsibleForTaxMatters = 6
        )
      )
    ).assertDataIntegrity()

    // when the number of key individuals is of type SixOrMore and padding is required because the number of
    // key individuals responsible for tax matters is less than minimum list size(5)
    val afterHowManyKeyIndividualsNeedsPadding: AgentApplicationScottishPartnership = afterHmrcStandardForAgentsAgreed.copy(
      numberOfIndividuals = Some(dependencies.sixOrMoreKeyIndividuals)
    ).assertDataIntegrity()

    val afterConfirmTwoIndividuals: AgentApplicationScottishPartnership = afterHowManyKeyIndividuals
      .copy(
        numberOfIndividuals = Some(
          FiveOrLess(
            numberOfKeyIndividuals = 2
          )
        ),
        hasOtherRelevantIndividuals = Some(false)
      ).assertDataIntegrity()

    val afterConfirmSixIndividuals: AgentApplicationScottishPartnership = afterHowManyKeyIndividuals
      .copy(
        numberOfIndividuals = Some(
          SixOrMore(
            numberOfKeyIndividualsResponsibleForTaxMatters = 6
          )
        ),
        hasOtherRelevantIndividuals = Some(false)
      ).assertDataIntegrity()

    val afterDeclarationSubmitted: AgentApplicationScottishPartnership = afterHmrcStandardForAgentsAgreed.copy(
      applicationState = ApplicationState.SentForRisking,
      submittedAt = Some(dependencies.nowAsInstant),
      applicationExpiresAt = None
    ).assertDataIntegrity()

    val afterDeclarationSubmittedAndTwoIndividualFinished: AgentApplicationScottishPartnership = afterConfirmTwoIndividuals.copy(
      applicationState = ApplicationState.SentForRisking,
      submittedAt = Some(dependencies.nowAsInstant),
      applicationExpiresAt = None
    ).assertDataIntegrity()

}
