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
import play.api.libs.json.JsonConfiguration
import play.api.libs.json.OFormat
import uk.gov.hmrc.agentregistration.shared.EntityCheckResult.Pass
import uk.gov.hmrc.agentregistration.shared.agentdetails.AgentDetails
import uk.gov.hmrc.agentregistration.shared.businessdetails.*
import uk.gov.hmrc.agentregistration.shared.contactdetails.ApplicantContactDetails
import uk.gov.hmrc.agentregistration.shared.util.Errors.getOrThrowExpectedDataMissing
import uk.gov.hmrc.agentregistration.shared.util.JsonConfig
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.===

import java.time.Clock
import java.time.Instant
import scala.annotation.nowarn

/** Agent (Registration) Application. This final case class represents the data entered by a user for registering as an agent.
  */
sealed trait AgentApplication:

  def _id: AgentApplicationId
  def internalUserId: InternalUserId
  def linkId: LinkId
  def groupId: GroupId
  def createdAt: Instant
  def applicationState: ApplicationState
  def businessType: BusinessType
  def userRole: Option[UserRole]
  def applicantContactDetails: Option[ApplicantContactDetails]
  def amlsDetails: Option[AmlsDetails]
  def agentDetails: Option[AgentDetails]
  def refusalToDealWithCheck: EntityCheckResult
  def companyStatusCheckResult: EntityCheckResult
  def hmrcStandardForAgentsAgreed: StateOfAgreement

  //  /** Updates the application state to the next state */
  //  def updateApplicationState: AgentApplication =
  //    this match
  //      case st: AgentApplicationSoleTrader => st.copy(applicationState = nextApplicationState)
  //      case llp: ApplicationLlp => llp.copy(applicationState = nextApplicationState)

  /* derived stuff: */
  val agentApplicationId: AgentApplicationId = _id
  val lastUpdated: Instant = Instant.now(Clock.systemUTC())

  val hasFinished: Boolean =
    applicationState match
      case ApplicationState.Submitted => true
      case ApplicationState.Started => false
      case ApplicationState.GrsDataReceived => false

  val isInProgress: Boolean = !hasFinished

  def isGrsDataReceived: Boolean =
    applicationState match
      case ApplicationState.Started => false
      case ApplicationState.GrsDataReceived => true
      case ApplicationState.Submitted => true

  def getUserRole: UserRole = userRole.getOrElse(expectedDataNotDefinedError("userRole"))

  def isIncorporated: Boolean =
    businessType match
      case BusinessType.Partnership.LimitedLiabilityPartnership => true
      case BusinessType.LimitedCompany => true
      case BusinessType.Partnership.LimitedPartnership => true
      case BusinessType.Partnership.ScottishLimitedPartnership => true
      case _ => false

  def getApplicantContactDetails: ApplicantContactDetails = applicantContactDetails.getOrThrowExpectedDataMissing("agentDetails")
  def getAgentDetails: AgentDetails = agentDetails.getOrThrowExpectedDataMissing("agentDetails")

  def getCompanyProfile: CompanyProfile =
    businessType match
      case BusinessType.Partnership.LimitedLiabilityPartnership => this.asLlpApplication.getBusinessDetails.companyProfile
      case BusinessType.LimitedCompany => this.asLimitedCompanyApplication.getBusinessDetails.companyProfile
      case BusinessType.Partnership.LimitedPartnership => this.asLimitedPartnershipApplication.getBusinessDetails.companyProfile
      case BusinessType.Partnership.ScottishLimitedPartnership => this.asScottishLimitedPartnershipApplication.getBusinessDetails.companyProfile
      case _ => expectedDataNotDefinedError("Calling getCompanyProfile on non-incorporated business types is not supported")

  // all agent applications must have a UTR
  def getUtr: Utr =
    businessType match
      case BusinessType.Partnership.LimitedLiabilityPartnership => this.asLlpApplication.getBusinessDetails.saUtr.asUtr
      case BusinessType.SoleTrader => this.asSoleTraderApplication.getBusinessDetails.saUtr.asUtr
      case BusinessType.LimitedCompany => this.asLimitedCompanyApplication.getBusinessDetails.ctUtr.asUtr
      case BusinessType.Partnership.GeneralPartnership => this.asGeneralPartnershipApplication.getBusinessDetails.saUtr.asUtr
      case BusinessType.Partnership.LimitedPartnership => this.asLimitedPartnershipApplication.getBusinessDetails.saUtr.asUtr
      case BusinessType.Partnership.ScottishLimitedPartnership => this.asScottishLimitedPartnershipApplication.getBusinessDetails.saUtr.asUtr
      case BusinessType.Partnership.ScottishPartnership => this.asScottishPartnershipApplication.getBusinessDetails.saUtr.asUtr

  def getAmlsDetails: AmlsDetails = amlsDetails.getOrElse(expectedDataNotDefinedError("amlsDetails"))

  def hasEntityCheckPassed: Boolean =
    (refusalToDealWithCheck, companyStatusCheckResult) match
      case (Pass, Pass) => true
      case _ => false

  private def as[T <: AgentApplication](using ct: reflect.ClassTag[T]): Option[T] =
    this match
      case t: T => Some(t)
      case _ => None

  private def asExpected[T <: AgentApplication](using ct: reflect.ClassTag[T]): T = as[T].getOrThrowExpectedDataMissing(
    s"The application is not of the expected type. Expected: ${ct.runtimeClass.getSimpleName}, Got: ${this.getClass.getSimpleName}"
  )

  def asLlpApplication: AgentApplicationLlp = asExpected[AgentApplicationLlp]
  def asSoleTraderApplication: AgentApplicationSoleTrader = asExpected[AgentApplicationSoleTrader]
  def asLimitedCompanyApplication: AgentApplicationLimitedCompany = asExpected[AgentApplicationLimitedCompany]
  def asGeneralPartnershipApplication: AgentApplicationGeneralPartnership = asExpected[AgentApplicationGeneralPartnership]
  def asLimitedPartnershipApplication: AgentApplicationLimitedPartnership = asExpected[AgentApplicationLimitedPartnership]
  def asScottishLimitedPartnershipApplication: AgentApplicationScottishLimitedPartnership = asExpected[AgentApplicationScottishLimitedPartnership]
  def asScottishPartnershipApplication: AgentApplicationScottishPartnership = asExpected[AgentApplicationScottishPartnership]

