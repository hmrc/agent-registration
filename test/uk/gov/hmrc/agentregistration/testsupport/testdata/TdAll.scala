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

import uk.gov.hmrc.agentregistration.shared.Utr
import uk.gov.hmrc.agentregistration.shared.testdata.TestOnlyData

import java.time.Instant

object TdAll:

  def apply(): TdAll = new TdAll {}

  val tdAll: TdAll = new TdAll {}

/** TestData (Td), All instances. Extends shared TestOnlyData (source of truth) plus agent-registration-specific traits.
  */
trait TdAll
extends TestOnlyData,
  TdRequest:

  // Backward-compatible aliases for properties renamed/removed in shared
  lazy val instant: Instant = nowAsInstant
  lazy val newInstant: Instant = nowAsInstant.plusSeconds(20)
  lazy val utr: Utr = Utr(saUtr.value)
  lazy val email: String = "test@example.com"
