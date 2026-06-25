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

import uk.gov.hmrc.agentregistration.shared.risking.EntityFailure
import uk.gov.hmrc.agentregistration.shared.risking.EntityFix
import uk.gov.hmrc.agentregistration.shared.risking.IndividualFailure
import uk.gov.hmrc.agentregistration.shared.risking.IndividualFix
import uk.gov.hmrc.agentregistration.shared.risking.RiskingOutcomeApplication
import uk.gov.hmrc.agentregistration.shared.risking.RiskingOutcomeEntity
import uk.gov.hmrc.agentregistration.shared.risking.RiskingOutcomeIndividual
import uk.gov.hmrc.agentregistration.testsupport.UnitSpec

class RiskingOutcomeApplicationHelperSpec
extends UnitSpec:

  private val approvedEntity: RiskingOutcomeEntity = RiskingOutcomeEntity.Approved
  private val fixableEntity: RiskingOutcomeEntity = RiskingOutcomeEntity.FailedFixable(
    fixes = Seq(EntityFix._4._1(isConfirmed = None))
  )
  private val nonFixableEntity: RiskingOutcomeEntity = RiskingOutcomeEntity.FailedNonFixable(
    failures = Seq(EntityFailure._7)
  )

  private val approvedIndividual: RiskingOutcomeIndividual = RiskingOutcomeIndividual.Approved
  private val fixableIndividual: RiskingOutcomeIndividual = RiskingOutcomeIndividual.FailedFixable(
    fixes = Seq(IndividualFix._4._1(isConfirmed = None))
  )
  private val nonFixableIndividual: RiskingOutcomeIndividual = RiskingOutcomeIndividual.FailedNonFixable(
    failures = Seq(IndividualFailure._6)
  )

  private case class TestCase(
    description: String,
    entity: RiskingOutcomeEntity,
    individuals: Seq[RiskingOutcomeIndividual],
    expected: RiskingOutcomeApplication.Outcome
  )

  "RiskingOutcomeApplicationHelper.riskingOutcomeApplicationOutcome" - {

    List(
      TestCase(
        description = "entity Approved with no individuals yields Approved",
        entity = approvedEntity,
        individuals = Seq.empty,
        expected = RiskingOutcomeApplication.Outcome.Approved
      ),
      TestCase(
        description = "entity Approved with all individuals Approved yields Approved",
        entity = approvedEntity,
        individuals = Seq(approvedIndividual, approvedIndividual),
        expected = RiskingOutcomeApplication.Outcome.Approved
      ),
      TestCase(
        description = "entity Approved with one fixable individual yields FailedFixable",
        entity = approvedEntity,
        individuals = Seq(approvedIndividual, fixableIndividual),
        expected = RiskingOutcomeApplication.Outcome.FailedFixable
      ),
      TestCase(
        description = "entity Approved with one non-fixable individual yields FailedNonFixable",
        entity = approvedEntity,
        individuals = Seq(approvedIndividual, nonFixableIndividual),
        expected = RiskingOutcomeApplication.Outcome.FailedNonFixable
      ),
      TestCase(
        description = "fixable entity with all individuals Approved yields FailedFixable",
        entity = fixableEntity,
        individuals = Seq(approvedIndividual),
        expected = RiskingOutcomeApplication.Outcome.FailedFixable
      ),
      TestCase(
        description = "fixable entity with one non-fixable individual yields FailedNonFixable",
        entity = fixableEntity,
        individuals = Seq(nonFixableIndividual),
        expected = RiskingOutcomeApplication.Outcome.FailedNonFixable
      ),
      TestCase(
        description = "non-fixable entity with all individuals Approved yields FailedNonFixable",
        entity = nonFixableEntity,
        individuals = Seq(approvedIndividual),
        expected = RiskingOutcomeApplication.Outcome.FailedNonFixable
      ),
      TestCase(
        description = "non-fixable entity with fixable individuals yields FailedNonFixable",
        entity = nonFixableEntity,
        individuals = Seq(fixableIndividual, fixableIndividual),
        expected = RiskingOutcomeApplication.Outcome.FailedNonFixable
      )
    ).foreach: tc =>
      tc.description in:
        RiskingOutcomeApplicationHelper.riskingOutcomeApplicationOutcome(tc.entity, tc.individuals) shouldBe tc.expected
  }
