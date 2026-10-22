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

package uk.gov.hmrc.agentregistration.shared

import play.api.libs.json.Format
import uk.gov.hmrc.agentregistration.shared.util.JsonFormatsFactory
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.===

enum ApplicationState:

  case Started
  case GrsDataReceived
  case SentForRisking
  case SentToMinerva
  case RiskingCompleted // outcomes are populated now
  case Expired

object ApplicationState:

  given Format[ApplicationState] = JsonFormatsFactory.makeEnumFormat[ApplicationState]

  val initialState: ApplicationState = Started

  def allowedPreviousStates(nextState: ApplicationState): Set[ApplicationState] = values.filter(_.canTransitionTo(nextState)).toSet

  extension (as: ApplicationState)

    def sentForRisking: Boolean = as === ApplicationState.SentForRisking

    def allowedNextStates: Set[ApplicationState] =
      as match
        case Started =>
          Set(
            Started,
            GrsDataReceived,
            Expired
          )
        case GrsDataReceived =>
          Set(
            GrsDataReceived,
            SentForRisking,
            Expired
          )
        case SentForRisking =>
          Set(
            SentForRisking,
            SentToMinerva,
            RiskingCompleted
          )
        case SentToMinerva => Set(SentToMinerva, RiskingCompleted)
        case RiskingCompleted =>
          Set(
            RiskingCompleted,
            SentForRisking,
            Expired
          )
        case Expired => Set.empty

    def isTerminal: Boolean = as.allowedNextStates.isEmpty

    def canTransitionTo(nextState: ApplicationState): Boolean = as.allowedNextStates.contains(nextState)
