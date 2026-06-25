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

import uk.gov.hmrc.agentregistration.services.RiskingOutcomeEntityHelper.*
import uk.gov.hmrc.agentregistration.shared.risking.EntityFailure
import uk.gov.hmrc.agentregistration.shared.risking.EntityFix
import uk.gov.hmrc.agentregistration.shared.risking.RiskingOutcomeEntity
import uk.gov.hmrc.agentregistration.testsupport.UnitSpec

class RiskingOutcomeEntityHelperSpec
extends UnitSpec:

  private case class TestCase(
    description: String,
    failures: Seq[EntityFailure],
    expected: RiskingOutcomeEntity
  )

  "Seq[EntityFailure].riskingOutcomeEntity" - {

    List(
      TestCase(
        description = "empty failures yield Approved",
        failures = Seq.empty,
        expected = RiskingOutcomeEntity.Approved
      ),
      TestCase(
        description = "a single fixable failure yields FailedFixable with the corresponding fix",
        failures = Seq(EntityFailure._4._1),
        expected = RiskingOutcomeEntity.FailedFixable(fixes = Seq(EntityFix._4._1(isConfirmed = None)))
      ),
      TestCase(
        description = "multiple non-AMLS fixable failures yield FailedFixable with one fix per failure",
        failures = Seq(
          EntityFailure._4._1,
          EntityFailure._5._3,
          EntityFailure._8._7
        ),
        expected = RiskingOutcomeEntity.FailedFixable(
          fixes = Seq(
            EntityFix._4._1(isConfirmed = None),
            EntityFix._5._3(isConfirmed = None),
            EntityFix._8._7(isConfirmed = None)
          )
        )
      ),
      TestCase(
        description = "multiple AMLS failures yield FailedFixable with one AmlsFix per distinct failure",
        failures = Seq(EntityFailure._3._1, EntityFailure._3._2),
        expected = RiskingOutcomeEntity.FailedFixable(
          fixes = Seq(
            EntityFix._3.AmlsFix(
              EntityFailure._3._1,
              isConfirmed = None,
              amlsDetails = None
            ),
            EntityFix._3.AmlsFix(
              EntityFailure._3._2,
              isConfirmed = None,
              amlsDetails = None
            )
          )
        )
      ),
      TestCase(
        description = "duplicate fixable failures yield a single fix after distinct",
        failures = Seq(EntityFailure._4._1, EntityFailure._4._1),
        expected = RiskingOutcomeEntity.FailedFixable(fixes = Seq(EntityFix._4._1(isConfirmed = None)))
      ),
      TestCase(
        description = "a single non-fixable failure yields FailedNonFixable with the failure",
        failures = Seq(EntityFailure._7),
        expected = RiskingOutcomeEntity.FailedNonFixable(failures = Seq(EntityFailure._7))
      ),
      TestCase(
        description = "multiple non-fixable failures yield FailedNonFixable with all failures",
        failures = Seq(EntityFailure._7, EntityFailure._8._1),
        expected = RiskingOutcomeEntity.FailedNonFixable(failures = Seq(EntityFailure._7, EntityFailure._8._1))
      ),
      TestCase(
        description = "mix of fixable and non-fixable yields FailedNonFixable carrying ALL failures in original order",
        failures = Seq(EntityFailure._4._1, EntityFailure._7),
        expected = RiskingOutcomeEntity.FailedNonFixable(failures = Seq(EntityFailure._4._1, EntityFailure._7))
      )
    ).foreach: tc =>
      tc.description in:
        tc.failures.riskingOutcomeEntity shouldBe tc.expected
  }
