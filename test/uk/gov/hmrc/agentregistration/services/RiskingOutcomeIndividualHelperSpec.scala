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

import uk.gov.hmrc.agentregistration.shared.risking.IndividualFailure
import uk.gov.hmrc.agentregistration.shared.risking.IndividualFix
import uk.gov.hmrc.agentregistration.shared.risking.RiskingOutcome
import uk.gov.hmrc.agentregistration.shared.risking.RiskingOutcomeIndividual
import uk.gov.hmrc.agentregistration.testsupport.UnitSpec

class RiskingOutcomeIndividualHelperSpec
extends UnitSpec:

  private case class TestCase(
    description: String,
    riskingOutcome: RiskingOutcome,
    individualFailures: Seq[IndividualFailure],
    expected: RiskingOutcomeIndividual
  )

  "RiskingOutcomeIndividualHelper.riskingOutcomeIndividual(riskingOutcome, individualFailures)" - {

    List(
      TestCase(
        description = "Approved riskingOutcome with empty individual failures yields RiskingOutcomeIndividual.Approved",
        riskingOutcome = RiskingOutcome.Approved,
        individualFailures = Seq.empty,
        expected = RiskingOutcomeIndividual.Approved
      ),
      TestCase(
        description = "FailedFixable riskingOutcome with a single fixable individual failure yields FailedFixable with the corresponding fix",
        riskingOutcome = RiskingOutcome.FailedFixable,
        individualFailures = Seq(IndividualFailure._4._1),
        expected = RiskingOutcomeIndividual.FailedFixable(
          fixes = Seq(IndividualFix._4._1(isConfirmed = None)),
          declarationAgreed = false
        )
      ),
      TestCase(
        description = "FailedFixable riskingOutcome with multiple non-_10 fixable individual failures yields FailedFixable with one fix per failure",
        riskingOutcome = RiskingOutcome.FailedFixable,
        individualFailures = Seq(
          IndividualFailure._4._1,
          IndividualFailure._5._3,
          IndividualFailure._8._7
        ),
        expected = RiskingOutcomeIndividual.FailedFixable(
          fixes = Seq(
            IndividualFix._4._1(isConfirmed = None),
            IndividualFix._5._3(isConfirmed = None),
            IndividualFix._8._7(isConfirmed = None)
          ),
          declarationAgreed = false
        )
      ),
      TestCase(
        description = "FailedFixable riskingOutcome with both _10 individual failures collapses to a single IndividualDetailsFix after distinct",
        riskingOutcome = RiskingOutcome.FailedFixable,
        individualFailures = Seq(IndividualFailure._10._1, IndividualFailure._10._2),
        expected = RiskingOutcomeIndividual.FailedFixable(
          fixes = Seq(
            IndividualFix._10.IndividualDetailsFix(
              dateOfBirth = None,
              saUtr = None,
              nino = None,
              isConfirmed = None
            )
          ),
          declarationAgreed = false
        )
      ),
      TestCase(
        description = "FailedFixable riskingOutcome with duplicate fixable individual failures yields a single fix after distinct",
        riskingOutcome = RiskingOutcome.FailedFixable,
        individualFailures = Seq(IndividualFailure._5._1, IndividualFailure._5._1),
        expected = RiskingOutcomeIndividual.FailedFixable(
          fixes = Seq(IndividualFix._5._1(isConfirmed = None)),
          declarationAgreed = false
        )
      ),
      TestCase(
        description = "FailedNonFixable riskingOutcome with a single non-fixable individual failure yields FailedNonFixable with the failure",
        riskingOutcome = RiskingOutcome.FailedNonFixable,
        individualFailures = Seq(IndividualFailure._6),
        expected = RiskingOutcomeIndividual.FailedNonFixable(failures = Seq(IndividualFailure._6))
      ),
      TestCase(
        description = "FailedNonFixable riskingOutcome with multiple non-fixable individual failures yields FailedNonFixable with all failures",
        riskingOutcome = RiskingOutcome.FailedNonFixable,
        individualFailures = Seq(IndividualFailure._6, IndividualFailure._9),
        expected = RiskingOutcomeIndividual.FailedNonFixable(failures = Seq(IndividualFailure._6, IndividualFailure._9))
      ),
      TestCase(
        description =
          "FailedNonFixable riskingOutcome with a mix of fixable and non-fixable individual failures yields FailedNonFixable carrying ALL failures in original order",
        riskingOutcome = RiskingOutcome.FailedNonFixable,
        individualFailures = Seq(IndividualFailure._4._1, IndividualFailure._6),
        expected = RiskingOutcomeIndividual.FailedNonFixable(failures = Seq(IndividualFailure._4._1, IndividualFailure._6))
      )
    ).foreach: tc =>
      tc.description in:
        RiskingOutcomeIndividualHelper.riskingOutcomeIndividual(tc.riskingOutcome, tc.individualFailures) shouldBe tc.expected
  }
