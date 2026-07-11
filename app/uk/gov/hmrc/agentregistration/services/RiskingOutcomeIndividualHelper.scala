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

object RiskingOutcomeIndividualHelper:

  /** Constructs the initial [[IndividualFix]] for a given fixable individual failure.
    *
    * Each [[IndividualFailure._10]] (`_10._1`, `_10._2`) maps to [[IndividualFix._10.IndividualDetailsFix]] with all detail fields unset; multiple `_10`
    * failures produce identical fix instances which collapse under `.distinct` at the call site.
    *
    * All other fixable failures (4.*, 5.*, 8.7) map 1:1 to their corresponding [[IndividualFix]] subtype with `isConfirmed = None`.
    */
  extension (failure: IndividualFailure.Fixable)
    def asIndividualFix: IndividualFix =
      failure match
        case IndividualFailure._4._1 => IndividualFix._4._1(isConfirmed = None)
        case IndividualFailure._4._3 => IndividualFix._4._3(isConfirmed = None)
        case IndividualFailure._4._4 => IndividualFix._4._4(isConfirmed = None)
        case IndividualFailure._5._1 => IndividualFix._5._1(isConfirmed = None)
        case IndividualFailure._5._3 => IndividualFix._5._3(isConfirmed = None)
        case IndividualFailure._5._4 => IndividualFix._5._4(isConfirmed = None)
        case IndividualFailure._5._5 => IndividualFix._5._5(isConfirmed = None)
        case IndividualFailure._5._6 => IndividualFix._5._6(isConfirmed = None)
        case IndividualFailure._5._7 => IndividualFix._5._7(isConfirmed = None)
        case IndividualFailure._8._7 => IndividualFix._8._7(isConfirmed = None)
        case IndividualFailure._10._1 | IndividualFailure._10._2 =>
          IndividualFix._10.IndividualDetailsFix(
            dateOfBirth = None,
            saUtr = None,
            nino = None,
            isConfirmed = None
          )

  def riskingOutcomeIndividual(
    riskingOutcome: RiskingOutcome,
    individualFailures: Seq[IndividualFailure]
  ): RiskingOutcomeIndividual =
    riskingOutcome match
      case RiskingOutcome.Approved => RiskingOutcomeIndividual.Approved
      case RiskingOutcome.FailedFixable =>
        RiskingOutcomeIndividual.FailedFixable(
          fixes = individualFailures.collect { case f: IndividualFailure.Fixable => f.asIndividualFix }.distinct,
          declarationAgreed = false
        )
      case RiskingOutcome.FailedNonFixable => RiskingOutcomeIndividual.FailedNonFixable(failures = individualFailures)
