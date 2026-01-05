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

import play.api.libs.json.JsObject
import play.api.libs.json.Json
import play.api.libs.json.JsonConfiguration
import play.api.libs.json.OFormat
import play.api.libs.json.OWrites
import play.api.libs.json.Reads
import uk.gov.hmrc.agentregistration.shared.agentdetails.AgentDetails
import uk.gov.hmrc.agentregistration.shared.contactdetails.ApplicantContactDetails
import uk.gov.hmrc.agentregistration.shared.util.JsonConfig
import uk.gov.hmrc.agentregistration.shared.util.Errors.getOrThrowExpectedDataMissing

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
  def amlsDetails: Option[AmlsDetails]
  def agentDetails: Option[AgentDetails]
  def entityCheckResult: Option[EntityCheckResult]
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

  // all agent applications must have a UTR
  def getUtr: Utr =
    this match
      case a: AgentApplicationLlp => a.getBusinessDetails.saUtr.asUtr
      case _ => expectedDataNotDefinedError("currently utr is only defined for Llp applications, as other types are not implemented yet")

  def getAmlsDetails: AmlsDetails = amlsDetails.getOrElse(expectedDataNotDefinedError("amlsDetails"))
  def getEntityCheckResult: EntityCheckResult = entityCheckResult.getOrElse(expectedDataNotDefinedError("entityCheckResult"))

  private def as[T <: AgentApplication](using ct: reflect.ClassTag[T]): Option[T] =
    this match
      case t: T => Some(t)
      case _ => None

  private def asExpected[T <: AgentApplication](using ct: reflect.ClassTag[T]): T = as[T].getOrThrowExpectedDataMissing(
    s"The application is not of the expected type. Expected: ${ct.runtimeClass.getSimpleName}, Got: ${this.getClass.getSimpleName}"
  )

  def asLlpApplication: AgentApplicationLlp = asExpected[AgentApplicationLlp]

/** Sole Trader Application. This final case class represents the data entered by a user for registering as a sole trader.
  */
final case class AgentApplicationSoleTrader(
  override val _id: AgentApplicationId,
  override val internalUserId: InternalUserId,
  override val linkId: LinkId,
  override val groupId: GroupId,
  override val createdAt: Instant,
  override val applicationState: ApplicationState,
  userRole: Option[UserRole] = None,
  businessDetails: Option[BusinessDetailsSoleTrader],
  override val amlsDetails: Option[AmlsDetails],
  override val agentDetails: Option[AgentDetails],
  override val entityCheckResult: Option[EntityCheckResult],
  override val hmrcStandardForAgentsAgreed: StateOfAgreement
)
extends AgentApplication:

  override val businessType: BusinessType.SoleTrader.type = BusinessType.SoleTrader
  def getUserRole: UserRole = userRole.getOrElse(expectedDataNotDefinedError("userRole"))
  def getBusinessDetails: BusinessDetailsSoleTrader = businessDetails.getOrElse(expectedDataNotDefinedError("businessDetails"))

/** Application Applicatoin for Limited Liability Partnership (Llp). This final case class represents the data entered by a user for registering as an Llp.
  */
final case class AgentApplicationLlp(
  override val _id: AgentApplicationId,
  override val internalUserId: InternalUserId,
  override val linkId: LinkId,
  override val groupId: GroupId,
  override val createdAt: Instant,
  override val applicationState: ApplicationState,
  businessDetails: Option[BusinessDetailsLlp],
  applicantContactDetails: Option[ApplicantContactDetails],
  override val amlsDetails: Option[AmlsDetails],
  override val agentDetails: Option[AgentDetails],
  override val entityCheckResult: Option[EntityCheckResult],
  override val hmrcStandardForAgentsAgreed: StateOfAgreement
)
extends AgentApplication:

  override val businessType: BusinessType.Partnership.LimitedLiabilityPartnership.type = BusinessType.Partnership.LimitedLiabilityPartnership

  def getApplicantContactDetails: ApplicantContactDetails = applicantContactDetails.getOrThrowExpectedDataMissing("ApplicantContactDetails")
  def getBusinessDetails: BusinessDetailsLlp = businessDetails.getOrThrowExpectedDataMissing("businessDetails")
  def getCrn: Crn = getBusinessDetails.companyProfile.companyNumber
  def getAgentDetails: AgentDetails = agentDetails.getOrThrowExpectedDataMissing("agentDetails")

object AgentApplication:

  @nowarn()
  given format: OFormat[AgentApplication] =
    given OFormat[AgentApplicationSoleTrader] = Json.format[AgentApplicationSoleTrader]
    given OFormat[AgentApplicationLlp] = Json.format[AgentApplicationLlp]
    given JsonConfiguration = JsonConfig.jsonConfiguration

    val dontDeleteMe = """
                         |Don't delete me.
                         |I will emit a warning so `@nowarn` can be applied to address below
                         |`Unreachable case except for null` problem emited by Play Json macro"""

    Json.format[AgentApplication]

private inline def expectedDataNotDefinedError(key: String): Nothing = throw new RuntimeException(s"Expected $key to be defined")
