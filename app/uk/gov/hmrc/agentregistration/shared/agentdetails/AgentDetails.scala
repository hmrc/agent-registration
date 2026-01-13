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

package uk.gov.hmrc.agentregistration.shared.agentdetails

import play.api.libs.json.Format
import play.api.libs.json.Json
import uk.gov.hmrc.agentregistration.shared.util.Errors.*

final case class AgentDetails(
  businessName: AgentBusinessName,
  telephoneNumber: Option[AgentTelephoneNumber] = None,
  agentEmailAddress: Option[AgentVerifiedEmailAddress] = None,
  agentCorrespondenceAddress: Option[AgentCorrespondenceAddress] = None
):

  def isComplete: Boolean =
    (businessName.agentBusinessName.nonEmpty || businessName.otherAgentBusinessName.nonEmpty)
      && telephoneNumber.isDefined
      && agentEmailAddress.exists(_.isVerified)
      && agentCorrespondenceAddress.isDefined

  def getTelephoneNumber: AgentTelephoneNumber = telephoneNumber.getOrThrowExpectedDataMissing("Telephone number is missing")
  def getAgentEmailAddress: AgentVerifiedEmailAddress = agentEmailAddress.getOrThrowExpectedDataMissing("Email address is missing")
  def getAgentCorrespondenceAddress: AgentCorrespondenceAddress = agentCorrespondenceAddress.getOrThrowExpectedDataMissing(
    "Correspondence address is missing"
  )

object AgentDetails:
  given format: Format[AgentDetails] = Json.format[AgentDetails]
