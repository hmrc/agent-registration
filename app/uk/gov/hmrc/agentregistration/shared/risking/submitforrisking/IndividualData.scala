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

package uk.gov.hmrc.agentregistration.shared.risking.submitforrisking

import play.api.libs.json.Json
import play.api.libs.json.OFormat
import uk.gov.hmrc.agentregistration.shared.individual.IndividualDateOfBirth
import uk.gov.hmrc.agentregistration.shared.individual.IndividualNino
import uk.gov.hmrc.agentregistration.shared.individual.IndividualSaUtr
import uk.gov.hmrc.agentregistration.shared.lists.IndividualName
import uk.gov.hmrc.agentregistration.shared.*

final case class IndividualData(
  personReference: PersonReference,
  individualName: IndividualName,
  isPersonOfControl: Boolean,
  internalUserId: InternalUserId,
  individualDateOfBirth: IndividualDateOfBirth,
  telephoneNumber: TelephoneNumber,
  emailAddress: EmailAddress,
  individualNino: IndividualNino,
  individualSaUtr: IndividualSaUtr,
  vrns: List[Vrn],
  payeRefs: List[PayeRef],
  passedIv: Boolean
):

  // values that we do not store at the moment
  def providedByApplicant: Boolean = false // TODO: APB-11409
  def companiesHouseName = None // TODO: APB-11409
  def companiesHouseDateOfBirth = None // TODO: APB-11409

object IndividualData:
  given format: OFormat[IndividualData] = Json.format[IndividualData]
