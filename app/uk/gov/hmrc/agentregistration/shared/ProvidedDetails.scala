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
import uk.gov.hmrc.agentregistration.shared.util.JsonConfig
import uk.gov.hmrc.agentregistration.shared.util.Errors.getOrThrowExpectedDataMissing

import java.time.Clock
import java.time.Instant
import scala.annotation.nowarn

/** Agent (Registration) Application. This case class represents the data entered by a user for registering as an agent.
  */
sealed trait ProvidedDetails:

  def internalUserId: InternalUserId
  def createdAt: Instant
  def linkId: LinkId
  def hasFinished: Boolean

  /* derived stuff: */

  val lastUpdated: Instant = Instant.now(Clock.systemUTC())
  val isInProgress: Boolean = !hasFinished

  private def as[T <: ProvidedDetails](using ct: reflect.ClassTag[T]): Option[T] =
    this match
      case t: T => Some(t)
      case _ => None

  private def asExpected[T <: ProvidedDetails](using ct: reflect.ClassTag[T]): T = as[T].getOrThrowExpectedDataMissing(
    s"The provided details are not of the expected type. Expected: ${ct.runtimeClass.getSimpleName}, Got: ${this.getClass.getSimpleName}"
  )

  def asLlp: MemberProvidedDetailsLlp = asExpected[MemberProvidedDetailsLlp]

/** Application Applicatoin for Limited Liability Partnership (Llp). This case class represents the data entered by a user for registering as an Llp.
  */
final case class MemberProvidedDetailsLlp(
  override val internalUserId: InternalUserId,
  override val createdAt: Instant,
  override val linkId: LinkId,
  nino: Option[Nino]
)
extends ProvidedDetails:

  override def hasFinished: Boolean = true

//  def getApplicantContactDetails: ApplicantContactDetails = applicantContactDetails.getOrThrowExpectedDataMissing("ApplicantContactDetails")
//  def getBusinessDetails: BusinessDetailsLlp = businessDetails.getOrThrowExpectedDataMissing("businessDetails")
//  def getCrn: Crn = getBusinessDetails.companyProfile.companyNumber

object ProvidedDetails:

  @nowarn()
  given format: OFormat[ProvidedDetails] =
    given OFormat[MemberProvidedDetailsLlp] = Json.format[MemberProvidedDetailsLlp]
    given JsonConfiguration = JsonConfig.jsonConfiguration

    val dontDeleteMe = """
                         |Don't delete me.
                         |I will emit a warning so `@nowarn` can be applied to address below
                         |`Unreachable case except for null` problem emited by Play Json macro"""

    Json.format[ProvidedDetails]

private inline def expectedDataNotDefinedError(key: String): Nothing = throw new RuntimeException(s"Expected $key to be defined")
