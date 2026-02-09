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

package uk.gov.hmrc.agentregistration.shared.individual

import play.api.libs.json.*
import uk.gov.hmrc.agentregistration.shared.AgentApplicationId
import uk.gov.hmrc.agentregistration.shared.InternalUserId
import uk.gov.hmrc.agentregistration.shared.StateOfAgreement
import uk.gov.hmrc.agentregistration.shared.TelephoneNumber
import uk.gov.hmrc.agentregistration.shared.companieshouse.CompaniesHouseMatch
import uk.gov.hmrc.agentregistration.shared.individual.ProvidedDetailsState.Finished
import uk.gov.hmrc.agentregistration.shared.util.Errors.*
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.===

import java.time.Instant

/** Individual provided details for Limited Liability Partnership (Llp). This final case class represents the data entered by a user for approving as an Llp.
  */
/*
@deprecated("Use `IndividualProvidedDetailsToBe` with optional internalUserId instead", "2026-02-02")
 */
final case class IndividualProvidedDetailsToBeDeleted(
  _id: IndividualProvidedDetailsId,
  internalUserId: InternalUserId,
  createdAt: Instant,
  providedDetailsState: ProvidedDetailsState,
  agentApplicationId: AgentApplicationId,
  companiesHouseMatch: Option[CompaniesHouseMatch] = None,
  individualDateOfBirth: Option[IndividualDateOfBirth] = None,
  telephoneNumber: Option[TelephoneNumber] = None,
  emailAddress: Option[IndividualVerifiedEmailAddress] = None,
  individualNino: Option[IndividualNino] = None,
  individualSaUtr: Option[IndividualSaUtr] = None,
  hmrcStandardForAgentsAgreed: StateOfAgreement = StateOfAgreement.NotSet,
  hasApprovedApplication: Option[Boolean] = None
):

  val individualProvidedDetailsId: IndividualProvidedDetailsId = _id

  val hasFinished: Boolean = providedDetailsState === Finished
  val isInProgress: Boolean = !hasFinished

  def getCompaniesHouseMatch: CompaniesHouseMatch = companiesHouseMatch.getOrThrowExpectedDataMissing(
    "Companies house query is missing for individual provided details"
  )

  def getEmailAddress: IndividualVerifiedEmailAddress = emailAddress.getOrThrowExpectedDataMissing("Email address")

  def getTelephoneNumber: TelephoneNumber = telephoneNumber.getOrThrowExpectedDataMissing("Telephone number")

  def getNino: IndividualNino = individualNino.getOrThrowExpectedDataMissing("Nino")

  def getSaUtr: IndividualSaUtr = individualSaUtr.getOrThrowExpectedDataMissing("SaUtr")

  def getDateOfBirth: IndividualDateOfBirth = individualDateOfBirth.getOrThrowExpectedDataMissing("Date of birth")

/*
@deprecated("Use `IndividualProvidedDetailsToBe` with optional internalUserId instead", "2026-02-02")
 */
object IndividualProvidedDetailsToBeDeleted:
  given format: OFormat[IndividualProvidedDetailsToBeDeleted] = Json.format[IndividualProvidedDetailsToBeDeleted]
