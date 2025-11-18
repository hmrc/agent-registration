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

package uk.gov.hmrc.agentregistration.shared.agentdetails

import play.api.libs.json.*
import uk.gov.hmrc.agentregistration.shared.AddressLookupFrontendAddress

final case class AgentCorrespondenceAddress(
  addressLine1: String,
  addressLine2: Option[String],
  addressLine3: Option[String] = None,
  addressLine4: Option[String] = None,
  postalCode: Option[String],
  countryCode: String
):

  def toValueString: String = Seq(
    Some(addressLine1),
    addressLine2,
    addressLine3,
    addressLine4,
    postalCode,
    Some(countryCode)
  ).flatten.mkString(", ")

  def toHtml: String = Seq(
    Some(addressLine1),
    addressLine2,
    addressLine3,
    addressLine4,
    postalCode,
    Some(countryCode)
  ).flatten.mkString("<br>")

object AgentCorrespondenceAddress:

  given format: Format[AgentCorrespondenceAddress] = Json.format[AgentCorrespondenceAddress]
  def fromString(address: String): AgentCorrespondenceAddress = {
    val parts = address.split(",").map(_.trim)
    if (parts.length == 6) then
      AgentCorrespondenceAddress(
        addressLine1 = parts.headOption.getOrElse(""),
        addressLine2 = parts.lift(1),
        addressLine3 = parts.lift(2),
        addressLine4 = parts.lift(3),
        postalCode = parts.lift(4),
        countryCode = parts.lift(5).getOrElse("")
      )
    else if (parts.length == 5) then
      AgentCorrespondenceAddress(
        addressLine1 = parts.headOption.getOrElse(""),
        addressLine2 = parts.lift(1),
        addressLine3 = parts.lift(2),
        addressLine4 = None,
        postalCode = parts.lift(3),
        countryCode = parts.lift(4).getOrElse("")
      )
    else if (parts.length == 4) then
      AgentCorrespondenceAddress(
        addressLine1 = parts.headOption.getOrElse(""),
        addressLine2 = parts.lift(1),
        addressLine3 = None,
        addressLine4 = None,
        postalCode = parts.lift(2),
        countryCode = parts.lift(3).getOrElse("")
      )
    else
      throw new IllegalArgumentException(s"Cannot parse CorrespondenceAddress from string: $address")
  }

  def fromAddressLookupAddress(address: AddressLookupFrontendAddress): AgentCorrespondenceAddress = {
    val lines = address.lines
    AgentCorrespondenceAddress(
      addressLine1 = lines.headOption.getOrElse(""),
      addressLine2 = lines.lift(1),
      addressLine3 = lines.lift(2),
      addressLine4 = lines.lift(3),
      postalCode = address.postcode,
      countryCode = address.country.code
    )
  }
