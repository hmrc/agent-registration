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
import uk.gov.hmrc.agentregistration.shared.agentdetails.*
import uk.gov.hmrc.agentregistration.shared.businessdetails.BusinessDetailsLlp
import uk.gov.hmrc.agentregistration.shared.businessdetails.CompanyProfile
import uk.gov.hmrc.agentregistration.shared.contactdetails.ApplicantContactDetails
import uk.gov.hmrc.agentregistration.shared.contactdetails.ApplicantEmailAddress
import uk.gov.hmrc.agentregistration.shared.contactdetails.ApplicantName
import uk.gov.hmrc.agentregistration.shared.lists.FiveOrLessOfficers
import uk.gov.hmrc.agentregistration.shared.lists.SixOrMoreOfficers
import uk.gov.hmrc.agentregistration.shared.testdata.TdBase
import uk.gov.hmrc.agentregistration.shared.testdata.TdGrsBusinessDetails

trait TdAgentApplicationLlp { dependencies: (TdBase & TdGrsBusinessDetails) =>

  object agentApplicationLlp:

    val afterStarted: AgentApplicationLlp = AgentApplicationLlp(
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
      companyStatusCheckResult = None,
      hmrcStandardForAgentsAgreed = StateOfAgreement.NotSet,
      numberOfIndividuals = None,
      hasOtherRelevantIndividuals = None,
      vrns = None,
      payeRefs = None
    )

    val afterGrsDataReceived: AgentApplicationLlp = afterStarted.copy(
      businessDetails = Some(
        dependencies.grsBusinessDetails.llp.businessDetails
      ),
      applicationState = GrsDataReceived
    )

    val afterRefusalToDealWithCheckPass: AgentApplicationLlp = afterGrsDataReceived.copy(
      refusalToDealWithCheckResult = Some(CheckResult.Pass)
    )

    val afterRefusalToDealWithCheckFail: AgentApplicationLlp = afterGrsDataReceived.copy(
      refusalToDealWithCheckResult = Some(CheckResult.Fail)
    )

    val afterCompaniesHouseStatusCheckPass: AgentApplicationLlp = afterRefusalToDealWithCheckPass.copy(
      companyStatusCheckResult = Some(CheckResult.Pass)
    )

    val afterCompaniesHouseStatusCheckFail: AgentApplicationLlp = afterRefusalToDealWithCheckPass.copy(
      companyStatusCheckResult = Some(CheckResult.Fail)
    )

    val afterUnifiedCustomerRegistryUpdateIdentifiers: AgentApplicationLlp = afterCompaniesHouseStatusCheckPass.copy(
      vrns = Some(List(dependencies.vrn)),
      payeRefs = Some(List(dependencies.payeRef))
    )

    val afterUnifiedCustomerRegistryUpdateEmptyIdentifiers: AgentApplicationLlp = afterCompaniesHouseStatusCheckPass.copy(
      vrns = Some(List.empty),
      payeRefs = Some(List.empty)
    )

    val afterContactDetailsComplete: AgentApplicationLlp = afterUnifiedCustomerRegistryUpdateIdentifiers.copy(
      applicantContactDetails = Some(dependencies.applicantContactDetails),
      agentDetails = None
    )

    val afterAgentDetailsComplete: AgentApplicationLlp = afterContactDetailsComplete.copy(
      agentDetails = Some(dependencies.completeAgentDetails)
    )

    val afterAmlsComplete: AgentApplicationLlp = afterAgentDetailsComplete.copy(
      amlsDetails = Some(dependencies.completeAmlsDetails)
    )

    val afterHmrcStandardForAgentsAgreed: AgentApplicationLlp = afterAmlsComplete.copy(
      hmrcStandardForAgentsAgreed = StateOfAgreement.Agreed
    )

    val afterConfirmCompaniesHouseOfficersYes: AgentApplicationLlp = afterHmrcStandardForAgentsAgreed.copy(
      numberOfIndividuals = Some(
        dependencies.fiveOrLessCompaniesHouseOfficers
      )
    )

