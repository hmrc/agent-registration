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

import play.api.libs.json.Json
import play.api.libs.json.Format
import uk.gov.hmrc.agentregistration.shared.util.StringExtensions.replaceCommasWithSpaces

final case class DesBusinessAddress(
  addressLine1: String,
  addressLine2: Option[String],
  addressLine3: Option[String] = None,
  addressLine4: Option[String] = None,
  postalCode: Option[String],
  countryCode: String
):
  def toValueString: String = Seq(
    this.addressLine1.replaceCommasWithSpaces,
    this.addressLine2.getOrElse("").replaceCommasWithSpaces,
    this.addressLine3.getOrElse("").replaceCommasWithSpaces,
    this.addressLine4.getOrElse("").replaceCommasWithSpaces,
    this.postalCode.getOrElse("").replaceCommasWithSpaces,
    this.countryCode.replaceCommasWithSpaces
  )
    .filter(_.nonEmpty).mkString(", ")

object DesBusinessAddress:
  given format: Format[DesBusinessAddress] = Json.format[DesBusinessAddress]
