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

import com.softwaremill.quicklens.*
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.ApplicationReference
import uk.gov.hmrc.agentregistration.shared.ApplicationState
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.===

object ApplicationStateTransitionHelper:

  final case class InvalidTransition(
    applicationReference: ApplicationReference,
    currentState: Option[ApplicationState],
    nextState: ApplicationState
  ):

    val message: String =
      s"Invalid application state transition from [${currentState.fold("no application")(_.toString)}] to [$nextState] " +
        s"for applicationReference [${applicationReference.value}]"

  extension (agentApplication: AgentApplication)

    def transitionTo(nextState: ApplicationState): Either[InvalidTransition, AgentApplication] = Either.cond(
      agentApplication.applicationState.canTransitionTo(nextState),
      agentApplication.modify(_.applicationState).setTo(nextState),
      InvalidTransition(
        agentApplication.applicationReference,
        Some(agentApplication.applicationState),
        nextState
      )
    )

    private def validateAsNew: Either[InvalidTransition, AgentApplication] = Either.cond(
      agentApplication.applicationState === ApplicationState.initialState,
      agentApplication,
      InvalidTransition(
        agentApplication.applicationReference,
        None,
        agentApplication.applicationState
      )
    )

  extension (existingAgentApplication: Option[AgentApplication])

    def validateTransitionTo(agentApplication: AgentApplication): Either[InvalidTransition, AgentApplication] =
      existingAgentApplication.fold(agentApplication.validateAsNew)(_.transitionTo(agentApplication.applicationState).map(_ => agentApplication))
