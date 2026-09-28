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

import uk.gov.hmrc.agentregistration.shared.risking.EntityFailure
import uk.gov.hmrc.agentregistration.shared.risking.RiskingOutcomeEntity
import uk.gov.hmrc.agentregistration.shared.testdata.TdBase

trait TdRiskingOutcomeEntity:
  dependencies: (TdBase) =>

  object entityRiskingOutcome:

    val approved: RiskingOutcomeEntity = RiskingOutcomeEntity.Approved

    val failedNonFixable: RiskingOutcomeEntity.FailedNonFixable = RiskingOutcomeEntity.FailedNonFixable(
      failures = List(
        EntityFailure._4._1, // fixable
        EntityFailure._7 // non fixable
      )
    )

    val singleNonFixableFailure: RiskingOutcomeEntity.FailedNonFixable = RiskingOutcomeEntity.FailedNonFixable(
      failures = List(
        EntityFailure._7 // non fixable
      )
    )

    val failedNonFixableWithDuplicates: RiskingOutcomeEntity.FailedNonFixable = RiskingOutcomeEntity.FailedNonFixable(
      failures = List(
        EntityFailure._4._1, // fixable
        EntityFailure._8._4, // non fixable
        EntityFailure._8._6 // non fixable
      )
    )
