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

import java.time.Clock
import java.time.Instant

/** Agent (Registration) Application. This case class represents the data entered by a user for registering as an agent.
  */
final case class AgentApplication(
  internalUserId: InternalUserId,
  createdAt: Instant,
  applicationState: ApplicationState,
  utr: Option[Utr],
  aboutYourApplication: AboutYourApplication,
  businessDetails: Option[BusinessDetails],
  amlsDetails: Option[AmlsDetails]
):

  /* derived stuff: */
  val lastUpdated: Instant = Instant.now(Clock.systemUTC())
  val hasFinished: Boolean =
    applicationState match
      case ApplicationState.Submitted => true
      case _ => false

  val isInProgress: Boolean = !hasFinished

  def getUtr(using request: RequestHeader): Utr = utr.getOrElse(
    throw RuntimeException(s"Expected 'utr' to be defined but it was None [${internalUserId.toString}] ")
  )

  def getBusinessType: BusinessType = aboutYourApplication.businessType.getOrElse(throw new RuntimeException("business type not defined"))

  def getUserRole: UserRole = aboutYourApplication.userRole.getOrElse(throw new RuntimeException("user role not defined"))

  def getBusinessDetails: BusinessDetails = businessDetails.getOrElse(throw new RuntimeException("business details not defined"))

  def getAmlsDetails: AmlsDetails = amlsDetails.getOrElse(throw new RuntimeException("AMLS details not defined"))

  def getApplicantName: String =
    getBusinessDetails match
      case sd: SoleTraderDetails => sd.fullName.toString
      case lcd: LimitedCompanyDetails => lcd.companyProfile.companyName
      case pd: PartnershipDetails => pd.companyProfile.get.companyName

object AgentApplication:
  given format: OFormat[AgentApplication] = Json.format[AgentApplication]
