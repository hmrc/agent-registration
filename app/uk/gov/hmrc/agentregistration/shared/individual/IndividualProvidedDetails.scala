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
import uk.gov.hmrc.agentregistration.shared.lists.IndividualName
import uk.gov.hmrc.agentregistration.shared.individual.ProvidedDetailsState.Finished
import uk.gov.hmrc.agentregistration.shared.individual.ProvidedDetailsState.Precreated
import uk.gov.hmrc.agentregistration.shared.util.Errors.*
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.===

import java.time.Instant

/** Individual provided details. This final case class represents the data entered by a user nominated within an application.
  */
final case class IndividualProvidedDetails(
  _id: IndividualProvidedDetailsId,
  individualName: IndividualName, // supplied by applicant
  isPersonOfControl: Boolean, // is this a person of control e.g. partner, director etc.
  internalUserId: Option[InternalUserId],
  createdAt: Instant,
  providedDetailsState: ProvidedDetailsState,
  agentApplicationId: AgentApplicationId,
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
  val isPrecreated: Boolean = providedDetailsState === Precreated

  def getInternalUserId: InternalUserId = internalUserId.getOrThrowExpectedDataMissing("Internal user ID")

  def getEmailAddress: IndividualVerifiedEmailAddress = emailAddress.getOrThrowExpectedDataMissing("Email address")

  def getTelephoneNumber: TelephoneNumber = telephoneNumber.getOrThrowExpectedDataMissing("Telephone number")

  def getNino: IndividualNino = individualNino.getOrThrowExpectedDataMissing("Nino")

  def getSaUtr: IndividualSaUtr = individualSaUtr.getOrThrowExpectedDataMissing("SaUtr")

  def getDateOfBirth: IndividualDateOfBirth = individualDateOfBirth.getOrThrowExpectedDataMissing("Date of birth")

object IndividualProvidedDetails:
  given format: OFormat[IndividualProvidedDetails] = Json.format[IndividualProvidedDetails]
