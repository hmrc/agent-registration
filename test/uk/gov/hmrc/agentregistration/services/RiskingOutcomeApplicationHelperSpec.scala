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

package uk.gov.hmrc.agentregistration.services

import uk.gov.hmrc.agentregistration.shared.risking.RiskingOutcome
import uk.gov.hmrc.agentregistration.shared.risking.RiskingOutcomeApplication
import uk.gov.hmrc.agentregistration.testsupport.UnitSpec

class RiskingOutcomeApplicationHelperSpec
extends UnitSpec:

  private case class TestCase(
    description: String,
    input: RiskingOutcome,
    expected: RiskingOutcomeApplication.Outcome
  )

  "RiskingOutcomeApplicationHelper.riskingOutcomeApplicationOutcome maps each wire RiskingOutcome to the corresponding RiskingOutcomeApplication.Outcome" - {

    List(
      TestCase(
        "Approved",
        RiskingOutcome.Approved,
        RiskingOutcomeApplication.Outcome.Approved
      ),
      TestCase(
        "FailedFixable",
        RiskingOutcome.FailedFixable,
        RiskingOutcomeApplication.Outcome.FailedFixable
      ),
      TestCase(
        "FailedNonFixable",
        RiskingOutcome.FailedNonFixable,
        RiskingOutcomeApplication.Outcome.FailedNonFixable
      )
    ).foreach: tc =>
      tc.description in:
        RiskingOutcomeApplicationHelper.riskingOutcomeApplicationOutcome(tc.input) shouldBe tc.expected
  }
