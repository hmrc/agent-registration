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

package uk.gov.hmrc.agentregistration.shared.amls

import play.api.libs.json.Format
import uk.gov.hmrc.agentregistration.shared.util.JsonFormatsFactory

import scala.util.matching.Regex

final case class AmlsRegistrationNumber(value: String)

object AmlsRegistrationNumber:

  /** These patterns are taken from https://confluence.tools.tax.service.gov.uk/pages/viewpage.action?spaceKey=AG&title=Anti-money+laundering+supervision When a
    * supervisory body does not have a specific pattern, the defaultRegex [A-Za-z0-9,.'\-/ ]{0,100} should be used.
    */
  private val defaultRegex: Regex = "^[A-Za-z0-9,.'\\-/ ]{0,100}$".r

  private val supervisoryBodyRegexes: Map[String, Regex] = Map(
    "HMRC" -> "^X[A-Z]ML00000[0-9]{6}$".r,
    "AAT" -> """^\d{2,9}$""".r,
    "ACCA" -> """^\d{5,10}$""".r,
    "AIA" -> """^\d{4,6}$""".r,
    "ATT" -> """^ATT AML-\d{1,4}-\d{6}$""".r,
    "CIMA" -> """^\d{7,9}$""".r,
    "CIOT" -> """^CIOT AML-\d{1,4}-\d{6}$""".r,
    "FCA" -> """^\d{6}$""".r,
    "ICAEW" -> """^(?:[A-Za-z]\d{9}|\d{7})$""".r,
    "ICAS" -> """^[A-Za-z]\d{6}$""".r,
    "ICB" -> """^\d{4,5}$""".r,
    "IFA" -> """^\d{6}$""".r,
    "FRA" -> """^\d{5,6}$""".r
  )

  def isValidForChosenSupervisoryBody(
    value: String,
    supervisoryBody: AmlsSupervisoryBodyCode
  ): Boolean =
    val regexForAmlsCode: Regex =
      supervisoryBodyRegexes.get(supervisoryBody.value) match
        case Some(regex: Regex) => regex
        case None => defaultRegex

    value match
      case regexForAmlsCode(_*) => true
      case _ => false

  def apply(value: String): AmlsRegistrationNumber = new AmlsRegistrationNumber(value.trim)

  implicit val format: Format[AmlsRegistrationNumber] = JsonFormatsFactory.makeValueClassFormat