    val afterNumberOfConfirmCompaniesHouseOfficers: AgentApplicationLlp = afterHmrcStandardForAgentsAgreed.copy(
      numberOfIndividuals = Some(
        dependencies.sixOrMoreCompaniesHouseOfficers
      )
    )

    val afterConfirmTwoChOfficers: AgentApplicationLlp = afterHmrcStandardForAgentsAgreed.copy(
      numberOfIndividuals = Some(
        dependencies.twoCompaniesHouseOfficers
      ),
      hasOtherRelevantIndividuals = Some(false)
    )

    val afterConfirmSixChOfficers: AgentApplicationLlp = afterHmrcStandardForAgentsAgreed.copy(
      numberOfIndividuals = Some(
        dependencies.sixCompaniesHouseOfficersSelectAll
      ),
      hasOtherRelevantIndividuals = Some(false)
    )

    val afterConfirmCompaniesHouseOfficersNo: AgentApplicationLlp = afterHmrcStandardForAgentsAgreed.copy(
      numberOfIndividuals = Some(
        dependencies.fiveOrLessCompaniesHouseOfficers.copy(isCompaniesHouseOfficersListCorrect = false)
      )
    )

    val afterConfirmOtherRelevantTaxAdvisersNo: AgentApplicationLlp = afterConfirmCompaniesHouseOfficersYes.copy(
      hasOtherRelevantIndividuals = Some(false)
    )

    val afterDeclarationSubmitted: AgentApplicationLlp = afterConfirmTwoChOfficers.copy(
      applicationState = ApplicationState.SentForRisking,
      submittedAt = Some(dependencies.nowAsInstant)
    )

    val afterSentForRisking: AgentApplicationLlp = afterDeclarationSubmitted.copy(
      userRole = Some(UserRole.Partner),
      businessDetails = Some(BusinessDetailsLlp(
        companyProfile = CompanyProfile(
          companyNumber = dependencies.crn,
          companyName = "Test LLP",
          dateOfIncorporation = Some(java.time.LocalDate.now()),
          unsanitisedCHROAddress = None
        ),
        saUtr = dependencies.saUtr,
        safeId = SafeId("AARN1234567")
      )),
      applicantContactDetails = Some(ApplicantContactDetails(
        applicantName = ApplicantName(dependencies.authorisedPersonName),
        telephoneNumber = Some(dependencies.telephoneNumber),
        applicantEmailAddress = Some(ApplicantEmailAddress(dependencies.applicantEmailAddress, isVerified = true))
      )),
      amlsDetails = Some(AmlsDetails(
        supervisoryBody = AmlsCode("HMRC"),
        amlsRegistrationNumber = Some(AmlsRegistrationNumber("XAML1234567890")),
        amlsEvidence = Some(uk.gov.hmrc.agentregistration.shared.amls.AmlsEvidence(
          uk.gov.hmrc.agentregistration.shared.upload.UploadId("evidence-reference-123"),
          "certificate.pdf",
          uk.gov.hmrc.objectstore.client.Path.File("/test.txt")
        ))
      )),
      agentDetails = Some(AgentDetails(
        businessName = AgentBusinessName(
          agentBusinessName = "Test LLP",
          otherAgentBusinessName = None
        ),
        telephoneNumber = Some(AgentTelephoneNumber(
          agentTelephoneNumber = dependencies.telephoneNumber.value,
          otherAgentTelephoneNumber = None
        )),
        agentEmailAddress = Some(AgentVerifiedEmailAddress(
          emailAddress = AgentEmailAddress(
            agentEmailAddress = dependencies.applicantEmailAddress.value,
            otherAgentEmailAddress = None
          ),
          isVerified = true
        )),
        agentCorrespondenceAddress = None
      )),
      companyStatusCheckResult = None,
      hasOtherRelevantIndividuals = Some(true),
      vrns = Some(List(Vrn("123456789"), Vrn("123456789"))),
      payeRefs = Some(List(PayeRef("123/AB12345"), PayeRef("123/AB12345")))
    )

}
