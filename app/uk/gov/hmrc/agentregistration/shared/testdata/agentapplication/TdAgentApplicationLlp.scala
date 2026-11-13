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
import uk.gov.hmrc.agentregistration.shared.BusinessType.Partnership.LimitedLiabilityPartnership
import uk.gov.hmrc.agentregistration.shared.agentdetails.*
import uk.gov.hmrc.agentregistration.shared.lists.FiveOrLessOfficers
import uk.gov.hmrc.agentregistration.shared.risking.EntityFailure
import uk.gov.hmrc.agentregistration.shared.risking.RiskingOutcomeEntity.Approved
import uk.gov.hmrc.agentregistration.shared.risking.submitforrisking.AgentDetailsData
import uk.gov.hmrc.agentregistration.shared.risking.submitforrisking.AmlsDetailsData
import uk.gov.hmrc.agentregistration.shared.risking.submitforrisking.ApplicantContactDetailsData
import uk.gov.hmrc.agentregistration.shared.risking.submitforrisking.ApplicationData
import uk.gov.hmrc.agentregistration.shared.testdata.TdBase
import uk.gov.hmrc.agentregistration.shared.testdata.TdTime
import uk.gov.hmrc.agentregistration.shared.testdata.TdGrsBusinessDetails
import uk.gov.hmrc.agentregistration.shared.testdata.agentapplication.DataIntegrityAssertion.assertDataIntegrity

trait TdAgentApplicationLlp { dependencies: (TdBase & TdTime & TdAgentApplicationTime & TdGrsBusinessDetails) =>

  object agentApplicationLlp:

