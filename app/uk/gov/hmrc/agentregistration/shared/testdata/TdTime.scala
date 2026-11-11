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

package uk.gov.hmrc.agentregistration.shared.testdata

import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/** All clock-based test data.
  *
  * Fixed in the future to prevent Mongo's expiry index deleting records. Kept in an object to prevent overrides and conflicting meanings of "overdue" in one
  * test.
  */
object TdTime:

  val zoneOffset: ZoneOffset = ZoneOffset.UTC
  val zoneId: ZoneId = ZoneId.of("UTC")

  val dateString: String = "2059-11-25"
  val timeString: String = s"${dateString}T16:33:51.880"

  val nowAsLocalDateTime: LocalDateTime = LocalDateTime.parse(timeString, DateTimeFormatter.ISO_DATE_TIME)

  val nowPlus6mAsLocalDateTime: LocalDateTime = nowAsLocalDateTime.plus(java.time.Period.ofMonths(6))
  val nowPlus13mAsLocalDateTime: LocalDateTime = nowAsLocalDateTime.plus(java.time.Period.ofMonths(13))
  val newPlus20sAsLocalDateTime: LocalDateTime = nowAsLocalDateTime.plusSeconds(20)

  /** When test data is viewed. Nothing is created then, so every `createdAt` is earlier. */
  val now: Instant = nowAsLocalDateTime.toInstant(zoneOffset)

  val nowAsLocalDate: LocalDate = nowAsLocalDateTime.toLocalDate

  val clock: Clock = Clock.fixed(now, zoneId)

trait TdTime:

  export TdTime.*

  def dateOfIncorporation: LocalDate = nowAsLocalDate.minusYears(10)
