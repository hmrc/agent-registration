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

package uk.gov.hmrc.agentregistration.shared.llp

import play.api.libs.json.*
import uk.gov.hmrc.agentregistration.shared.AgentApplicationId
import uk.gov.hmrc.agentregistration.shared.InternalUserId
import uk.gov.hmrc.agentregistration.shared.TelephoneNumber
import uk.gov.hmrc.agentregistration.shared.companieshouse.CompaniesHouseMatch
import uk.gov.hmrc.agentregistration.shared.llp.ProvidedDetailsState.Finished
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.===
import uk.gov.hmrc.agentregistration.shared.util.Errors.*

import java.time.Instant

/** Member provided details for Limited Liability Partnership (Llp). This case class represents the data entered by a user for approving as an Llp.
  */
final case class MemberProvidedDetails(
  _id: MemberProvidedDetailsId,
  internalUserId: InternalUserId,
  createdAt: Instant,
  providedDetailsState: ProvidedDetailsState,
  agentApplicationId: AgentApplicationId,
  companiesHouseMatch: Option[CompaniesHouseMatch] = None,
  telephoneNumber: Option[TelephoneNumber] = None,
  emailAddress: Option[MemberVerifiedEmailAddress] = None,
  memberNino: Option[MemberNino] = None,
  memberSaUtr: Option[MemberSaUtr] = None
):

  val memberProvidedDetailsId: MemberProvidedDetailsId = _id
  val hasFinished: Boolean = if providedDetailsState === Finished then true else false
  val isInProgress: Boolean = !hasFinished
  def getCompaniesHouseMatch: CompaniesHouseMatch = companiesHouseMatch.getOrThrowExpectedDataMissing(
    "Companies house query is missing for member provided details"
  )
  def getEmailAddress: MemberVerifiedEmailAddress = emailAddress.getOrThrowExpectedDataMissing("Email address is missing")

object MemberProvidedDetails:
  given format: OFormat[MemberProvidedDetails] = Json.format[MemberProvidedDetails]
