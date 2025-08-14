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

package uk.gov.hmrc.agentregistration.shared.testdata

import uk.gov.hmrc.agentregistration.shared.AboutYourApplication
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.ApplicationState
import uk.gov.hmrc.agentregistration.shared.InternalUserId

import java.time.Instant

trait TdAgentApplication { dependencies: TdBase =>

  private val createdAt: Instant = dependencies.instant

  val agentApplicationAfterCreated = AgentApplication(
    internalUserId = internalUserId,
    createdAt = createdAt,
    applicationState = ApplicationState.InProgress,
    utr = None,
    aboutYourApplication = AboutYourApplication(
      businessType = None,
      userRole = None,
      confirmed = None
    ),
    businessDetails = None,
    amlsDetails = None
  )

}
