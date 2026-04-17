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
import uk.gov.hmrc.agentregistration.shared.lists.FiveOrLessOfficers
import uk.gov.hmrc.agentregistration.shared.lists.SixOrMoreOfficers
import uk.gov.hmrc.agentregistration.shared.testdata.TdBase
import uk.gov.hmrc.agentregistration.shared.testdata.TdGrsBusinessDetails

trait TdAgentApplicationLimitedPartnership {
  dependencies: (TdBase & TdGrsBusinessDetails) =>

  object agentApplicationLimitedPartnership:

    val afterStarted: AgentApplicationLimitedPartnership = AgentApplicationLimitedPartnership(
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
      companyStatusCheckResult = None,
      hmrcStandardForAgentsAgreed = StateOfAgreement.NotSet,
      numberOfIndividuals = None,
      hasOtherRelevantIndividuals = None,
      vrns = None,
      payeRefs = None
    )

    val afterGrsDataReceived: AgentApplicationLimitedPartnership = afterStarted.copy(
      businessDetails = Some(
        dependencies.grsBusinessDetails.ltdPartnership.businessDetails
      ),
      applicationState = GrsDataReceived
    )

    val afterRefusalToDealWithCheckPass: AgentApplicationLimitedPartnership = afterGrsDataReceived.copy(
      refusalToDealWithCheckResult = Some(CheckResult.Pass)
    )

    val afterRefusalToDealWithCheckFail: AgentApplicationLimitedPartnership = afterGrsDataReceived.copy(
      refusalToDealWithCheckResult = Some(CheckResult.Fail)
    )

    val afterCompaniesHouseStatusCheckPass: AgentApplicationLimitedPartnership = afterRefusalToDealWithCheckPass.copy(
      companyStatusCheckResult = Some(CheckResult.Pass)
    )

    val afterCompaniesHouseStatusCheckFail: AgentApplicationLimitedPartnership = afterRefusalToDealWithCheckPass.copy(
      companyStatusCheckResult = Some(CheckResult.Fail)
    )

    val afterContactDetailsComplete: AgentApplicationLimitedPartnership = afterCompaniesHouseStatusCheckPass.copy(
      applicantContactDetails = Some(dependencies.applicantContactDetails),
      agentDetails = None
    )

    val afterAgentDetailsComplete: AgentApplicationLimitedPartnership = afterContactDetailsComplete.copy(
      agentDetails = Some(dependencies.completeAgentDetails)
    )

    val afterAmlsComplete: AgentApplicationLimitedPartnership = afterAgentDetailsComplete.copy(
      amlsDetails = Some(dependencies.completeAmlsDetails)
    )

    val afterHmrcStandardForAgentsAgreed: AgentApplicationLimitedPartnership = afterAmlsComplete.copy(
      hmrcStandardForAgentsAgreed = StateOfAgreement.Agreed
    )

    val afterConfirmCompaniesHouseOfficersYes: AgentApplicationLimitedPartnership = afterHmrcStandardForAgentsAgreed.copy(
      numberOfIndividuals = Some(
        dependencies.twoCompaniesHouseOfficers
      )
    )

    val afterNumberOfConfirmCompaniesHouseOfficers: AgentApplicationLimitedPartnership = afterHmrcStandardForAgentsAgreed.copy(
      numberOfIndividuals = Some(
        dependencies.sixOrMoreCompaniesHouseOfficers
      )
    )

    val afterConfirmCompaniesHouseOfficersNo: AgentApplicationLimitedPartnership = afterHmrcStandardForAgentsAgreed.copy(
      numberOfIndividuals = Some(
        dependencies.twoCompaniesHouseOfficers.copy(isCompaniesHouseOfficersListCorrect = false)
      )
    )

    val afterConfirmTwoChOfficers: AgentApplicationLimitedPartnership = afterHmrcStandardForAgentsAgreed.copy(
      numberOfIndividuals = Some(
        dependencies.twoCompaniesHouseOfficers
      ),
      hasOtherRelevantIndividuals = Some(false)
    )

    val afterConfirmSixChOfficers: AgentApplicationLimitedPartnership = afterHmrcStandardForAgentsAgreed.copy(
      numberOfIndividuals = Some(
        dependencies.sixCompaniesHouseOfficersSelectAll
      ),
      hasOtherRelevantIndividuals = Some(false)
    )

    val afterDeclarationSubmitted: AgentApplicationLimitedPartnership = afterHmrcStandardForAgentsAgreed.copy(
      applicationState = ApplicationState.SentForRisking,
      submittedAt = Some(dependencies.nowAsInstant)
    )

}
