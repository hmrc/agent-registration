/*
 * Copyright 2026 HM Revenue & Customs
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

package uk.gov.hmrc.agentregistration.model

import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.agentregistration.shared.*
import uk.gov.hmrc.agentregistration.shared.agentdetails.AgentDetails
import uk.gov.hmrc.agentregistration.shared.businessdetails.BusinessDetailsSoleTrader
import uk.gov.hmrc.agentregistration.shared.contactdetails.ApplicantContactDetails
import uk.gov.hmrc.agentregistration.shared.individual.*
import uk.gov.hmrc.agentregistration.shared.lists.{IndividualName, NumberOfRequiredKeyIndividuals}

// TODO: Finalise what fields need to be returned here
final case class IndividualAddDetailsResponse(
  individualProvidedDetailsId: IndividualProvidedDetailsId,
  individualName: IndividualName, // supplied by applicant
  isPersonOfControl: Boolean, // is this a person of control e.g. partner, director etc.
  internalUserId: Option[InternalUserId],
  providedDetailsState: ProvidedDetailsState,
  individualDateOfBirth: Option[IndividualDateOfBirth] = None,
  telephoneNumber: Option[TelephoneNumber] = None,
  emailAddress: Option[IndividualVerifiedEmailAddress] = None,
  individualNino: Option[IndividualNino] = None,
  individualSaUtr: Option[IndividualSaUtr] = None,
  hmrcStandardForAgentsAgreed: StateOfAgreement = StateOfAgreement.NotSet,
  hasApprovedApplication: Option[Boolean] = None,
  linkId: LinkId,
  groupId: GroupId,
  applicationState: ApplicationState,
  businessType: BusinessType,
  userRole: Option[UserRole],
  applicantContactDetails: Option[ApplicantContactDetails],
  amlsDetails: Option[AmlsDetails],
  agentDetails: Option[AgentDetails],
  refusalToDealWithCheckResult: Option[CheckResult],
  numberOfRequiredKeyIndividuals: Option[NumberOfRequiredKeyIndividuals], // all applications require this, sole traders will have a list of one
  hasOtherRelevantIndividuals: Option[Boolean],
  businessDetails: Option[BusinessDetailsSoleTrader],
  deceasedCheckResult: Option[CheckResult],
  companyStatusCheckResult: Option[CheckResult]
)

object IndividualAddDetailsResponse:

  def from(
    ipd: IndividualProvidedDetails,
    aa: AgentApplication
  ): IndividualAddDetailsResponse = IndividualAddDetailsResponse(
    ipd.individualProvidedDetailsId,
    ipd.individualName,
    ipd.isPersonOfControl,
    ipd.internalUserId,
    ipd.providedDetailsState,
    ipd.individualDateOfBirth,
    ipd.telephoneNumber,
    ipd.emailAddress,
    ipd.individualNino,
    ipd.individualSaUtr,
    ipd.hmrcStandardForAgentsAgreed,
    ipd.hasApprovedApplication,
    aa.linkId,
    aa.groupId,
    aa.applicationState,
    aa.businessType,
    aa.userRole,
    aa.applicantContactDetails,
    aa.amlsDetails,
    aa.agentDetails,
    aa.refusalToDealWithCheckResult,
    aa.numberOfRequiredKeyIndividuals,
    aa.hasOtherRelevantIndividuals,
    businessDetails =
      aa match {
        case st: AgentApplicationSoleTrader => st.businessDetails
        case _ => None
      },
    deceasedCheckResult =
      aa match {
        case st: AgentApplicationSoleTrader => st.deceasedCheckResult
        case _ => None
      },
    companyStatusCheckResult =
      aa match {
        case lp: AgentApplicationLimitedPartnership => lp.companyStatusCheckResult
        case slp: AgentApplicationScottishLimitedPartnership => slp.companyStatusCheckResult
        case llp: AgentApplicationLlp => llp.companyStatusCheckResult
        case lc: AgentApplicationLimitedCompany => lc.companyStatusCheckResult
        case _ => None
      }
  )
  given format: OFormat[IndividualAddDetailsResponse] = Json.format[IndividualAddDetailsResponse]
