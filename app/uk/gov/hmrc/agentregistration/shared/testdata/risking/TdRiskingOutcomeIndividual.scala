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

package uk.gov.hmrc.agentregistration.shared.testdata.risking

import uk.gov.hmrc.agentregistration.shared.risking.IndividualFailure
import uk.gov.hmrc.agentregistration.shared.risking.RiskingOutcomeIndividual
import uk.gov.hmrc.agentregistration.shared.testdata.TdBase

trait TdRiskingOutcomeIndividual:
  dependencies: TdBase =>

  object individualRiskingOutcome:

    val failedNonFixable: RiskingOutcomeIndividual.FailedNonFixable = RiskingOutcomeIndividual.FailedNonFixable(
      failures = List(
        IndividualFailure._5._1, // fixable
        IndividualFailure._6 // non fixable
      )
    )

    val singleNonFixableFailure: RiskingOutcomeIndividual.FailedNonFixable = RiskingOutcomeIndividual.FailedNonFixable(
      failures = List(
        IndividualFailure._9 // non fixable
      )
    )
