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

import uk.gov.hmrc.agentregistration.shared.risking.RiskingOutcomeApplication
import uk.gov.hmrc.agentregistration.shared.risking.RiskingOutcomeEntity
import uk.gov.hmrc.agentregistration.shared.risking.RiskingOutcomeIndividual

object RiskingOutcomeApplicationHelper:

  def riskingOutcomeApplicationOutcome(
    entity: RiskingOutcomeEntity,
    individuals: Seq[RiskingOutcomeIndividual]
  ): RiskingOutcomeApplication.Outcome =
    (asOutcome(entity) +: individuals.map(asOutcome))
      .foldLeft[RiskingOutcomeApplication.Outcome](RiskingOutcomeApplication.Outcome.Approved)(foldRiskingOutcomeApplicationOutcome)

  private def asOutcome(entity: RiskingOutcomeEntity): RiskingOutcomeApplication.Outcome =
    entity match
      case RiskingOutcomeEntity.Approved => RiskingOutcomeApplication.Outcome.Approved
      case _: RiskingOutcomeEntity.FailedFixable => RiskingOutcomeApplication.Outcome.FailedFixable
      case _: RiskingOutcomeEntity.FailedNonFixable => RiskingOutcomeApplication.Outcome.FailedNonFixable

  private def asOutcome(individual: RiskingOutcomeIndividual): RiskingOutcomeApplication.Outcome =
    individual match
      case RiskingOutcomeIndividual.Approved => RiskingOutcomeApplication.Outcome.Approved
      case _: RiskingOutcomeIndividual.FailedFixable => RiskingOutcomeApplication.Outcome.FailedFixable
      case _: RiskingOutcomeIndividual.FailedNonFixable => RiskingOutcomeApplication.Outcome.FailedNonFixable

  def foldRiskingOutcomeApplicationOutcome(
    o1: RiskingOutcomeApplication.Outcome,
    o2: RiskingOutcomeApplication.Outcome
  ): RiskingOutcomeApplication.Outcome =
    import RiskingOutcomeApplication.Outcome.*
    (o1, o2) match
      case (Approved, x) => x
      case (x, Approved) => x
      case (FailedFixable, FailedFixable) => FailedFixable
      case (FailedFixable, FailedNonFixable) => FailedNonFixable
      case (FailedNonFixable, FailedFixable) => FailedNonFixable
      case (FailedNonFixable, FailedNonFixable) => FailedNonFixable
