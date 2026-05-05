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

import uk.gov.hmrc.agentregistration.testsupport.UnitSpec

class PersonReferenceGeneratorSpec
extends UnitSpec:

  "generatePersonReference should create unique references in the agreed format" in:

    val generator: PersonReferenceGenerator = new PersonReferenceGenerator
    val formatRegex: String = "^[A-HJ-NP-RTXYZ2346789]{9}$"

    val ref1: PersonReference = generator.generatePersonReference()
    val ref2: PersonReference = generator.generatePersonReference()
    ref1 should not be ref2
    ref1.value.length shouldBe 9
    ref2.value.length shouldBe 9
    ref1.value.matches(formatRegex) shouldBe true
    ref2.value.matches(formatRegex) shouldBe true
