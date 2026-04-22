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
import uk.gov.hmrc.agentregistration.shared.risking.ApplicationRiskingResponse
import uk.gov.hmrc.agentregistration.shared.risking.EntityFailure
import uk.gov.hmrc.agentregistration.shared.risking.IndividualFailure
import uk.gov.hmrc.agentregistration.shared.testdata.TdBase

trait TdApplicationRiskingResponse:
  dependencies: (TdBase & TdIndividualRiskingResponse) =>

  object applicationRiskingResponse:

    val readyForSubmissionResponse: ApplicationRiskingResponse = ApplicationRiskingResponse(
      applicationReference = dependencies.applicationReference,
      status = ApplicationForRiskingStatus.ReadyForSubmission,
      individuals = List(
        individualRiskingResponse.readyForSubmissionResponse,
        individualRiskingResponse.readyForSubmissionResponse.copy(
          providedName = dependencies.getIndividualName(1),
          personReference = PersonReference("any-id")
        )
      ),
      failures = None
    )

    val submittedForRiskingResponse: ApplicationRiskingResponse = ApplicationRiskingResponse(
      applicationReference = dependencies.applicationReference,
      status = ApplicationForRiskingStatus.SubmittedForRisking,
      individuals = List(
        individualRiskingResponse.submittedForRiskingResponse,
        individualRiskingResponse.submittedForRiskingResponse.copy(
          personReference = PersonReference("any-id")
        )
      ),
      failures = None
    )

    val failedNonFixableResponse: ApplicationRiskingResponse = ApplicationRiskingResponse(
      applicationReference = dependencies.applicationReference,
      status = FailedNonFixable,
      individuals = List(
        individualRiskingResponse.failedNonFixableResponse,
        individualRiskingResponse.failedNonFixableResponse.copy(
          providedName = dependencies.getIndividualName(1),
          personReference = PersonReference("test-individual-2"),
          failures = Some(List(
            IndividualFailure._4._1
          ))
        )
      ),
      failures = Some(List(
        EntityFailure._4._1,
        EntityFailure._4._3,
        EntityFailure._4._4
      ))
    )
