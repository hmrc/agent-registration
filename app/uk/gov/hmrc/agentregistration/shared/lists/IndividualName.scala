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
import uk.gov.hmrc.agentregistration.shared.util.JsonFormatsFactory

final case class IndividualName(
  value: String
):

  /** Regex using Unicode Property Escapes: \p{L} = Letters, \p{M} = Diacritics/Marks, \s = Spaces Includes standard apostrophe, right smart quote, hyphens, and
    * periods Supports names with diacritics, e.g., José, Chloë, François, etc. Examples of valid names:
    *   - John Doe
    *   - Anne-Marie O'Neill
    *   - José Ángel
    *   - Chloë Sévigny
    *   - François L'Écuyer
    *   - O'Connor
    *   - Jean-Luc Picard
    *   - Mary Jane Watson-Parker
    *   - Dr. John A. Smith
    *   - Jean-Pierre
    *   - Løvring
    */
  private val NameRegex = """^[\p{L}\p{M}\s'\u2019\-.]+$""".r
  def isValidName: Boolean = value.matches(NameRegex.regex)

object IndividualName:
  given format: Format[IndividualName] = JsonFormatsFactory.makeValueClassFormat
