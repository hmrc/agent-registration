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
import uk.gov.hmrc.agentregistration.shared.risking.ApplicationForRiskingStatus
import uk.gov.hmrc.agentregistration.shared.risking.ApplicationForRiskingStatus.FailedNonFixable
import uk.gov.hmrc.agentregistration.shared.risking.IndividualFailure
import uk.gov.hmrc.agentregistration.shared.risking.IndividualRiskingResponse
import uk.gov.hmrc.agentregistration.shared.testdata.TdBase

trait TdIndividualRiskingResponse:
  dependencies: (TdBase) =>

  object individualRiskingResponse:

    val readyForSubmissionResponse: IndividualRiskingResponse = IndividualRiskingResponse(
      personReference = PersonReference(dependencies.individualProvidedDetailsId.value),
      providedName = dependencies.getIndividualName(0),
      status = ApplicationForRiskingStatus.ReadyForSubmission,
      failures = None
    )

    val submittedForRiskingResponse: IndividualRiskingResponse = readyForSubmissionResponse.copy(
      status = ApplicationForRiskingStatus.SubmittedForRisking
    )

    val failedNonFixableResponse: IndividualRiskingResponse = readyForSubmissionResponse.copy(
      status = FailedNonFixable,
      failures = Some(List(
        IndividualFailure._5._1(325),
        IndividualFailure._6
      ))
    )
