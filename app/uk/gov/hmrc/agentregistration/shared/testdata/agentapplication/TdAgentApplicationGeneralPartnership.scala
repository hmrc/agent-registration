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
import uk.gov.hmrc.agentregistration.shared.agentdetails.*
import uk.gov.hmrc.agentregistration.shared.AgentApplicationGeneralPartnership
import uk.gov.hmrc.agentregistration.shared.risking.RiskingOutcomeEntity
import uk.gov.hmrc.agentregistration.shared.ApplicationState
import uk.gov.hmrc.agentregistration.shared.ApplicationState.GrsDataReceived
import uk.gov.hmrc.agentregistration.shared.BusinessType.Partnership.GeneralPartnership
import uk.gov.hmrc.agentregistration.shared.CheckResult
import uk.gov.hmrc.agentregistration.shared.StateOfAgreement
import uk.gov.hmrc.agentregistration.shared.UserRole
import uk.gov.hmrc.agentregistration.shared.lists.FiveOrLess
import uk.gov.hmrc.agentregistration.shared.lists.SixOrMore
import uk.gov.hmrc.agentregistration.shared.testdata.TdBase
import uk.gov.hmrc.agentregistration.shared.testdata.TdTime
import uk.gov.hmrc.agentregistration.shared.testdata.TdGrsBusinessDetails
import uk.gov.hmrc.agentregistration.shared.testdata.agentapplication.DataIntegrityAssertion.assertDataIntegrity

trait TdAgentApplicationGeneralPartnership { dependencies: (TdBase & TdTime & TdAgentApplicationTime & TdGrsBusinessDetails) =>

  object agentApplicationGeneralPartnership:

    val afterStarted: AgentApplicationGeneralPartnership = AgentApplicationGeneralPartnership(
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

    val afterGrsDataReceived: AgentApplicationGeneralPartnership = afterStarted.copy(
      businessDetails = Some(
        dependencies.grsBusinessDetails.generalPartnership.businessDetails
      ),
      applicationState = GrsDataReceived
    ).assertDataIntegrity()

    val afterRefusalToDealWithCheckPass: AgentApplicationGeneralPartnership = afterGrsDataReceived.copy(
      refusalToDealWithCheckResult = Some(CheckResult.Pass)
    ).assertDataIntegrity()

    val afterUnifiedCustomerRegistryUpdateEmptyIdentifiers: AgentApplicationGeneralPartnership = afterRefusalToDealWithCheckPass.copy(
      vrns = Some(List.empty),
      payeRefs = Some(List.empty)
    ).assertDataIntegrity()

    val afterRefusalToDealWithCheckFail: AgentApplicationGeneralPartnership = afterGrsDataReceived.copy(
      refusalToDealWithCheckResult = Some(CheckResult.Fail)
    ).assertDataIntegrity()

    val afterUnifiedCustomerRegistryUpdateIdentifiers: AgentApplicationGeneralPartnership = afterRefusalToDealWithCheckPass.copy(
      vrns = Some(List(dependencies.vrn)),
      payeRefs = Some(List(dependencies.payeRef))
    ).assertDataIntegrity()

    val afterGlobalAsaEnrolmentCheckPass: AgentApplicationGeneralPartnership = afterUnifiedCustomerRegistryUpdateIdentifiers.copy(
      globalAsaEnrolmentCheckResult = Some(CheckResult.Pass)
    ).assertDataIntegrity()

    val afterGlobalAsaEnrolmentCheckFail: AgentApplicationGeneralPartnership = afterUnifiedCustomerRegistryUpdateIdentifiers.copy(
      globalAsaEnrolmentCheckResult = Some(CheckResult.Fail)
    ).assertDataIntegrity()

    val afterContactDetailsComplete: AgentApplicationGeneralPartnership = afterGlobalAsaEnrolmentCheckPass.copy(
      applicantContactDetails = Some(dependencies.applicantContactDetails)
    ).assertDataIntegrity()

    val afterAgentDetailsComplete: AgentApplicationGeneralPartnership = afterContactDetailsComplete.copy(
      agentDetails = Some(dependencies.getCompleteAgentDetails(GeneralPartnership))
    ).assertDataIntegrity()

    val afterAmlsComplete: AgentApplicationGeneralPartnership = afterAgentDetailsComplete.copy(
      amlsDetails = Some(dependencies.completeAmlsDetails)
    ).assertDataIntegrity()

    val afterHmrcStandardForAgentsAgreed: AgentApplicationGeneralPartnership = afterAmlsComplete.copy(
      hmrcStandardForAgentsAgreed = StateOfAgreement.Agreed
    ).assertDataIntegrity()

    val afterHowManyKeyIndividuals: AgentApplicationGeneralPartnership = afterHmrcStandardForAgentsAgreed.copy(
      numberOfIndividuals = Some(
        FiveOrLess(
          numberOfKeyIndividuals = 3
        )
      )
    ).assertDataIntegrity()

    val afterSixOrMoreChosen: AgentApplicationGeneralPartnership = afterHmrcStandardForAgentsAgreed.copy(
      numberOfIndividuals = Some(SixOrMore())
    ).assertDataIntegrity()

    val afterSixOrMorePopulated: AgentApplicationGeneralPartnership = afterHmrcStandardForAgentsAgreed.copy(
      numberOfIndividuals = Some(
        SixOrMore(
          numberOfKeyIndividualsResponsibleForTaxMatters = Some(3)
        )
      )
    ).assertDataIntegrity()

    val afterZeroKeyIndividuals: AgentApplicationGeneralPartnership = afterHmrcStandardForAgentsAgreed.copy(
      numberOfIndividuals = Some(
        FiveOrLess(
          numberOfKeyIndividuals = 0
        )
      ),
      hasOtherRelevantIndividuals = Some(true)
    ).assertDataIntegrity()

    val afterOnlyOneKeyIndividual: AgentApplicationGeneralPartnership = afterHmrcStandardForAgentsAgreed.copy(
      numberOfIndividuals = Some(
        FiveOrLess(
          numberOfKeyIndividuals = 1
        )
      )
    ).assertDataIntegrity()

    val afterHowManyKeyIndividualsNeedsNoPadding: AgentApplicationGeneralPartnership = afterHmrcStandardForAgentsAgreed.copy(
      numberOfIndividuals = Some(
        SixOrMore(
          numberOfKeyIndividualsResponsibleForTaxMatters = Some(6)
        )
      )
    ).assertDataIntegrity()

    // when the number of key individuals is of type SixOrMore and padding is required because the number of
    // key individuals responsible for tax matters is less than minimum list size(5)
    val afterHowManyKeyIndividualsNeedsPadding: AgentApplicationGeneralPartnership = afterHmrcStandardForAgentsAgreed.copy(
      numberOfIndividuals = Some(dependencies.sixOrMoreKeyIndividuals)
    ).assertDataIntegrity()

    val afterConfirmOtherRelevantIndividualsYes: AgentApplicationGeneralPartnership = afterHowManyKeyIndividuals
      .copy(
        hasOtherRelevantIndividuals = Some(true)
      ).assertDataIntegrity()

    val afterConfirmOtherRelevantIndividualsNo: AgentApplicationGeneralPartnership = afterHowManyKeyIndividuals
      .copy(
        hasOtherRelevantIndividuals = Some(false)
      ).assertDataIntegrity()

    val afterConfirmTwoOtherRelevantIndividualsNo: AgentApplicationGeneralPartnership = afterHowManyKeyIndividuals
      .copy(
        numberOfIndividuals = Some(
          SixOrMore(
            numberOfKeyIndividualsResponsibleForTaxMatters = Some(2)
          )
        ),
        hasOtherRelevantIndividuals = Some(false)
      ).assertDataIntegrity()

    val afterConfirmTwoIndividuals: AgentApplicationGeneralPartnership = afterHowManyKeyIndividuals
      .copy(
        numberOfIndividuals = Some(
          FiveOrLess(
            numberOfKeyIndividuals = 2
          )
        ),
        hasOtherRelevantIndividuals = Some(false)
      ).assertDataIntegrity()

    val afterConfirmSixIndividuals: AgentApplicationGeneralPartnership = afterHowManyKeyIndividuals
      .copy(
        numberOfIndividuals = Some(
          SixOrMore(
            numberOfKeyIndividualsResponsibleForTaxMatters = Some(6)
          )
        ),
        hasOtherRelevantIndividuals = Some(false)
      ).assertDataIntegrity()

    val afterDeclarationSubmitted: AgentApplicationGeneralPartnership = afterHmrcStandardForAgentsAgreed.copy(
      applicationState = ApplicationState.SentForRisking,
      submittedAt = Some(dependencies.submittedAt),
      applicationExpiresAt = None
    ).assertDataIntegrity()

    val afterDeclarationSubmittedWithAllOptionalFields: AgentApplicationGeneralPartnership = afterDeclarationSubmitted.copy(
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

    val afterSentToMinerva: AgentApplicationGeneralPartnership = afterDeclarationSubmitted.copy(
      applicationState = ApplicationState.SentToMinerva
    ).assertDataIntegrity()

    val afterRiskingCompletedApproved: AgentApplicationGeneralPartnership = afterSentToMinerva.copy(
      applicationState = ApplicationState.RiskingCompleted,
      riskingOutcomeApplication = Some(dependencies.riskingOutcomeApplication.approved),
      riskingOutcomeEntity = Some(RiskingOutcomeEntity.Approved)
    ).assertDataIntegrity()

    val afterRiskingCompletedApprovedWithFixableIndividuals: AgentApplicationGeneralPartnership = afterSentToMinerva.copy(
      applicationState = ApplicationState.RiskingCompleted,
      riskingOutcomeApplication = Some(dependencies.riskingOutcomeApplication.failedFixable),
      riskingOutcomeEntity = Some(RiskingOutcomeEntity.Approved)
    ).assertDataIntegrity()

    val afterRiskingCompletedFixable: AgentApplicationGeneralPartnership = afterSentToMinerva.copy(
      applicationState = ApplicationState.RiskingCompleted,
      riskingOutcomeApplication = Some(dependencies.riskingOutcomeApplication.failedFixable),
      riskingOutcomeEntity = Some(dependencies.riskingOutcomeEntityFailedFixable(isFixed = None))
    ).assertDataIntegrity()

    val afterRiskingCompletedFixableNonHmrcAmls: AgentApplicationGeneralPartnership = afterSentToMinerva.copy(
      applicationState = ApplicationState.RiskingCompleted,
      riskingOutcomeApplication = Some(dependencies.riskingOutcomeApplication.failedFixable),
      riskingOutcomeEntity = Some(dependencies.riskingOutcomeEntityFixableNonHmrcAmls)
    ).assertDataIntegrity()

    val afterRiskingCompletedFixableAllCodes: AgentApplicationGeneralPartnership = afterSentToMinerva.copy(
      applicationState = ApplicationState.RiskingCompleted,
      riskingOutcomeApplication = Some(dependencies.riskingOutcomeApplication.failedFixable),
      riskingOutcomeEntity = Some(dependencies.riskingOutcomeEntityFailedFixableAllCodes)
    ).assertDataIntegrity()

    val afterRiskingCompletedNonFixable: AgentApplicationGeneralPartnership = afterSentToMinerva.copy(
      applicationState = ApplicationState.RiskingCompleted,
      riskingOutcomeApplication = Some(dependencies.riskingOutcomeApplication.failedNonFixable),
      riskingOutcomeEntity = Some(dependencies.riskingOutcomeEntityFailedNonFixable)
    ).assertDataIntegrity()

    val afterRiskingCompletedFixableFixed: AgentApplicationGeneralPartnership = afterSentToMinerva.copy(
      applicationState = ApplicationState.RiskingCompleted,
      riskingOutcomeApplication = Some(dependencies.riskingOutcomeApplication.failedFixable),
      riskingOutcomeEntity = Some(dependencies.riskingOutcomeEntityFailedFixable(isFixed = Some(true)))
    ).assertDataIntegrity()

    val afterResubmitted: AgentApplicationGeneralPartnership = afterRiskingCompletedFixableFixed.copy(
      applicationState = ApplicationState.SentForRisking,
      riskingOutcomeApplication = Some(dependencies.riskingOutcomeApplication.failedFixable.copy(reSubmittedAt = Some(dependencies.reSubmittedAt)))
    ).assertDataIntegrity()

    val afterResubmittedSentToMinerva: AgentApplicationGeneralPartnership = afterResubmitted.copy(
      applicationState = ApplicationState.SentToMinerva
    ).assertDataIntegrity()

    val afterDeclarationSubmittedAndTwoIndividualFinished: AgentApplicationGeneralPartnership = afterConfirmTwoIndividuals.copy(
      applicationState = ApplicationState.SentForRisking,
      submittedAt = Some(dependencies.submittedAt),
      applicationExpiresAt = None
    ).assertDataIntegrity()

}
