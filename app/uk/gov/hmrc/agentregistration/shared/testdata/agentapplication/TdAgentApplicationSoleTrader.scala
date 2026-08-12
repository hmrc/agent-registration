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

import uk.gov.hmrc.agentregistration.shared.*
import uk.gov.hmrc.agentregistration.shared.ApplicationState.GrsDataReceived
import uk.gov.hmrc.agentregistration.shared.risking.RiskingOutcomeEntity
import uk.gov.hmrc.agentregistration.shared.testdata.TdBase
import uk.gov.hmrc.agentregistration.shared.testdata.TdGrsBusinessDetails
import uk.gov.hmrc.agentregistration.shared.testdata.agentapplication.DataIntegrityAssertion.assertDataIntegrity

trait TdAgentApplicationSoleTrader { dependencies: (TdBase & TdGrsBusinessDetails) =>

  object agentApplicationSoleTrader:

    val afterStarted: AgentApplicationSoleTrader = AgentApplicationSoleTrader(
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
      userRole = Some(UserRole.Owner),
      businessDetails = None,
      applicantContactDetails = None,
      amlsDetails = None,
      agentDetails = None,
      refusalToDealWithCheckResult = None,
      globalAsaEnrolmentCheckResult = None,
      deceasedCheckResult = None,
      hmrcStandardForAgentsAgreed = StateOfAgreement.NotSet,
      hasOtherRelevantIndividuals = None,
      vrns = None,
      payeRefs = None,
      riskingOutcomeApplication = None,
      riskingOutcomeEntity = None
    ).assertDataIntegrity()

    val afterGrsDataReceived: AgentApplicationSoleTrader = afterStarted.copy(
      businessDetails = Some(
        dependencies.grsBusinessDetails.soleTrader.businessDetails
      ),
      applicationState = GrsDataReceived
    ).assertDataIntegrity()

    val afterRefusalToDealWithCheckPass: AgentApplicationSoleTrader = afterGrsDataReceived.copy(
      refusalToDealWithCheckResult = Some(CheckResult.Pass)
    ).assertDataIntegrity()

    val afterRefusalToDealWithCheckFail: AgentApplicationSoleTrader = afterGrsDataReceived.copy(
      refusalToDealWithCheckResult = Some(CheckResult.Fail)
    ).assertDataIntegrity()

    val afterDeceasedCheckPass: AgentApplicationSoleTrader = afterRefusalToDealWithCheckPass.copy(
      deceasedCheckResult = Some(CheckResult.Pass)
    ).assertDataIntegrity()

    val afterDeceasedCheckFail: AgentApplicationSoleTrader = afterRefusalToDealWithCheckPass.copy(
      deceasedCheckResult = Some(CheckResult.Fail)
    ).assertDataIntegrity()

    val afterUnifiedCustomerRegistryUpdateIdentifiers: AgentApplicationSoleTrader = afterDeceasedCheckPass.copy(
      vrns = Some(List(dependencies.vrn)),
      payeRefs = Some(List(dependencies.payeRef))
    ).assertDataIntegrity()

    val afterGlobalAsaEnrolmentCheckPass: AgentApplicationSoleTrader = afterUnifiedCustomerRegistryUpdateIdentifiers.copy(
      globalAsaEnrolmentCheckResult = Some(CheckResult.Pass)
    ).assertDataIntegrity()

    val afterGlobalAsaEnrolmentCheckFail: AgentApplicationSoleTrader = afterUnifiedCustomerRegistryUpdateIdentifiers.copy(
      globalAsaEnrolmentCheckResult = Some(CheckResult.Fail)
    ).assertDataIntegrity()

    val afterContactDetailsComplete: AgentApplicationSoleTrader = afterGlobalAsaEnrolmentCheckPass.copy(
      applicantContactDetails = Some(dependencies.applicantContactDetails),
      agentDetails = None
    ).assertDataIntegrity()

    val afterAgentDetailsComplete: AgentApplicationSoleTrader = afterContactDetailsComplete.copy(
      agentDetails = Some(dependencies.completeAgentDetails)
    ).assertDataIntegrity()

    val afterAmlsComplete: AgentApplicationSoleTrader = afterAgentDetailsComplete.copy(
      amlsDetails = Some(dependencies.completeAmlsDetails)
    ).assertDataIntegrity()

    val afterHmrcStandardForAgentsAgreed: AgentApplicationSoleTrader = afterAmlsComplete.copy(
      hmrcStandardForAgentsAgreed = StateOfAgreement.Agreed
    ).assertDataIntegrity()

    val afterDeclarationSubmitted: AgentApplicationSoleTrader = afterHmrcStandardForAgentsAgreed.copy(
      applicationState = ApplicationState.SentForRisking,
      submittedAt = Some(dependencies.nowAsInstant),
      applicationExpiresAt = None
    ).assertDataIntegrity()

    val soleTraderWithTrn: AgentApplicationSoleTrader = afterDeclarationSubmitted.copy(
      businessDetails = afterDeclarationSubmitted.businessDetails.map(_.copy(trn = Some(dependencies.trn)))
    ).assertDataIntegrity()

    val afterSentToMinerva: AgentApplicationSoleTrader = afterDeclarationSubmitted.copy(
      applicationState = ApplicationState.SentToMinerva
    ).assertDataIntegrity()

    val riskingOutcomeEntityFailedFixableAllSoleTraderCodes: AgentApplicationSoleTrader = afterSentToMinerva.copy(
      applicationState = ApplicationState.RiskingCompleted,
      riskingOutcomeApplication = Some(dependencies.riskingOutcomeApplication.failedFixable),
      riskingOutcomeEntity = Some(dependencies.riskingOutcomeEntityFailedFixableAllSoleTraderCodes)
    ).assertDataIntegrity()

    val riskingOutcomeEntitySoleTraderDuplicateCodes: AgentApplicationSoleTrader = afterSentToMinerva.copy(
      applicationState = ApplicationState.RiskingCompleted,
      riskingOutcomeApplication = Some(dependencies.riskingOutcomeApplication.failedFixable),
      riskingOutcomeEntity = Some(dependencies.riskingOutcomeEntitySoleTraderDuplicateFixes)
    ).assertDataIntegrity()

    val riskingOutcomeSoleTraderAmls: AgentApplicationSoleTrader = afterSentToMinerva.copy(
      applicationState = ApplicationState.RiskingCompleted,
      riskingOutcomeApplication = Some(dependencies.riskingOutcomeApplication.failedFixable),
      riskingOutcomeEntity = Some(dependencies.riskingOutcomeEntityFailedFixable(isFixed = None))
    ).assertDataIntegrity()

    val riskingOutcomeEntityFailedFixableNoEntityFailures: AgentApplicationSoleTrader = afterSentToMinerva.copy(
      applicationState = ApplicationState.RiskingCompleted,
      riskingOutcomeApplication = Some(dependencies.riskingOutcomeApplication.failedFixable),
      riskingOutcomeEntity = Some(RiskingOutcomeEntity.Approved)
    ).assertDataIntegrity()

}