/** Sole Trader Application. This final case class represents the data entered by a user for registering as a sole trader.
  */
final case class AgentApplicationSoleTrader(
  override val _id: AgentApplicationId,
  override val internalUserId: InternalUserId,
  override val linkId: LinkId,
  override val groupId: GroupId,
  override val createdAt: Instant,
  override val applicationState: ApplicationState,
  override val userRole: Option[UserRole],
  businessDetails: Option[BusinessDetailsSoleTrader],
  override val applicantContactDetails: Option[ApplicantContactDetails],
  override val amlsDetails: Option[AmlsDetails],
  override val agentDetails: Option[AgentDetails],
  override val refusalToDealWithCheck: EntityCheckResult,
  deceasedCheck: EntityCheckResult,
  override val companyStatusCheckResult: EntityCheckResult,
  override val hmrcStandardForAgentsAgreed: StateOfAgreement
)
extends AgentApplication:

  override val businessType: BusinessType.SoleTrader.type = BusinessType.SoleTrader

  override def hasEntityCheckPassed: Boolean =
    (refusalToDealWithCheck, deceasedCheck) match
      case (Pass, Pass) => true
      case _ => false

  def getBusinessDetails: BusinessDetailsSoleTrader = businessDetails.getOrElse(expectedDataNotDefinedError("businessDetails"))

/** Application for Limited Liability Partnership (Llp). This final case class represents the data entered by a user for registering as an Llp.
  */
