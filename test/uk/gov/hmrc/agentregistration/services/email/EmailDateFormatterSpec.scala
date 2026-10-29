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

package uk.gov.hmrc.agentregistration.services.email

import uk.gov.hmrc.agentregistration.testsupport.UnitSpec

import java.time.Instant

class EmailDateFormatterSpec
extends UnitSpec:

  "format" - {
    "renders the date as day, full month and year" in:
      EmailDateFormatter.format(Instant.parse("2026-11-19T10:00:00Z")) shouldBe "19 November 2026"

    "renders the date in UK local time" in:
      EmailDateFormatter.format(Instant.parse("2026-07-31T23:30:00Z")) shouldBe "1 August 2026"
  }
