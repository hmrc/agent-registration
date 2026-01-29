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

package uk.gov.hmrc.agentregistration.testsupport.testdata

import uk.gov.hmrc.agentregistration.shared.AgentApplicationLlp
import uk.gov.hmrc.agentregistration.shared.ApplicationState
import uk.gov.hmrc.agentregistration.shared.InternalUserId
import uk.gov.hmrc.agentregistration.shared.StateOfAgreement
import uk.gov.hmrc.agentregistration.shared.UserRole

import java.time.Instant

trait TdAgentApplication { dependencies: TdBase =>

  private val createdAt: Instant = dependencies.instant

  val llpApplicationAfterCreated: AgentApplicationLlp = AgentApplicationLlp(
    _id = agentApplicationId,
    internalUserId = internalUserId,
    linkId = linkId,
    groupId = groupId,
    createdAt = createdAt,
    applicationState = ApplicationState.Started,
    userRole = Some(UserRole.Authorised),
    businessDetails = None,
    applicantContactDetails = None,
    amlsDetails = None,
    agentDetails = None,
    hmrcStandardForAgentsAgreed = StateOfAgreement.NotSet,
    numberOfRequiredKeyIndividuals = None,
    refusalToDealWithCheckResult = None,
    companyStatusCheckResult = None
  )

}
