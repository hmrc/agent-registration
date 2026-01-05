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

package uk.gov.hmrc.agentregistration.shared

import play.api.libs.json.Json
import play.api.libs.json.OFormat
import play.api.mvc.RequestHeader
import uk.gov.hmrc.agentregistration.shared.contactdetails.ApplicantContactDetails
import uk.gov.hmrc.agentregistration.shared.util.Errors.getOrThrowExpectedDataMissing

import java.time.Clock
import java.time.Instant

/** Agent (Registration) Application. This case class represents the data entered by a user for registering as an agent.
  */
final case class AgentApplicationOld(
  internalUserId: InternalUserId,
  createdAt: Instant,
  applicationState: ApplicationState,
  utr: Option[Utr],
  businessDetails: Option[BusinessDetails],
  applicantContactDetails: Option[ApplicantContactDetails],
  amlsDetails: Option[AmlsDetails]
):

  /* derived stuff: */
  val lastUpdated: Instant = Instant.now(Clock.systemUTC())
  val hasFinished: Boolean =
    applicationState match
      case ApplicationState.Submitted => true
      case _ => false

  val isInProgress: Boolean = !hasFinished

  def getUtr(using request: RequestHeader): Utr = utr
    .getOrThrowExpectedDataMissing(s"Expected 'utr' to be defined but it was None [${internalUserId.toString}] ")

  def getBusinessDetails: BusinessDetails = businessDetails
    .getOrThrowExpectedDataMissing("business details not defined")

  def getApplicantContactDetails: ApplicantContactDetails = applicantContactDetails
    .getOrThrowExpectedDataMissing("applicant contact details not defined")

  def getAmlsDetails: AmlsDetails = amlsDetails.getOrThrowExpectedDataMissing("AMLS details not defined")

  def getApplicantBusinessName: String =
    getBusinessDetails match
      case sd: SoleTraderDetails => sd.fullName.toString
      case lcd: LimitedCompanyDetails => lcd.companyProfile.companyName
      // not sure why partnership name as optional but if there's no name return empty string
      // until we have a requirement to make it mandatory and throw an exception
      case pd: PartnershipDetails => pd.companyProfile.fold("")(_.companyName)

  def getCompanyRegistrationNumber: Crn =
    getBusinessDetails match
      case lcd: LimitedCompanyDetails => lcd.companyProfile.companyNumber
      case pd: PartnershipDetails =>
        pd.companyProfile
          .map(_.companyNumber)
          .getOrThrowExpectedDataMissing("company registration number not defined")
      case _: SoleTraderDetails => throw new RuntimeException("company registration number not applicable for sole traders")

object AgentApplicationOld:
  given format: OFormat[AgentApplicationOld] = Json.format[AgentApplicationOld]