    val afterStarted: AgentApplicationLlp = AgentApplicationLlp(
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

    val afterGrsDataReceived: AgentApplicationLlp = afterStarted.copy(
      businessDetails = Some(
        dependencies.grsBusinessDetails.llp.businessDetails
      ),
      applicationState = GrsDataReceived
    ).assertDataIntegrity()

    val afterRefusalToDealWithCheckPass: AgentApplicationLlp = afterGrsDataReceived.copy(
      refusalToDealWithCheckResult = Some(CheckResult.Pass)
    ).assertDataIntegrity()

    val afterRefusalToDealWithCheckFail: AgentApplicationLlp = afterGrsDataReceived.copy(
      refusalToDealWithCheckResult = Some(CheckResult.Fail)
    ).assertDataIntegrity()

    val afterUnifiedCustomerRegistryUpdateIdentifiers: AgentApplicationLlp = afterRefusalToDealWithCheckPass.copy(
      vrns = Some(List(dependencies.vrn)),
      payeRefs = Some(List(dependencies.payeRef))
    ).assertDataIntegrity()

    val afterUnifiedCustomerRegistryUpdateEmptyIdentifiers: AgentApplicationLlp = afterRefusalToDealWithCheckPass.copy(
      vrns = Some(List.empty),
      payeRefs = Some(List.empty)
    ).assertDataIntegrity()

    val afterGlobalAsaEnrolmentCheckPass: AgentApplicationLlp = afterUnifiedCustomerRegistryUpdateIdentifiers.copy(
      globalAsaEnrolmentCheckResult = Some(CheckResult.Pass)
    ).assertDataIntegrity()

    val afterGlobalAsaEnrolmentCheckFail: AgentApplicationLlp = afterUnifiedCustomerRegistryUpdateIdentifiers.copy(
      globalAsaEnrolmentCheckResult = Some(CheckResult.Fail)
    ).assertDataIntegrity()

    val afterContactDetailsComplete: AgentApplicationLlp = afterGlobalAsaEnrolmentCheckPass.copy(
      applicantContactDetails = Some(dependencies.applicantContactDetails)
    ).assertDataIntegrity()

    val afterAgentDetailsComplete: AgentApplicationLlp = afterContactDetailsComplete.copy(
      agentDetails = Some(dependencies.getCompleteAgentDetails(LimitedLiabilityPartnership))
    ).assertDataIntegrity()

    val afterAmlsComplete: AgentApplicationLlp = afterAgentDetailsComplete.copy(
      amlsDetails = Some(dependencies.completeAmlsDetails)
    ).assertDataIntegrity()

    val afterAmlsCompleteWithEvidence: AgentApplicationLlp = afterAmlsComplete.copy(
      amlsDetails = Some(dependencies.completeAmlsDetailsAtt)
    ).assertDataIntegrity()

    val afterHmrcStandardForAgentsAgreed: AgentApplicationLlp = afterAmlsComplete.copy(
      hmrcStandardForAgentsAgreed = StateOfAgreement.Agreed
    ).assertDataIntegrity()

    val afterZeroCompaniesHouseOfficers: AgentApplicationLlp = afterHmrcStandardForAgentsAgreed.copy(
      numberOfIndividuals = Some(
        FiveOrLessOfficers(
          numberOfCompaniesHouseOfficers = 0,
          isCompaniesHouseOfficersListCorrect = true
        )
      ),
      hasOtherRelevantIndividuals = Some(true)
    ).assertDataIntegrity()

    val afterConfirmCompaniesHouseOfficersYes: AgentApplicationLlp = afterHmrcStandardForAgentsAgreed.copy(
      numberOfIndividuals = Some(
        dependencies.fiveOrLessCompaniesHouseOfficers
      )
    ).assertDataIntegrity()

    val afterNumberOfConfirmCompaniesHouseOfficers: AgentApplicationLlp = afterHmrcStandardForAgentsAgreed.copy(
      numberOfIndividuals = Some(
        dependencies.sixOrMoreCompaniesHouseOfficers
      )
    ).assertDataIntegrity()

    val afterConfirmTwoChOfficers: AgentApplicationLlp = afterHmrcStandardForAgentsAgreed.copy(
      numberOfIndividuals = Some(
        dependencies.twoCompaniesHouseOfficers
      ),
      hasOtherRelevantIndividuals = Some(false)
    ).assertDataIntegrity()

    val afterConfirmSixChOfficers: AgentApplicationLlp = afterHmrcStandardForAgentsAgreed.copy(
      numberOfIndividuals = Some(
        dependencies.sixCompaniesHouseOfficersSelectAll
      ),
      hasOtherRelevantIndividuals = Some(false)
    ).assertDataIntegrity()

    val afterConfirmCompaniesHouseOfficersNo: AgentApplicationLlp = afterHmrcStandardForAgentsAgreed.copy(
      numberOfIndividuals = Some(
        dependencies.fiveOrLessCompaniesHouseOfficers.copy(isCompaniesHouseOfficersListCorrect = false)
      )
    ).assertDataIntegrity()

    val afterDeclarationSubmitted: AgentApplicationLlp = afterConfirmTwoChOfficers.copy(
      applicationState = ApplicationState.SentForRisking,
      submittedAt = Some(dependencies.submittedAt),
      applicationExpiresAt = None
    ).assertDataIntegrity()

    val afterSentToMinerva: AgentApplicationLlp = afterDeclarationSubmitted.copy(
      applicationState = ApplicationState.SentToMinerva
    ).assertDataIntegrity()

    val afterRiskingCompletedApproved: AgentApplicationLlp = afterSentToMinerva.copy(
      applicationState = ApplicationState.RiskingCompleted,
      riskingOutcomeApplication = Some(dependencies.riskingOutcomeApplication.approved),
      riskingOutcomeEntity = Some(Approved)
    ).assertDataIntegrity()

    val afterRiskingCompletedApprovedWithFixableIndividuals: AgentApplicationLlp = afterSentToMinerva.copy(
      applicationState = ApplicationState.RiskingCompleted,
      riskingOutcomeApplication = Some(dependencies.riskingOutcomeApplication.failedFixable),
      riskingOutcomeEntity = Some(Approved)
    ).assertDataIntegrity()

    def afterRiskingCompletedWithFixableAmls(failure: EntityFailure.IsAmls): AgentApplicationLlp = afterSentToMinerva.copy(
      applicationState = ApplicationState.RiskingCompleted,
      riskingOutcomeApplication = Some(dependencies.riskingOutcomeApplication.failedFixable),
      riskingOutcomeEntity = Some(dependencies.riskingOutcomeEntityFixableAmls(failure))
    ).assertDataIntegrity()

    val afterRiskingCompletedFixable: AgentApplicationLlp = afterSentToMinerva.copy(
      applicationState = ApplicationState.RiskingCompleted,
      riskingOutcomeApplication = Some(dependencies.riskingOutcomeApplication.failedFixable),
      riskingOutcomeEntity = Some(dependencies.riskingOutcomeEntityFailedFixable(isFixed = None))
    ).assertDataIntegrity()

    val afterRiskingCompletedFixableNonHmrcAmls: AgentApplicationLlp = afterSentToMinerva.copy(
      applicationState = ApplicationState.RiskingCompleted,
      riskingOutcomeApplication = Some(dependencies.riskingOutcomeApplication.failedFixable),
      riskingOutcomeEntity = Some(dependencies.riskingOutcomeEntityFixableNonHmrcAmls)
    ).assertDataIntegrity()

    val afterRiskingCompletedFixableAllCodes: AgentApplicationLlp = afterSentToMinerva.copy(
      applicationState = ApplicationState.RiskingCompleted,
      riskingOutcomeApplication = Some(dependencies.riskingOutcomeApplication.failedFixable),
      riskingOutcomeEntity = Some(dependencies.riskingOutcomeEntityFailedFixableAllCodes)
    ).assertDataIntegrity()

    val afterRiskingCompletedNonFixable: AgentApplicationLlp = afterSentToMinerva.copy(
      applicationState = ApplicationState.RiskingCompleted,
      riskingOutcomeApplication = Some(dependencies.riskingOutcomeApplication.failedNonFixable),
      riskingOutcomeEntity = Some(dependencies.riskingOutcomeEntityFailedNonFixable)
    ).assertDataIntegrity()

    val afterRiskingCompletedFixableFixed: AgentApplicationLlp = afterSentToMinerva.copy(
      applicationState = ApplicationState.RiskingCompleted,
      riskingOutcomeApplication = Some(dependencies.riskingOutcomeApplication.failedFixable),
      riskingOutcomeEntity = Some(dependencies.riskingOutcomeEntityFailedFixable(isFixed = Some(true)))
    ).assertDataIntegrity()

    val afterResubmitted: AgentApplicationLlp = afterRiskingCompletedFixableFixed.copy(
      applicationState = ApplicationState.SentForRisking,
      riskingOutcomeApplication = Some(dependencies.riskingOutcomeApplication.failedFixable.copy(reSubmittedAt = Some(dependencies.reSubmittedAt)))
    ).assertDataIntegrity()

    val afterResubmittedSentToMinerva: AgentApplicationLlp = afterResubmitted.copy(
      applicationState = ApplicationState.SentToMinerva
    ).assertDataIntegrity()

    val applicationData: ApplicationData =
      val a: AgentApplicationLlp = afterDeclarationSubmitted
      ApplicationData(
        applicationReference = dependencies.applicationReference,
        internalUserId = dependencies.internalUserId,
        applicantCredentials = dependencies.credentials,
        businessType = BusinessType.Partnership.LimitedLiabilityPartnership,
        groupId = dependencies.groupId,
        applicantContactDetails = ApplicantContactDetailsData(
          applicantName = dependencies.applicantName,
          telephoneNumber = dependencies.telephoneNumber,
          applicantEmailAddress = dependencies.applicantEmailAddress
        ),
        amlsDetails = AmlsDetailsData(
          supervisoryBody = dependencies.amlsCode,
          amlsRegistrationNumber = dependencies.amlsRegistrationNumber,
          amlsEvidence = None
        ),
        agentDetails = AgentDetailsData(
          businessName = dependencies.getAgentBusinessName(LimitedLiabilityPartnership),
          telephoneNumber = dependencies.agentTelephoneNumber,
          agentEmailAddress = dependencies.applicantEmailAddress,
          agentCorrespondenceAddress = dependencies.chroAddress
        ),
        vrns = List(dependencies.vrn),
        payeRefs = List(dependencies.payeRef),
        crn = Some(dependencies.crn),
        utr = a.getUtr,
        safeId = a.getSafeId,
        arn = None
      )

    /** Variant of [[afterDeclarationSubmitted]] with every optional agent field populated. Used by encryption tests to exercise paths like
      * `otherAgentBusinessName`, `otherAgentTelephoneNumber`, `otherAgentEmailAddress` which the default fixture leaves as `None`.
      */
    val afterDeclarationSubmittedWithAllOptionalFields: AgentApplicationLlp = afterDeclarationSubmitted.copy(
      agentDetails = Some(AgentDetails(
        businessName = AgentBusinessName(
          agentBusinessName = "Test LLP Trading Name",
          otherAgentBusinessName = Some("Other LLP Trading Name")
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

}