final case class AgentApplicationLlp(
  override val _id: AgentApplicationId,
  override val internalUserId: InternalUserId,
  override val linkId: LinkId,
  override val groupId: GroupId,
  override val createdAt: Instant,
  override val applicationState: ApplicationState,
  override val userRole: Option[UserRole],
  businessDetails: Option[BusinessDetailsLlp],
  override val applicantContactDetails: Option[ApplicantContactDetails],
  override val amlsDetails: Option[AmlsDetails],
  override val agentDetails: Option[AgentDetails],
  override val refusalToDealWithCheck: EntityCheckResult,
  override val companyStatusCheckResult: EntityCheckResult,
  override val hmrcStandardForAgentsAgreed: StateOfAgreement
)
extends AgentApplication:

  override val businessType: BusinessType.Partnership.LimitedLiabilityPartnership.type = BusinessType.Partnership.LimitedLiabilityPartnership

  def getBusinessDetails: BusinessDetailsLlp = businessDetails.getOrThrowExpectedDataMissing("businessDetails")
  def getCrn: Crn = getBusinessDetails.companyProfile.companyNumber

/** Application for Limited Company. This final case class represents the data entered by a user for registering as a Limited Company.
  */
final case class AgentApplicationLimitedCompany(
  override val _id: AgentApplicationId,
  override val internalUserId: InternalUserId,
  override val linkId: LinkId,
  override val groupId: GroupId,
  override val createdAt: Instant,
  override val applicationState: ApplicationState,
  override val userRole: Option[UserRole],
  businessDetails: Option[BusinessDetailsLimitedCompany],
  override val applicantContactDetails: Option[ApplicantContactDetails],
  override val amlsDetails: Option[AmlsDetails],
  override val agentDetails: Option[AgentDetails],
  override val refusalToDealWithCheck: EntityCheckResult,
  override val companyStatusCheckResult: EntityCheckResult,
  override val hmrcStandardForAgentsAgreed: StateOfAgreement
)
extends AgentApplication:

  override val businessType: BusinessType.LimitedCompany.type = BusinessType.LimitedCompany

  def getBusinessDetails: BusinessDetailsLimitedCompany = businessDetails.getOrThrowExpectedDataMissing("businessDetails")
  def getCrn: Crn = getBusinessDetails.companyProfile.companyNumber

/** General Partnership Application. This final case class represents the data entered by a user for registering as a general partnership.
  */
final case class AgentApplicationGeneralPartnership(
  override val _id: AgentApplicationId,
  override val internalUserId: InternalUserId,
  override val linkId: LinkId,
  override val groupId: GroupId,
  override val createdAt: Instant,
  override val applicationState: ApplicationState,
  override val userRole: Option[UserRole],
  businessDetails: Option[BusinessDetailsGeneralPartnership],
  override val applicantContactDetails: Option[ApplicantContactDetails],
  override val amlsDetails: Option[AmlsDetails],
  override val agentDetails: Option[AgentDetails],
  override val refusalToDealWithCheck: EntityCheckResult,
  override val companyStatusCheckResult: EntityCheckResult,
  override val hmrcStandardForAgentsAgreed: StateOfAgreement
)
extends AgentApplication:

  override val businessType: BusinessType.Partnership.GeneralPartnership.type = BusinessType.Partnership.GeneralPartnership
  def getBusinessDetails: BusinessDetailsGeneralPartnership = businessDetails.getOrElse(expectedDataNotDefinedError("businessDetails"))

/** Application for Limited Partnership. This final case class represents the data entered by a user for registering as a Limited Partnership.
  */
final case class AgentApplicationLimitedPartnership(
  override val _id: AgentApplicationId,
  override val internalUserId: InternalUserId,
  override val linkId: LinkId,
  override val groupId: GroupId,
  override val createdAt: Instant,
  override val applicationState: ApplicationState,
  override val userRole: Option[UserRole],
  businessDetails: Option[BusinessDetailsPartnership],
  override val applicantContactDetails: Option[ApplicantContactDetails],
  override val amlsDetails: Option[AmlsDetails],
  override val agentDetails: Option[AgentDetails],
  override val refusalToDealWithCheck: EntityCheckResult,
  override val companyStatusCheckResult: EntityCheckResult,
  override val hmrcStandardForAgentsAgreed: StateOfAgreement
)
extends AgentApplication:

  override val businessType: BusinessType.Partnership.LimitedPartnership.type = BusinessType.Partnership.LimitedPartnership

  def getBusinessDetails: BusinessDetailsPartnership = businessDetails.getOrThrowExpectedDataMissing("businessDetails")
  def getCrn: Crn = getBusinessDetails.companyProfile.companyNumber

