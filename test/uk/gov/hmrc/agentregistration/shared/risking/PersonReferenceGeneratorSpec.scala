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

package uk.gov.hmrc.agentregistration.shared.risking

import uk.gov.hmrc.agentregistration.testsupport.UnitSpec

class PersonReferenceGeneratorSpec extends UnitSpec:

  "PersonReferenceGenerator.nextPersonReference" - {

    "should generate a reference in 4 letters + 4 digits format" in {
      val generator = PersonReferenceGenerator()

      val generated = generator.nextPersonReference().value

      generated should have length 8
      generated.take(4) should fullyMatch regex "[A-Z]{4}"
      generated.drop(4) should fullyMatch regex "[0-9]{4}"
    }
  }
