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
import uk.gov.hmrc.agentregistration.shared.ApplicationState.GrsDataReceived
import uk.gov.hmrc.agentregistration.shared.risking.RiskingOutcomeEntity
import uk.gov.hmrc.agentregistration.shared.*
import uk.gov.hmrc.agentregistration.shared.agentdetails.*
import uk.gov.hmrc.agentregistration.shared.lists.FiveOrLessOfficers
import uk.gov.hmrc.agentregistration.shared.testdata.TdBase
import uk.gov.hmrc.agentregistration.shared.testdata.TdTime
import uk.gov.hmrc.agentregistration.shared.testdata.TdGrsBusinessDetails
import uk.gov.hmrc.agentregistration.shared.testdata.agentapplication.DataIntegrityAssertion.assertDataIntegrity

trait TdAgentApplicationLimitedCompany { dependencies: (TdBase & TdTime & TdAgentApplicationTime & TdGrsBusinessDetails) =>

  object agentApplicationLimitedCompany:

    val afterStarted: AgentApplicationLimitedCompany = AgentApplicationLimitedCompany(
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

    val afterGrsDataReceived: AgentApplicationLimitedCompany = afterStarted.copy(
      businessDetails = Some(
        dependencies.grsBusinessDetails.ltd.businessDetails
      ),
      applicationState = GrsDataReceived
    ).assertDataIntegrity()

    val afterRefusalToDealWithCheckPass: AgentApplicationLimitedCompany = afterGrsDataReceived.copy(
      refusalToDealWithCheckResult = Some(CheckResult.Pass)
    ).assertDataIntegrity()

    val afterUnifiedCustomerRegistryUpdateEmptyIdentifiers: AgentApplicationLimitedCompany = afterRefusalToDealWithCheckPass.copy(
      vrns = Some(List.empty),
      payeRefs = Some(List.empty)
    ).assertDataIntegrity()

    val afterRefusalToDealWithCheckFail: AgentApplicationLimitedCompany = afterGrsDataReceived.copy(
      refusalToDealWithCheckResult = Some(CheckResult.Fail)
    ).assertDataIntegrity()

    val afterUnifiedCustomerRegistryUpdateIdentifiers: AgentApplicationLimitedCompany = afterRefusalToDealWithCheckPass.copy(
      vrns = Some(List(dependencies.vrn)),
      payeRefs = Some(List(dependencies.payeRef))
    ).assertDataIntegrity()

    val afterGlobalAsaEnrolmentCheckPass: AgentApplicationLimitedCompany = afterUnifiedCustomerRegistryUpdateIdentifiers.copy(
      globalAsaEnrolmentCheckResult = Some(CheckResult.Pass)
    ).assertDataIntegrity()

    val afterGlobalAsaEnrolmentCheckFail: AgentApplicationLimitedCompany = afterUnifiedCustomerRegistryUpdateIdentifiers.copy(
      globalAsaEnrolmentCheckResult = Some(CheckResult.Fail)
    ).assertDataIntegrity()

    val afterContactDetailsComplete: AgentApplicationLimitedCompany = afterGlobalAsaEnrolmentCheckPass.copy(
      applicantContactDetails = Some(dependencies.applicantContactDetails)
    ).assertDataIntegrity()

    val afterAgentDetailsComplete: AgentApplicationLimitedCompany = afterContactDetailsComplete.copy(
      agentDetails = Some(dependencies.completeAgentDetails)
    ).assertDataIntegrity()

    val afterAmlsComplete: AgentApplicationLimitedCompany = afterAgentDetailsComplete.copy(
      amlsDetails = Some(dependencies.completeAmlsDetails)
    ).assertDataIntegrity()

    val afterHmrcStandardForAgentsAgreed: AgentApplicationLimitedCompany = afterAmlsComplete.copy(
      hmrcStandardForAgentsAgreed = StateOfAgreement.Agreed
    ).assertDataIntegrity()

    val afterZeroCompaniesHouseOfficers: AgentApplicationLimitedCompany = afterHmrcStandardForAgentsAgreed.copy(
      numberOfIndividuals = Some(
        FiveOrLessOfficers(
          numberOfCompaniesHouseOfficers = 0,
          isCompaniesHouseOfficersListCorrect = true
        )
      ),
      hasOtherRelevantIndividuals = Some(true)
    ).assertDataIntegrity()

    val afterConfirmCompaniesHouseOfficersYes: AgentApplicationLimitedCompany = afterHmrcStandardForAgentsAgreed.copy(
      numberOfIndividuals = Some(
        dependencies.twoCompaniesHouseOfficers
      )
    ).assertDataIntegrity()

    val afterNumberOfConfirmCompaniesHouseOfficers: AgentApplicationLimitedCompany = afterHmrcStandardForAgentsAgreed.copy(
      numberOfIndividuals = Some(
        dependencies.sixOrMoreCompaniesHouseOfficers
      )
    ).assertDataIntegrity()

    val afterConfirmTwoChOfficers: AgentApplicationLimitedCompany = afterHmrcStandardForAgentsAgreed.copy(
      numberOfIndividuals = Some(
        dependencies.twoCompaniesHouseOfficers
      ),
      hasOtherRelevantIndividuals = Some(false)
    ).assertDataIntegrity()

    val afterConfirmSixChOfficers: AgentApplicationLimitedCompany = afterHmrcStandardForAgentsAgreed.copy(
      numberOfIndividuals = Some(
        dependencies.sixCompaniesHouseOfficersSelectAll
      ),
      hasOtherRelevantIndividuals = Some(false)
    ).assertDataIntegrity()

    val afterConfirmCompaniesHouseOfficersNo: AgentApplicationLimitedCompany = afterHmrcStandardForAgentsAgreed.copy(
      numberOfIndividuals = Some(
        dependencies.twoCompaniesHouseOfficers.copy(isCompaniesHouseOfficersListCorrect = false)
      )
    ).assertDataIntegrity()

    val afterDeclarationSubmitted: AgentApplicationLimitedCompany = afterHmrcStandardForAgentsAgreed.copy(
      applicationState = ApplicationState.SentForRisking,
      submittedAt = Some(dependencies.submittedAt),
      applicationExpiresAt = None
    ).assertDataIntegrity()

    val afterDeclarationSubmittedWithAllOptionalFields: AgentApplicationLimitedCompany = afterDeclarationSubmitted.copy(
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

    val afterSentToMinerva: AgentApplicationLimitedCompany = afterDeclarationSubmitted.copy(
      applicationState = ApplicationState.SentToMinerva
    ).assertDataIntegrity()

    val afterRiskingCompletedApproved: AgentApplicationLimitedCompany = afterSentToMinerva.copy(
      applicationState = ApplicationState.RiskingCompleted,
      riskingOutcomeApplication = Some(dependencies.riskingOutcomeApplication.approved),
      riskingOutcomeEntity = Some(RiskingOutcomeEntity.Approved)
    ).assertDataIntegrity()

    val afterRiskingCompletedApprovedWithFixableIndividuals: AgentApplicationLimitedCompany = afterSentToMinerva.copy(
      applicationState = ApplicationState.RiskingCompleted,
      riskingOutcomeApplication = Some(dependencies.riskingOutcomeApplication.failedFixable),
      riskingOutcomeEntity = Some(RiskingOutcomeEntity.Approved)
    ).assertDataIntegrity()

    val afterRiskingCompletedFixable: AgentApplicationLimitedCompany = afterSentToMinerva.copy(
      applicationState = ApplicationState.RiskingCompleted,
      riskingOutcomeApplication = Some(dependencies.riskingOutcomeApplication.failedFixable),
      riskingOutcomeEntity = Some(dependencies.riskingOutcomeEntityFailedFixable(isFixed = None))
    ).assertDataIntegrity()

    val afterRiskingCompletedFixableNonHmrcAmls: AgentApplicationLimitedCompany = afterSentToMinerva.copy(
      applicationState = ApplicationState.RiskingCompleted,
      riskingOutcomeApplication = Some(dependencies.riskingOutcomeApplication.failedFixable),
      riskingOutcomeEntity = Some(dependencies.riskingOutcomeEntityFixableNonHmrcAmls)
    ).assertDataIntegrity()

    val afterRiskingCompletedFixableAllCodes: AgentApplicationLimitedCompany = afterSentToMinerva.copy(
      applicationState = ApplicationState.RiskingCompleted,
      riskingOutcomeApplication = Some(dependencies.riskingOutcomeApplication.failedFixable),
      riskingOutcomeEntity = Some(dependencies.riskingOutcomeEntityFailedFixableAllCodes)
    ).assertDataIntegrity()

    val afterRiskingCompletedNonFixable: AgentApplicationLimitedCompany = afterSentToMinerva.copy(
      applicationState = ApplicationState.RiskingCompleted,
      riskingOutcomeApplication = Some(dependencies.riskingOutcomeApplication.failedNonFixable),
      riskingOutcomeEntity = Some(dependencies.riskingOutcomeEntityFailedNonFixable)
    ).assertDataIntegrity()

    val afterRiskingCompletedFixableFixed: AgentApplicationLimitedCompany = afterSentToMinerva.copy(
      applicationState = ApplicationState.RiskingCompleted,
      riskingOutcomeApplication = Some(dependencies.riskingOutcomeApplication.failedFixable),
      riskingOutcomeEntity = Some(dependencies.riskingOutcomeEntityFailedFixable(isFixed = Some(true)))
    ).assertDataIntegrity()

    val afterResubmitted: AgentApplicationLimitedCompany = afterRiskingCompletedFixableFixed.copy(
      applicationState = ApplicationState.SentForRisking,
      riskingOutcomeApplication = Some(dependencies.riskingOutcomeApplication.failedFixable.copy(reSubmittedAt = Some(dependencies.reSubmittedAt)))
    ).assertDataIntegrity()

    val afterResubmittedSentToMinerva: AgentApplicationLimitedCompany = afterResubmitted.copy(
      applicationState = ApplicationState.SentToMinerva
    ).assertDataIntegrity()

}
