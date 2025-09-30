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

import play.api.libs.json.Format
import uk.gov.hmrc.agentregistration.shared.util.JsonFormatsFactory

final case class AmlsRegistrationNumber(value: String)

object AmlsRegistrationNumber {

  private val amlsRegistrationNumberRegex = "^[A-Za-z0-9,.'\\-/ ]{0,100}".r
  private val amlsRegistrationNumberForHmrcRegex = "^X[A-Z]ML00000[0-9]{6}$".r

  def isValidForHmrc(value: String): Boolean =
    value match {
      case amlsRegistrationNumberForHmrcRegex(_*) => true
      case _ => false
    }

  def isValidForNonHmrc(value: String): Boolean =
    value match {
      case amlsRegistrationNumberRegex(_*) => true
      case _ => false
    }

  def apply(value: String): AmlsRegistrationNumber = new AmlsRegistrationNumber(value.trim)

  implicit val format: Format[AmlsRegistrationNumber] = JsonFormatsFactory.makeValueClassFormat

}
