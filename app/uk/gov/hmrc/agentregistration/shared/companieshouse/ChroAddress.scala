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
    // ensure serialisation to CorrespondenceAddress
    Seq(
      care_of.getOrElse("").trim,
      po_box.getOrElse("").trim,
      premises.getOrElse("").trim
    )
      .filter(_.nonEmpty).mkString(" "),
    address_line_1.getOrElse("").trim,
    address_line_2.getOrElse("").trim,
    locality.getOrElse("").trim,
    postal_code.getOrElse("").trim,
    country.getOrElse("").trim
  )
    .filter(_.nonEmpty).mkString(", ")

object ChroAddress:
  implicit val format: Format[ChroAddress] = Json.format[ChroAddress]
