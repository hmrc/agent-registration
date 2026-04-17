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

package uk.gov.hmrc.agentregistration.shared

import play.api.libs.json.Format
import play.api.mvc.PathBindable
import uk.gov.hmrc.agentregistration.shared.util.JsonFormatsFactory
import uk.gov.hmrc.agentregistration.shared.util.ValueClassBinder

/** Application Reference used by Minerva as a unique identifier for an Entity(application)
  */
final case class ApplicationReference(value: String)

object ApplicationReference:

  given format: Format[ApplicationReference] = JsonFormatsFactory.makeValueClassFormat
  given pathBindable: PathBindable[ApplicationReference] = ValueClassBinder.valueClassBinder[ApplicationReference](_.value)

  val validCharacters: List[Char] = {
    val allowedLetters = ('A' to 'Z').toList.diff(List(
      'I',
      'O',
      'S',
      'U',
      'V',
      'W'
    ))
    val allowedDigits = ('0' to '9').toList.diff(List('0', '1', '5'))
    allowedLetters ::: allowedDigits
  }

  val validLength: Int = 9
