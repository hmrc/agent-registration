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

package uk.gov.hmrc.agentregistration.shared.lists

import play.api.libs.json.Format
import play.api.libs.json.Json
import play.api.libs.json.JsonConfiguration
import uk.gov.hmrc.agentregistration.shared.util.JsonConfig
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.===

sealed trait NumberOfIndividuals

// =======================================================
// Required Key Individuals
// =======================================================
sealed trait NumberOfRequiredKeyIndividuals
extends NumberOfIndividuals:

  def isValid: Boolean
  def totalListSize: Int

/** When there less or equal 5, the Applicant has to declare the exact number of all key individuals (partners, directors, owners, etc) */
final case class FiveOrLess(
  numberOfKeyIndividuals: Int
)
extends NumberOfRequiredKeyIndividuals:

  override def isValid: Boolean = numberOfKeyIndividuals <= 5 && numberOfKeyIndividuals >= 1
  override def totalListSize: Int = numberOfKeyIndividuals

/** When there are more than 5, the Applicant has to declare how many of those are responsible for tax matters */
final case class SixOrMore(
  numberOfKeyIndividualsResponsibleForTaxMatters: Int
)
extends NumberOfRequiredKeyIndividuals:

  override def isValid: Boolean = numberOfKeyIndividualsResponsibleForTaxMatters >= 1 && numberOfKeyIndividualsResponsibleForTaxMatters <= 30

  def requiredPadding: Int = Math.max(0, 5 - numberOfKeyIndividualsResponsibleForTaxMatters)

  override def totalListSize: Int = numberOfKeyIndividualsResponsibleForTaxMatters + requiredPadding

object NumberOfRequiredKeyIndividuals:

  def isKeyIndividualListComplete(
    listSize: Int,
    numberOfRequiredKeyIndividuals: NumberOfRequiredKeyIndividuals
  ): Boolean =
    numberOfRequiredKeyIndividuals match
      case FiveOrLess(a) => listSize === a
      case a @ SixOrMore(_) => listSize === a.totalListSize

  given Format[NumberOfRequiredKeyIndividuals] =
    given Format[FiveOrLess] = Json.format[FiveOrLess]
    given Format[SixOrMore] = Json.format[SixOrMore]

    given JsonConfiguration = JsonConfig.jsonConfiguration
    Json.format[NumberOfRequiredKeyIndividuals]

// =======================================================
// Companies House Officers
// =======================================================
sealed trait NumberOfCompaniesHouseOfficers
extends NumberOfIndividuals:

  def numberOfCompaniesHouseOfficers: Int
  def isValid: Boolean
  def totalListSize: Int

/** When there less or equal 5, the Applicant has to declare the exact number of all key individuals (partners, directors, owners, etc) */
final case class FiveOrLessOfficers(
  override val numberOfCompaniesHouseOfficers: Int,
  isCompaniesHouseOfficersListCorrect: Boolean
)
extends NumberOfCompaniesHouseOfficers:

  override def isValid: Boolean = numberOfCompaniesHouseOfficers <= 5 && numberOfCompaniesHouseOfficers >= 1
  override def totalListSize: Int = numberOfCompaniesHouseOfficers

/** When there are more than 5, the Applicant has to declare how many of those are responsible for tax matters */
final case class SixOrMoreOfficers(
  override val numberOfCompaniesHouseOfficers: Int,
  numberOfOfficersResponsibleForTaxMatters: Int
)
extends NumberOfCompaniesHouseOfficers:

  override def isValid: Boolean = numberOfOfficersResponsibleForTaxMatters >= 1 && numberOfOfficersResponsibleForTaxMatters <= numberOfCompaniesHouseOfficers

  override def totalListSize: Int = numberOfOfficersResponsibleForTaxMatters

object NumberOfCompaniesHouseOfficers:

  def isOfficersListComplete(
    listSize: Int,
    numberOfCompaniesHouseOfficers: NumberOfCompaniesHouseOfficers
  ): Boolean =
    numberOfCompaniesHouseOfficers match
      case a @ FiveOrLessOfficers(_, b) => a.isValid && b
      case a @ SixOrMoreOfficers(_, _) => a.isValid && listSize === a.totalListSize

  given Format[NumberOfCompaniesHouseOfficers] =
    given Format[FiveOrLessOfficers] = Json.format[FiveOrLessOfficers]
    given Format[SixOrMoreOfficers] = Json.format[SixOrMoreOfficers]

    given JsonConfiguration = JsonConfig.jsonConfiguration
    Json.format[NumberOfCompaniesHouseOfficers]

// =======================================================
// NumberOfIndividuals helpers + format
// =======================================================
object NumberOfIndividuals:

  def isKeyIndividualListComplete(
    listSize: Int,
    numberOfIndividuals: Option[NumberOfIndividuals]
  ): Boolean =
    numberOfIndividuals match
      case Some(a: NumberOfRequiredKeyIndividuals) => NumberOfRequiredKeyIndividuals.isKeyIndividualListComplete(listSize, a)
      case Some(a: NumberOfCompaniesHouseOfficers) => NumberOfCompaniesHouseOfficers.isOfficersListComplete(listSize, a)
      case None => false

  given Format[NumberOfIndividuals] =
    given JsonConfiguration = JsonConfig.jsonConfiguration

    // Leaf ADT formats must be in scope for the parent macro derivation:
    given Format[FiveOrLess] = Json.format[FiveOrLess]
    given Format[SixOrMore] = Json.format[SixOrMore]
    given Format[FiveOrLessOfficers] = Json.format[FiveOrLessOfficers]
    given Format[SixOrMoreOfficers] = Json.format[SixOrMoreOfficers]

//    // Intermediate ADTs:
//    given Format[NumberOfRequiredKeyIndividuals] = Json.format[NumberOfRequiredKeyIndividuals]
//    given Format[NumberOfCompaniesHouseOfficers] = Json.format[NumberOfCompaniesHouseOfficers]

    // Parent:
    Json.format[NumberOfIndividuals]
