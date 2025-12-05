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

/** Self Assessment (Sa) Unique Taxpayer Reference (Utr)
  */
final case class SaUtr(value: String):
  def asUtr: Utr = Utr(value)

object SaUtr:

  given format: Format[SaUtr] = JsonFormatsFactory.makeValueClassFormat

  private val saUtrRegex = "^[0-9]{10}$"

  def isValid(utr: String): Boolean = utr.matches(saUtrRegex)
