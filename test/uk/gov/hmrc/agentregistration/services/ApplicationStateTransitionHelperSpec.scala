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
import uk.gov.hmrc.agentregistration.services.ApplicationStateTransitionHelper.*
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.ApplicationState
import uk.gov.hmrc.agentregistration.shared.ApplicationState.*
import uk.gov.hmrc.agentregistration.testsupport.testdata.TdAll
import uk.gov.hmrc.agentregistration.testsupport.UnitSpec

class ApplicationStateTransitionHelperSpec
extends UnitSpec:

  private val tdAll: TdAll = TdAll.tdAll

  private def applicationInState(state: ApplicationState): AgentApplication =
    state match
      case Started => tdAll.agentApplicationLlp.afterStarted
      case GrsDataReceived => tdAll.agentApplicationLlp.afterGrsDataReceived
      case SentForRisking => tdAll.agentApplicationLlp.afterSentForRisking
      case SentToMinerva => tdAll.agentApplicationLlp.afterSentToMinerva
      case RiskingCompleted => tdAll.agentApplicationLlp.afterRiskingCompletedApproved
      case Expired => tdAll.agentApplicationLlp.afterExpired

  "transitionTo moves the application to the next state exactly when ApplicationState.canTransitionTo allows it" in:
    ApplicationState.values.foreach: currentState =>
      val agentApplication: AgentApplication = applicationInState(currentState)
      ApplicationState.values.foreach: nextState =>
        val expected: Either[InvalidTransition, AgentApplication] =
          if currentState.canTransitionTo(nextState) then Right(agentApplication.modify(_.applicationState).setTo(nextState))
          else
            Left(InvalidTransition(
              agentApplication.applicationReference,
              Some(currentState),
              nextState
            ))
        agentApplication.transitionTo(nextState) shouldBe expected withClue s"transition $currentState -> $nextState"

  "validateTransitionTo with no existing application accepts only the initial state" in:
    ApplicationState.values.foreach: state =>
      val agentApplication: AgentApplication = applicationInState(state)
      val expected: Either[InvalidTransition, AgentApplication] =
        state match
          case Started => Right(agentApplication)
          case GrsDataReceived | SentForRisking | SentToMinerva | RiskingCompleted | Expired =>
            Left(InvalidTransition(
              agentApplication.applicationReference,
              None,
              state
            ))
      Option.empty[AgentApplication].validateTransitionTo(agentApplication) shouldBe expected withClue s"new application in $state"

  "validateTransitionTo with an existing application returns the incoming application exactly when the transition is allowed" in:
    ApplicationState.values.foreach: currentState =>
      val existingAgentApplication: AgentApplication = applicationInState(currentState)
      ApplicationState.values.foreach: nextState =>
        val agentApplication: AgentApplication = applicationInState(nextState)
        val expected: Either[InvalidTransition, AgentApplication] =
          if currentState.canTransitionTo(nextState) then Right(agentApplication)
          else
            Left(InvalidTransition(
              agentApplication.applicationReference,
              Some(currentState),
              nextState
            ))
        Some(existingAgentApplication).validateTransitionTo(agentApplication) shouldBe expected withClue s"transition $currentState -> $nextState"

  "InvalidTransition.message names both states and the applicationReference" in:
    val agentApplication: AgentApplication = tdAll.agentApplicationLlp.afterExpired
    agentApplication.transitionTo(GrsDataReceived).left.map(_.message) shouldBe
      Left(s"Invalid application state transition from [Expired] to [GrsDataReceived] for applicationReference [${agentApplication.applicationReference.value}]")
    Option.empty[AgentApplication].validateTransitionTo(tdAll.agentApplicationLlp.afterGrsDataReceived).left.map(_.message) shouldBe
      Left(s"Invalid application state transition from [no application] to [GrsDataReceived] for applicationReference [${agentApplication.applicationReference.value}]")
