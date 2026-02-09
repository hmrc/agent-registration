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

package uk.gov.hmrc.agentregistration.shared.lists

import play.api.libs.json.Format
import play.api.libs.json.Json
import play.api.libs.json.JsonConfiguration
import uk.gov.hmrc.agentregistration.shared.util.JsonConfig

sealed trait NumberOfRequiredKeyIndividuals:

  def isValid: Boolean
  def totalListSize: Int

/** When there less or equal 5, the Applicant has to declare the exact number of all key individuals (partners, directors, owners, etc)
  */
final case class FiveOrLess(
  numberOfKeyIndividuals: Int
)
extends NumberOfRequiredKeyIndividuals:

  override def isValid: Boolean = numberOfKeyIndividuals <= 5 && numberOfKeyIndividuals >= 1

  override def totalListSize: Int = numberOfKeyIndividuals

/** When there are more than 5, the Applicant has to declare how many of those are responsible for tax matters
  */
final case class SixOrMore(
  numberOfKeyIndividualsResponsibleForTaxMatters: Int
)
extends NumberOfRequiredKeyIndividuals:

  override def isValid: Boolean = numberOfKeyIndividualsResponsibleForTaxMatters >= 1 && numberOfKeyIndividualsResponsibleForTaxMatters <= 30
  def requiredPadding: Int = Math.max(0, 5 - numberOfKeyIndividualsResponsibleForTaxMatters)
  override def totalListSize: Int = numberOfKeyIndividualsResponsibleForTaxMatters + requiredPadding

object NumberOfRequiredKeyIndividuals:

  given Format[NumberOfRequiredKeyIndividuals] =
    given Format[FiveOrLess] = Json.format[FiveOrLess]
    given Format[SixOrMore] = Json.format[SixOrMore]

    given JsonConfiguration = JsonConfig.jsonConfiguration

    Json.format[NumberOfRequiredKeyIndividuals]
