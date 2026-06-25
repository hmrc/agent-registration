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
import uk.gov.hmrc.agentregistration.shared.risking.RiskingOutcomeEntity

object RiskingOutcomeEntityHelper:

  /** Constructs the initial [[EntityFix]] for a given fixable entity failure.
    *
    * Each [[EntityFailure.IsAmls]] (3.1–3.5) maps to [[EntityFix._3.AmlsFix]] carrying the originating failure; multiple AMLS failures yield multiple `AmlsFix`
    * instances, each preserving its own `failure` field.
    *
    * All other fixable failures (4.*, 5.*, 8.5, 8.7) map 1:1 to their corresponding [[EntityFix]] subtype with `isConfirmed = None`.
    */
  extension (failure: EntityFailure.Fixable)
    def asEntityFix: EntityFix =
      failure match
        case f: EntityFailure.IsAmls =>
          EntityFix._3.AmlsFix(
            f,
            isConfirmed = None,
            amlsDetails = None
          )
        case EntityFailure._4._1 => EntityFix._4._1(isConfirmed = None)
        case EntityFailure._4._2 => EntityFix._4._2(isConfirmed = None)
        case EntityFailure._4._3 => EntityFix._4._3(isConfirmed = None)
        case EntityFailure._4._4 => EntityFix._4._4(isConfirmed = None)
        case EntityFailure._5._1 => EntityFix._5._1(isConfirmed = None)
        case EntityFailure._5._2 => EntityFix._5._2(isConfirmed = None)
        case EntityFailure._5._3 => EntityFix._5._3(isConfirmed = None)
        case EntityFailure._5._4 => EntityFix._5._4(isConfirmed = None)
        case EntityFailure._5._5 => EntityFix._5._5(isConfirmed = None)
        case EntityFailure._5._6 => EntityFix._5._6(isConfirmed = None)
        case EntityFailure._5._7 => EntityFix._5._7(isConfirmed = None)
        case EntityFailure._8._5 => EntityFix._8._5(isConfirmed = None)
        case EntityFailure._8._7 => EntityFix._8._7(isConfirmed = None)

  extension (failure: EntityFailure)
    def asRiskingOutcomeEntity: RiskingOutcomeEntity =
      failure match
        case f: EntityFailure.Fixable => RiskingOutcomeEntity.FailedFixable(fixes = Seq(f.asEntityFix))
        case _: EntityFailure.NonFixable => RiskingOutcomeEntity.FailedNonFixable(failures = Seq(failure))

  extension (failures: Seq[EntityFailure])
    def riskingOutcomeEntity: RiskingOutcomeEntity =
      failures
        .map(_.asRiskingOutcomeEntity)
        .foldLeft[RiskingOutcomeEntity](RiskingOutcomeEntity.Approved)(foldRiskingOutcomeEntity) match
        case f: RiskingOutcomeEntity.FailedFixable => f.copy(fixes = f.fixes.distinct)
        case other => other

  def foldRiskingOutcomeEntity(
    o1: RiskingOutcomeEntity,
    o2: RiskingOutcomeEntity
  ): RiskingOutcomeEntity =
    import RiskingOutcomeEntity.*
    (o1, o2) match
      case (Approved, x) => x
      case (x, Approved) => x
      case (FailedFixable(f1), FailedFixable(f2)) => FailedFixable(fixes = f1 ++ f2)
      case (_: FailedFixable, nf: FailedNonFixable) => nf
      case (nf: FailedNonFixable, _: FailedFixable) => nf
      case (FailedNonFixable(f1), FailedNonFixable(f2)) => FailedNonFixable(failures = f1 ++ f2)
