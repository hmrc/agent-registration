/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.agentregistration.testsupport.testdata

import uk.gov.hmrc.agentregistration.shared.CompanyProfile
import uk.gov.hmrc.agentregistration.shared.FullName
import uk.gov.hmrc.agentregistration.shared.GroupId
import uk.gov.hmrc.agentregistration.shared.InternalUserId
import uk.gov.hmrc.agentregistration.shared.Utr

import java.time.format.DateTimeFormatter
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

trait TdBase:

  lazy val dateString: String = "2059-11-25"
  lazy val timeString: String = s"${dateString}T16:33:51.880"
  lazy val localDateTime: LocalDateTime =
    // the frozen time has to be in future otherwise the applications will disappear from mongodb because of expiry index
    LocalDateTime.parse(timeString, DateTimeFormatter.ISO_DATE_TIME)
  lazy val instant: Instant = localDateTime.toInstant(ZoneOffset.UTC)
  lazy val newInstant: Instant = instant.plusSeconds(20) // used when a new application is created from existing one

  lazy val utr: Utr = Utr("1234567895")
  lazy val internalUserId: InternalUserId = InternalUserId("internal-user-id-12345")
  lazy val groupId: GroupId = GroupId("group-id-12345")
  lazy val nino: String = "AB123456C"
  lazy val safeId: String = "X00000123456789"
  lazy val dateOfBirth: LocalDate = LocalDate.of(2000, 1, 1)
  lazy val firstName: String = "Test"
  lazy val lastName: String = "Name"
  lazy val fullName = FullName(firstName, lastName)
  lazy val companyNumber: String = "1234567890"
  lazy val companyName: String = "Test Company Name"
  lazy val companyProfile: CompanyProfile = CompanyProfile(companyNumber, companyName)
  lazy val postcode: String = "AA1 1AA"
