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

import uk.gov.hmrc.agentregistration.shared.amls.AmlsDetails
import uk.gov.hmrc.agentregistration.shared.risking.EntityFailure
import uk.gov.hmrc.agentregistration.shared.risking.EntityFix
import uk.gov.hmrc.agentregistration.shared.risking.RiskingOutcome
import uk.gov.hmrc.agentregistration.shared.risking.RiskingOutcomeEntity
import uk.gov.hmrc.agentregistration.testsupport.testdata.TdAll
import uk.gov.hmrc.agentregistration.testsupport.UnitSpec

class RiskingOutcomeEntityHelperSpec
extends UnitSpec:

  private val existingAmlsDetails: AmlsDetails = TdAll.tdAll.completeAmlsDetails

  private case class TestCase(
    description: String,
    riskingOutcome: RiskingOutcome,
    entityFailures: Seq[EntityFailure],
    existingAmlsDetails: Option[AmlsDetails],
    expected: RiskingOutcomeEntity
  )

  "RiskingOutcomeEntityHelper.riskingOutcomeEntity(riskingOutcome, entityFailures, existingAmlsDetails)" - {

    List(
      TestCase(
        description = "Approved riskingOutcome with empty entity failures yields RiskingOutcomeEntity.Approved",
        riskingOutcome = RiskingOutcome.Approved,
        entityFailures = Seq.empty,
        existingAmlsDetails = Some(existingAmlsDetails),
        expected = RiskingOutcomeEntity.Approved
      ),
      TestCase(
        description = "FailedFixable riskingOutcome with a single fixable entity failure yields FailedFixable with the corresponding fix",
        riskingOutcome = RiskingOutcome.FailedFixable,
        entityFailures = Seq(EntityFailure._4._1),
        existingAmlsDetails = Some(existingAmlsDetails),
        expected = RiskingOutcomeEntity.FailedFixable(fixes = Seq(EntityFix._4._1(isConfirmed = None)))
      ),
      TestCase(
        description = "FailedFixable riskingOutcome with multiple non-AMLS fixable entity failures yields FailedFixable with one fix per failure",
        riskingOutcome = RiskingOutcome.FailedFixable,
        entityFailures = Seq(
          EntityFailure._4._1,
          EntityFailure._5._3,
          EntityFailure._8._7
        ),
        existingAmlsDetails = Some(existingAmlsDetails),
        expected = RiskingOutcomeEntity.FailedFixable(
          fixes = Seq(
            EntityFix._4._1(isConfirmed = None),
            EntityFix._5._3(isConfirmed = None),
            EntityFix._8._7(isConfirmed = None)
          )
        )
      ),
      TestCase(
        description = "FailedFixable riskingOutcome with an AMLS entity failure pre-populates AmlsFix.amlsDetails from the existing application AMLS details",
        riskingOutcome = RiskingOutcome.FailedFixable,
        entityFailures = Seq(EntityFailure._3._1),
        existingAmlsDetails = Some(existingAmlsDetails),
        expected = RiskingOutcomeEntity.FailedFixable(
          fixes = Seq(
            EntityFix._3.AmlsFix(
              EntityFailure._3._1,
              isConfirmed = None,
              amlsDetails = Some(existingAmlsDetails)
            )
          )
        )
      ),
      TestCase(
        description =
          "FailedFixable riskingOutcome with multiple AMLS entity failures yields one AmlsFix per distinct failure, each pre-populated with existing AMLS details",
        riskingOutcome = RiskingOutcome.FailedFixable,
        entityFailures = Seq(EntityFailure._3._1, EntityFailure._3._2),
        existingAmlsDetails = Some(existingAmlsDetails),
        expected = RiskingOutcomeEntity.FailedFixable(
          fixes = Seq(
            EntityFix._3.AmlsFix(
              EntityFailure._3._1,
              isConfirmed = None,
              amlsDetails = Some(existingAmlsDetails)
            ),
            EntityFix._3.AmlsFix(
              EntityFailure._3._2,
              isConfirmed = None,
              amlsDetails = Some(existingAmlsDetails)
            )
          )
        )
      ),
      TestCase(
        description = "FailedFixable riskingOutcome with an AMLS entity failure and no existing application AMLS details carries None through unchanged",
        riskingOutcome = RiskingOutcome.FailedFixable,
        entityFailures = Seq(EntityFailure._3._1),
        existingAmlsDetails = None,
        expected = RiskingOutcomeEntity.FailedFixable(
          fixes = Seq(
            EntityFix._3.AmlsFix(
              EntityFailure._3._1,
              isConfirmed = None,
              amlsDetails = None
            )
          )
        )
      ),
      TestCase(
        description = "FailedFixable riskingOutcome with duplicate fixable entity failures yields a single fix after distinct",
        riskingOutcome = RiskingOutcome.FailedFixable,
        entityFailures = Seq(EntityFailure._4._1, EntityFailure._4._1),
        existingAmlsDetails = Some(existingAmlsDetails),
        expected = RiskingOutcomeEntity.FailedFixable(fixes = Seq(EntityFix._4._1(isConfirmed = None)))
      ),
      TestCase(
        description = "FailedNonFixable riskingOutcome with a single non-fixable entity failure yields FailedNonFixable with the failure",
        riskingOutcome = RiskingOutcome.FailedNonFixable,
        entityFailures = Seq(EntityFailure._7),
        existingAmlsDetails = Some(existingAmlsDetails),
        expected = RiskingOutcomeEntity.FailedNonFixable(failures = Seq(EntityFailure._7))
      ),
      TestCase(
        description = "FailedNonFixable riskingOutcome with multiple non-fixable entity failures yields FailedNonFixable with all failures",
        riskingOutcome = RiskingOutcome.FailedNonFixable,
        entityFailures = Seq(EntityFailure._7, EntityFailure._8._1),
        existingAmlsDetails = Some(existingAmlsDetails),
        expected = RiskingOutcomeEntity.FailedNonFixable(failures = Seq(EntityFailure._7, EntityFailure._8._1))
      ),
      TestCase(
        description =
          "FailedNonFixable riskingOutcome with a mix of fixable and non-fixable entity failures yields FailedNonFixable carrying ALL failures in original order",
        riskingOutcome = RiskingOutcome.FailedNonFixable,
        entityFailures = Seq(EntityFailure._4._1, EntityFailure._7),
        existingAmlsDetails = Some(existingAmlsDetails),
        expected = RiskingOutcomeEntity.FailedNonFixable(failures = Seq(EntityFailure._4._1, EntityFailure._7))
      )
    ).foreach: tc =>
      tc.description in:
        RiskingOutcomeEntityHelper.riskingOutcomeEntity(
          tc.riskingOutcome,
          tc.entityFailures,
          tc.existingAmlsDetails
        ) shouldBe tc.expected
  }
