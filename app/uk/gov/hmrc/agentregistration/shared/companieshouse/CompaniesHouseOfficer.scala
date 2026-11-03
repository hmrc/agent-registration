/*
 * Copyright 2024 HM Revenue & Customs
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

import play.api.libs.json.*

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import scala.util.matching.Regex

final case class CompaniesHouseOfficer(
  name: String,
  dateOfBirth: Option[CompaniesHouseDateOfBirth],
  resignedOn: Option[LocalDate],
  officerRole: Option[CompaniesHouseOfficerRole],
  identification: Option[CompaniesHouseOfficerIdentification]
)

object CompaniesHouseOfficer:

  private val dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd")

  private given Format[LocalDate] = Format(
    Reads.localDateReads("yyyy-MM-dd"),
    Writes.temporalWrites[LocalDate, DateTimeFormatter](dateFormat)
  )

  given Format[CompaniesHouseOfficer] = Json.format[CompaniesHouseOfficer]

  def normaliseOfficerName(raw: String): String =
    val trimmed = raw.trim

    if trimmed.contains(",") then
      // Format: SURNAME, Forename(s), Title (Not used in matching, Optional, e.g. Mr.)
      val parts = trimmed.split(",").map(_.trim)
      parts.toList match
        case surname :: forenames :: _ => s"${titleCase(forenames)} ${titleCase(surname)}"
        case _ => titleCase(trimmed)
    else
      // Already likely "Forename Surname"
      titleCase(trimmed)

  private val namePartDelimiter: Regex = "(?<=^|[\\s.'’-])(\\p{L})".r
  // this ensures names like O'CONNOR do not come out as O'connor, but rather O'Connor, and similarly for hyphenated names like Smith-Jones
  // the only companies house officer names we support are for individuals, so we don't need to worry about corporate acronyms like "ACME UK LTD" or "ACME LIMITED"
  private def titleCase(s: String): String = namePartDelimiter.replaceAllIn(s.toLowerCase, m => m.group(1).toUpperCase)
