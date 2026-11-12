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

import uk.gov.hmrc.agentregistration.shared.EmailAddress
import uk.gov.hmrc.agentregistration.shared.*
import uk.gov.hmrc.agentregistration.shared.agentdetails.*
import uk.gov.hmrc.agentregistration.shared.risking.RiskingOutcomeEntity
import uk.gov.hmrc.agentregistration.shared.ApplicationState.GrsDataReceived
import uk.gov.hmrc.agentregistration.shared.BusinessType.Partnership.ScottishLimitedPartnership
import uk.gov.hmrc.agentregistration.shared.lists.FiveOrLessOfficers
import uk.gov.hmrc.agentregistration.shared.testdata.TdBase
import uk.gov.hmrc.agentregistration.shared.testdata.TdTime
import uk.gov.hmrc.agentregistration.shared.testdata.TdGrsBusinessDetails
import uk.gov.hmrc.agentregistration.shared.testdata.agentapplication.DataIntegrityAssertion.assertDataIntegrity

trait TdAgentApplicationScottishLimitedPartnership {
  dependencies: (TdBase & TdTime & TdAgentApplicationTime & TdGrsBusinessDetails) =>

  object agentApplicationScottishLimitedPartnership:

    val afterStarted: AgentApplicationScottishLimitedPartnership = AgentApplicationScottishLimitedPartnership(
      _id = dependencies.agentApplicationId,
      cachedSessionId = dependencies.cachedSessionId,
      applicationReference = dependencies.applicationReference,
      internalUserId = dependencies.internalUserId,
      applicantCredentials = dependencies.credentials,
      linkId = dependencies.linkId,
      groupId = dependencies.groupId,
      createdAt = dependencies.createdAt,
      applicationExpiresAt = Some(dependencies.applicationExpiresAt),
      submittedAt = None,
      postExpiryGracePeriodEndsAt = None,
      expiredAt = None,
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

    val afterGrsDataReceived: AgentApplicationScottishLimitedPartnership = afterStarted.copy(
      businessDetails = Some(
        dependencies.grsBusinessDetails.scottishLtdPartnership.businessDetails
      ),
      applicationState = GrsDataReceived
    ).assertDataIntegrity()

    val afterRefusalToDealWithCheckPass: AgentApplicationScottishLimitedPartnership = afterGrsDataReceived.copy(
      refusalToDealWithCheckResult = Some(CheckResult.Pass)
    ).assertDataIntegrity()

    val afterUnifiedCustomerRegistryUpdateEmptyIdentifiers: AgentApplicationScottishLimitedPartnership = afterRefusalToDealWithCheckPass.copy(
      vrns = Some(List.empty),
      payeRefs = Some(List.empty)
    ).assertDataIntegrity()

    val afterRefusalToDealWithCheckFail: AgentApplicationScottishLimitedPartnership = afterGrsDataReceived.copy(
      refusalToDealWithCheckResult = Some(CheckResult.Fail)
    ).assertDataIntegrity()

    val afterUnifiedCustomerRegistryUpdateIdentifiers: AgentApplicationScottishLimitedPartnership = afterRefusalToDealWithCheckPass.copy(
      vrns = Some(List(dependencies.vrn)),
      payeRefs = Some(List(dependencies.payeRef))
    ).assertDataIntegrity()

    val afterGlobalAsaEnrolmentCheckPass: AgentApplicationScottishLimitedPartnership = afterUnifiedCustomerRegistryUpdateIdentifiers.copy(
      globalAsaEnrolmentCheckResult = Some(CheckResult.Pass)
    ).assertDataIntegrity()

    val afterGlobalAsaEnrolmentCheckFail: AgentApplicationScottishLimitedPartnership = afterUnifiedCustomerRegistryUpdateIdentifiers.copy(
      globalAsaEnrolmentCheckResult = Some(CheckResult.Fail)
    ).assertDataIntegrity()

    val afterContactDetailsComplete: AgentApplicationScottishLimitedPartnership = afterGlobalAsaEnrolmentCheckPass.copy(
      applicantContactDetails = Some(dependencies.applicantContactDetails)
    ).assertDataIntegrity()

    val afterAgentDetailsComplete: AgentApplicationScottishLimitedPartnership = afterContactDetailsComplete.copy(
      agentDetails = Some(dependencies.getCompleteAgentDetails(ScottishLimitedPartnership))
    ).assertDataIntegrity()

    val afterAmlsComplete: AgentApplicationScottishLimitedPartnership = afterAgentDetailsComplete.copy(
      amlsDetails = Some(dependencies.completeAmlsDetails)
    ).assertDataIntegrity()

    val afterHmrcStandardForAgentsAgreed: AgentApplicationScottishLimitedPartnership = afterAmlsComplete.copy(
      hmrcStandardForAgentsAgreed = StateOfAgreement.Agreed
    ).assertDataIntegrity()

    val afterZeroCompaniesHouseOfficers: AgentApplicationScottishLimitedPartnership = afterHmrcStandardForAgentsAgreed.copy(
      numberOfIndividuals = Some(
        FiveOrLessOfficers(
          numberOfCompaniesHouseOfficers = 0,
          isCompaniesHouseOfficersListCorrect = true
        )
      ),
      hasOtherRelevantIndividuals = Some(true)
    ).assertDataIntegrity()

    val afterConfirmCompaniesHouseOfficersYes: AgentApplicationScottishLimitedPartnership = afterHmrcStandardForAgentsAgreed.copy(
      numberOfIndividuals = Some(
        dependencies.twoCompaniesHouseOfficers
      )
    ).assertDataIntegrity()

    val afterNumberOfConfirmCompaniesHouseOfficers: AgentApplicationScottishLimitedPartnership = afterHmrcStandardForAgentsAgreed.copy(
      numberOfIndividuals = Some(
        dependencies.sixOrMoreCompaniesHouseOfficers
      )
    ).assertDataIntegrity()

    val afterConfirmCompaniesHouseOfficersNo: AgentApplicationScottishLimitedPartnership = afterHmrcStandardForAgentsAgreed.copy(
      numberOfIndividuals = Some(
        dependencies.twoCompaniesHouseOfficers.copy(isCompaniesHouseOfficersListCorrect = false)
      )
    ).assertDataIntegrity()

    val afterConfirmTwoChOfficers: AgentApplicationScottishLimitedPartnership = afterHmrcStandardForAgentsAgreed.copy(
      numberOfIndividuals = Some(
        dependencies.twoCompaniesHouseOfficers
      ),
      hasOtherRelevantIndividuals = Some(false)
    ).assertDataIntegrity()

    val afterConfirmSixChOfficers: AgentApplicationScottishLimitedPartnership = afterHmrcStandardForAgentsAgreed.copy(
      numberOfIndividuals = Some(
        dependencies.sixCompaniesHouseOfficersSelectAll
      ),
      hasOtherRelevantIndividuals = Some(false)
    ).assertDataIntegrity()

    val afterDeclarationSubmitted: AgentApplicationScottishLimitedPartnership = afterHmrcStandardForAgentsAgreed.copy(
      applicationState = ApplicationState.SentForRisking,
      submittedAt = Some(dependencies.submittedAt),
      applicationExpiresAt = None
    ).assertDataIntegrity()

    val afterDeclarationSubmittedWithAllOptionalFields: AgentApplicationScottishLimitedPartnership = afterDeclarationSubmitted.copy(
      agentDetails = Some(AgentDetails(
        businessName = AgentBusinessName(
          agentBusinessName = "Test Trading Name",
          otherAgentBusinessName = Some("Other Trading Name")
        ),
        telephoneNumber = Some(AgentTelephoneNumber(
          agentTelephoneNumber = dependencies.telephoneNumber.value,
          otherAgentTelephoneNumber = Some("+44 1234 567890")
        )),
        agentEmailAddress = Some(AgentVerifiedEmailAddress(
          emailAddress = AgentEmailAddress(
            agentEmailAddress = dependencies.applicantEmailAddress,
            otherAgentEmailAddress = Some(EmailAddress("other.address@example.com"))
          ),
          isVerified = true
        )),
        agentCorrespondenceAddress = Some(dependencies.chroAddress)
      ))
    ).assertDataIntegrity()

    val afterSentToMinerva: AgentApplicationScottishLimitedPartnership = afterDeclarationSubmitted.copy(
      applicationState = ApplicationState.SentToMinerva
    ).assertDataIntegrity()

    val afterRiskingCompletedApproved: AgentApplicationScottishLimitedPartnership = afterSentToMinerva.copy(
      applicationState = ApplicationState.RiskingCompleted,
      riskingOutcomeApplication = Some(dependencies.riskingOutcomeApplication.approved),
      riskingOutcomeEntity = Some(RiskingOutcomeEntity.Approved)
    ).assertDataIntegrity()

    val afterRiskingCompletedApprovedWithFixableIndividuals: AgentApplicationScottishLimitedPartnership = afterSentToMinerva.copy(
      applicationState = ApplicationState.RiskingCompleted,
      riskingOutcomeApplication = Some(dependencies.riskingOutcomeApplication.failedFixable),
      riskingOutcomeEntity = Some(RiskingOutcomeEntity.Approved)
    ).assertDataIntegrity()

    val afterRiskingCompletedFixable: AgentApplicationScottishLimitedPartnership = afterSentToMinerva.copy(
      applicationState = ApplicationState.RiskingCompleted,
      riskingOutcomeApplication = Some(dependencies.riskingOutcomeApplication.failedFixable),
      riskingOutcomeEntity = Some(dependencies.riskingOutcomeEntityFailedFixable(isFixed = None))
    ).assertDataIntegrity()

    val afterRiskingCompletedFixableNonHmrcAmls: AgentApplicationScottishLimitedPartnership = afterSentToMinerva.copy(
      applicationState = ApplicationState.RiskingCompleted,
      riskingOutcomeApplication = Some(dependencies.riskingOutcomeApplication.failedFixable),
      riskingOutcomeEntity = Some(dependencies.riskingOutcomeEntityFixableNonHmrcAmls)
    ).assertDataIntegrity()

    val afterRiskingCompletedFixableAllCodes: AgentApplicationScottishLimitedPartnership = afterSentToMinerva.copy(
      applicationState = ApplicationState.RiskingCompleted,
      riskingOutcomeApplication = Some(dependencies.riskingOutcomeApplication.failedFixable),
      riskingOutcomeEntity = Some(dependencies.riskingOutcomeEntityFailedFixableAllCodes)
    ).assertDataIntegrity()

    val afterRiskingCompletedNonFixable: AgentApplicationScottishLimitedPartnership = afterSentToMinerva.copy(
      applicationState = ApplicationState.RiskingCompleted,
      riskingOutcomeApplication = Some(dependencies.riskingOutcomeApplication.failedNonFixable),
      riskingOutcomeEntity = Some(dependencies.riskingOutcomeEntityFailedNonFixable)
    ).assertDataIntegrity()

    val afterRiskingCompletedFixableFixed: AgentApplicationScottishLimitedPartnership = afterSentToMinerva.copy(
      applicationState = ApplicationState.RiskingCompleted,
      riskingOutcomeApplication = Some(dependencies.riskingOutcomeApplication.failedFixable),
      riskingOutcomeEntity = Some(dependencies.riskingOutcomeEntityFailedFixable(isFixed = Some(true)))
    ).assertDataIntegrity()

    val afterResubmitted: AgentApplicationScottishLimitedPartnership = afterRiskingCompletedFixableFixed.copy(
      applicationState = ApplicationState.SentForRisking,
      riskingOutcomeApplication = Some(dependencies.riskingOutcomeApplication.failedFixable.copy(reSubmittedAt = Some(dependencies.reSubmittedAt)))
    ).assertDataIntegrity()

    val afterResubmittedSentToMinerva: AgentApplicationScottishLimitedPartnership = afterResubmitted.copy(
      applicationState = ApplicationState.SentToMinerva
    ).assertDataIntegrity()

}
