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

package uk.gov.hmrc.agentregistration.shared.companieshouse

import play.api.libs.json.Format
import play.api.libs.json.Json
import uk.gov.hmrc.agentregistration.shared.util.StringExtensions.*

/** As returned to GRS by Companies House as the Companies House Registered Office (CHRO) address can contain care_of, po_box and premises fields which are not
  * part of the standard AgentCorrespondenceAddress model we subscribe agents with so when we map ChroAddress to a value string, compatible with
  * AgentCorrespondenceAddress, we concatenate these fields into a single line without commas to avoid losing data.
  *
  * We never have to map CorrespondenceAddress back to ChroAddress so this one-way mapping is sufficient.
  */

final case class ChroAddress(
  address_line_1: Option[String] = None,
  address_line_2: Option[String] = None,
  locality: Option[String] = None,
  care_of: Option[String] = None,
  po_box: Option[String] = None,
  postal_code: Option[String] = None,
  premises: Option[String] = None,
  country: Option[String] = None
):

  // concat address fields into a single string to use in radio values and labels
  def toValueString: String = Seq(
    // concatenate optional care_of, po_box, premises values into a single line to
    // ensure they are included in any serialisation to AgentCorrespondenceAddress
    Seq(
      care_of.getOrElse("").replaceCommasWithSpaces.trim,
      po_box.getOrElse("").replaceCommasWithSpaces.trim,
      premises.getOrElse("").replaceCommasWithSpaces.trim
    )
      .filter(_.nonEmpty).mkString(" "),
    address_line_1.getOrElse("").replaceCommasWithSpaces,
    address_line_2.getOrElse("").replaceCommasWithSpaces.trim,
    locality.getOrElse("").replaceCommasWithSpaces.trim,
    postal_code.getOrElse("").replaceCommasWithSpaces.trim,
    country.getOrElse("").replaceCommasWithSpaces.trim
  )
    .filter(_.nonEmpty).mkString(", ")

object ChroAddress:
  implicit val format: Format[ChroAddress] = Json.format[ChroAddress]