final case class AgentApplicationScottishLimitedPartnership(
  override val _id: AgentApplicationId,
  override val internalUserId: InternalUserId,
  override val linkId: LinkId,
  override val groupId: GroupId,
  override val createdAt: Instant,
  override val applicationState: ApplicationState,
  override val userRole: Option[UserRole],
  businessDetails: Option[BusinessDetailsPartnership],
  override val applicantContactDetails: Option[ApplicantContactDetails],
  override val amlsDetails: Option[AmlsDetails],
  override val agentDetails: Option[AgentDetails],
  override val refusalToDealWithCheck: EntityCheckResult,
  override val companyStatusCheckResult: EntityCheckResult,
  override val hmrcStandardForAgentsAgreed: StateOfAgreement
)
extends AgentApplication:

  override val businessType: BusinessType.Partnership.ScottishLimitedPartnership.type = BusinessType.Partnership.ScottishLimitedPartnership

  def getBusinessDetails: BusinessDetailsPartnership = businessDetails.getOrThrowExpectedDataMissing("businessDetails")
  def getCrn: Crn = getBusinessDetails.companyProfile.companyNumber

final case class AgentApplicationScottishPartnership(
  override val _id: AgentApplicationId,
  override val internalUserId: InternalUserId,
  override val linkId: LinkId,
  override val groupId: GroupId,
  override val createdAt: Instant,
  override val applicationState: ApplicationState,
  override val userRole: Option[UserRole],
  businessDetails: Option[BusinessDetailsScottishPartnership],
  override val applicantContactDetails: Option[ApplicantContactDetails],
  override val amlsDetails: Option[AmlsDetails],
  override val agentDetails: Option[AgentDetails],
  override val refusalToDealWithCheck: EntityCheckResult,
  override val companyStatusCheckResult: EntityCheckResult,
  override val hmrcStandardForAgentsAgreed: StateOfAgreement
)
extends AgentApplication:

  override val businessType: BusinessType.Partnership.ScottishPartnership.type = BusinessType.Partnership.ScottishPartnership

  override def hasEntityCheckPassed: Boolean = refusalToDealWithCheck === Pass

  def getBusinessDetails: BusinessDetailsScottishPartnership = businessDetails.getOrThrowExpectedDataMissing("businessDetails")

object AgentApplication:

  @nowarn()
  given format: OFormat[AgentApplication] =
    given OFormat[AgentApplicationSoleTrader] = Json.format[AgentApplicationSoleTrader]
    given OFormat[AgentApplicationLlp] = Json.format[AgentApplicationLlp]
    given OFormat[AgentApplicationLimitedCompany] = Json.format[AgentApplicationLimitedCompany]
    given OFormat[AgentApplicationGeneralPartnership] = Json.format[AgentApplicationGeneralPartnership]
    given OFormat[AgentApplicationLimitedPartnership] = Json.format[AgentApplicationLimitedPartnership]
    given OFormat[AgentApplicationScottishLimitedPartnership] = Json.format[AgentApplicationScottishLimitedPartnership]
    given OFormat[AgentApplicationScottishPartnership] = Json.format[AgentApplicationScottishPartnership]
    given JsonConfiguration = JsonConfig.jsonConfiguration

    val dontDeleteMe = """
                         |Don't delete me.
                         |I will emit a warning so `@nowarn` can be applied to address below
                         |`Unreachable case except for null` problem emited by Play Json macro"""

    Json.format[AgentApplication]

private inline def expectedDataNotDefinedError(key: String): Nothing = throw new RuntimeException(s"Expected $key to be defined")
