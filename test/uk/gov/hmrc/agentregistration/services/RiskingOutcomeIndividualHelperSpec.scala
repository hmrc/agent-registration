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

import uk.gov.hmrc.agentregistration.services.RiskingOutcomeIndividualHelper.*
import uk.gov.hmrc.agentregistration.shared.risking.IndividualFailure
import uk.gov.hmrc.agentregistration.shared.risking.IndividualFix
import uk.gov.hmrc.agentregistration.shared.risking.RiskingOutcomeIndividual
import uk.gov.hmrc.agentregistration.testsupport.UnitSpec

class RiskingOutcomeIndividualHelperSpec
extends UnitSpec:

  private case class TestCase(
    description: String,
    failures: Seq[IndividualFailure],
    expected: RiskingOutcomeIndividual
  )

  "Seq[IndividualFailure].riskingOutcomeIndividual" - {

    List(
      TestCase(
        description = "empty failures yield Approved",
        failures = Seq.empty,
        expected = RiskingOutcomeIndividual.Approved
      ),
      TestCase(
        description = "a single fixable failure yields FailedFixable with the corresponding fix",
        failures = Seq(IndividualFailure._4._1),
        expected = RiskingOutcomeIndividual.FailedFixable(fixes = Seq(IndividualFix._4._1(isConfirmed = None)))
      ),
      TestCase(
        description = "multiple non-_10 fixable failures yield FailedFixable with one fix per failure",
        failures = Seq(
          IndividualFailure._4._1,
          IndividualFailure._5._3,
          IndividualFailure._8._7
        ),
        expected = RiskingOutcomeIndividual.FailedFixable(
          fixes = Seq(
            IndividualFix._4._1(isConfirmed = None),
            IndividualFix._5._3(isConfirmed = None),
            IndividualFix._8._7(isConfirmed = None)
          )
        )
      ),
      TestCase(
        description = "both _10 failures collapse to a single IndividualDetailsFix after distinct",
        failures = Seq(IndividualFailure._10._1, IndividualFailure._10._2),
        expected = RiskingOutcomeIndividual.FailedFixable(
          fixes = Seq(
            IndividualFix._10.IndividualDetailsFix(
              dateOfBirth = None,
              saUtr = None,
              nino = None,
              isConfirmed = None
            )
          )
        )
      ),
      TestCase(
        description = "duplicate fixable failures yield a single fix after distinct",
        failures = Seq(IndividualFailure._5._1, IndividualFailure._5._1),
        expected = RiskingOutcomeIndividual.FailedFixable(fixes = Seq(IndividualFix._5._1(isConfirmed = None)))
      ),
      TestCase(
        description = "a single non-fixable failure yields FailedNonFixable with the failure",
        failures = Seq(IndividualFailure._6),
        expected = RiskingOutcomeIndividual.FailedNonFixable(failures = Seq(IndividualFailure._6))
      ),
      TestCase(
        description = "multiple non-fixable failures yield FailedNonFixable with all failures",
        failures = Seq(IndividualFailure._6, IndividualFailure._9),
        expected = RiskingOutcomeIndividual.FailedNonFixable(failures = Seq(IndividualFailure._6, IndividualFailure._9))
      ),
      TestCase(
        description = "mix of fixable and non-fixable yields FailedNonFixable carrying ALL failures in original order",
        failures = Seq(IndividualFailure._4._1, IndividualFailure._6),
        expected = RiskingOutcomeIndividual.FailedNonFixable(failures = Seq(IndividualFailure._4._1, IndividualFailure._6))
      )
    ).foreach: tc =>
      tc.description in:
        tc.failures.riskingOutcomeIndividual shouldBe tc.expected
  }
