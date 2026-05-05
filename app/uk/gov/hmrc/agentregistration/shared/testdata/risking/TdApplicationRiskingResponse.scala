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

import uk.gov.hmrc.agentregistration.shared.PersonReference
import uk.gov.hmrc.agentregistration.shared.testdata.TdBase
import uk.gov.hmrc.agentregistration.shared.risking.RiskingProgress

import java.time.LocalDate
import java.time.format.DateTimeFormatter

trait TdApplicationRiskingResponse:
  dependencies: (TdBase & TdRiskedEntity & TdRiskedIndividual) =>

  val riskingCompletedDateString: String = "2059-12-21"
  val riskingCompletedDate: LocalDate = LocalDate.parse(riskingCompletedDateString, DateTimeFormatter.ISO_DATE)

  object applicationRiskingResponse:

    val failedFixable: RiskingProgress.FailedFixable = RiskingProgress.FailedFixable(
      riskedEntity = dependencies.riskedEntityApproved,
      riskedIndividuals = List(
        dependencies.riskedIndividualApproved(
          personReference = PersonReference("PREF0"),
          individualName = dependencies.getIndividualName(0)
        ),
        dependencies.riskedIndividualFixable(individualName = dependencies.getIndividualName(1))
      ),
      riskingCompletedDate = riskingCompletedDate
    )

    val failedNonFixable: RiskingProgress.FailedNonFixable = RiskingProgress.FailedNonFixable(
      riskedEntity = dependencies.riskedEntityFailedFixable,
      riskedIndividuals = List(
        dependencies.riskedIndividualNonFixable(
          personReference = PersonReference("PREF0"),
          individualName = dependencies.getIndividualName(0)
        ),
        dependencies.riskedIndividualFixable(
          personReference = PersonReference("PREF1"),
          individualName = dependencies.getIndividualName(1)
        )
      ),
      riskingCompletedDate = riskingCompletedDate
    )

    val failedNonFixable_failedApplicant_approvedIndividuls: RiskingProgress.FailedNonFixable = RiskingProgress.FailedNonFixable(
      riskedEntity = dependencies.riskedEntityFailedFixable,
      riskedIndividuals = List(
        dependencies.riskedIndividualApproved(
          personReference = PersonReference("PREF0"),
          individualName = dependencies.getIndividualName(0)
        ),
        dependencies.riskedIndividualApproved(
          personReference = PersonReference("PREF1"),
          individualName = dependencies.getIndividualName(1)
        )
      ),
      riskingCompletedDate = riskingCompletedDate
    )

    // TODO: more cases possible, those should be created and used in tests
