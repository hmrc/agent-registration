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

package uk.gov.hmrc.agentregistration.shared.dataintegrity

import uk.gov.hmrc.agentregistration.shared.risking.RiskingOutcomeApplication
import uk.gov.hmrc.agentregistration.shared.*

object DataIntegrity:

  def violations(agentApplication: AgentApplication): Seq[String] =
    val found: Seq[String] =
      agentApplication.riskingOutcomeApplication match
        case Some(fixable: RiskingOutcomeApplication.FailedFixable) if fixable.reSubmittedAt.isDefined => Resubmission.violations(agentApplication)
        case _ => FirstSubmission.violations(agentApplication)
    found.map(message => s"integrity check failed [applicationReference=${agentApplication.applicationReference.value}]: $message")

  private def requireThat(
    condition: Boolean,
    message: => String
  ): Option[String] = if !condition then Some(message) else None

  private def violationsBeforeSubmission(agentApplication: AgentApplication)(using state: ApplicationState): Seq[String] =
    Seq(
      requireThat(agentApplication.applicationExpiresAt.isDefined, s"applicationExpiresAt should be defined in $state state"),
      requireThat(agentApplication.submittedAt.isEmpty, s"submittedAt should not be defined in $state state"),
      requireThat(agentApplication.riskingOutcomeApplication.isEmpty, s"riskingOutcomeApplication should not be defined in $state state"),
      requireThat(agentApplication.riskingOutcomeEntity.isEmpty, s"riskingOutcomeEntity should not be defined in $state state")
    ).flatten

  private def violationsAfterSubmission(agentApplication: AgentApplication)(using state: ApplicationState): Seq[String] =
    Seq(
      requireThat(agentApplication.applicationExpiresAt.isEmpty, s"applicationExpiresAt should not be defined in $state state"),
      requireThat(agentApplication.submittedAt.isDefined, s"submittedAt should be defined in $state state"),
      requireThat(businessDetailsDefined(agentApplication), s"businessDetails should be defined in $state state"),
      requireThat(agentApplication.applicantContactDetails.isDefined, s"applicantContactDetails should be defined in $state state"),
      requireThat(agentApplication.applicantContactDetails.forall(_.isComplete), s"applicantContactDetails should be complete in $state state"),
      requireThat(agentApplication.amlsDetails.isDefined, s"amlsDetails should be defined in $state state"),
      requireThat(agentApplication.amlsDetails.forall(_.isComplete), s"amlsDetails should be complete in $state state"),
      requireThat(agentApplication.agentDetails.isDefined, s"agentDetails should be defined in $state state"),
      requireThat(agentApplication.agentDetails.forall(_.isComplete), s"agentDetails should be complete in $state state"),
      requireThat(agentApplication.refusalToDealWithCheckResult.isDefined, s"refusalToDealWithCheckResult should be defined in $state state"),
      requireThat(agentApplication.globalAsaEnrolmentCheckResult.isDefined, s"globalAsaEnrolmentCheckResult should be defined in $state state"),
      requireThat(agentApplication.vrns.isDefined, s"vrns should be defined in $state state"),
      requireThat(agentApplication.payeRefs.isDefined, s"payeRefs should be defined in $state state")
    ).flatten

  private object FirstSubmission:

    def violations(agentApplication: AgentApplication): Seq[String] =
      agentApplication.applicationState match
        case ApplicationState.Started => whenStarted(agentApplication)
        case ApplicationState.GrsDataReceived => whenGrsDataReceived(agentApplication)
        case ApplicationState.SentForRisking | ApplicationState.SentToMinerva => whenAwaitingRiskingOutcome(agentApplication)
        case ApplicationState.RiskingCompleted => whenRiskingCompleted(agentApplication)

    private def whenStarted(agentApplication: AgentApplication): Seq[String] =
      given ApplicationState = ApplicationState.Started
      violationsBeforeSubmission(agentApplication)

    private def whenGrsDataReceived(agentApplication: AgentApplication): Seq[String] =
      given state: ApplicationState = ApplicationState.GrsDataReceived
      violationsBeforeSubmission(agentApplication) ++
        requireThat(businessDetailsDefined(agentApplication), s"businessDetails should be defined in $state state")

    private def whenAwaitingRiskingOutcome(agentApplication: AgentApplication): Seq[String] =
      given state: ApplicationState = agentApplication.applicationState
      violationsAfterSubmission(agentApplication) ++
        Seq(
          requireThat(
            agentApplication.riskingOutcomeApplication.isEmpty,
            s"riskingOutcomeApplication should not be defined in $state state during first submission"
          ),
          requireThat(agentApplication.riskingOutcomeEntity.isEmpty, s"riskingOutcomeEntity should not be defined in $state state during first submission")
        ).flatten

  private object Resubmission:

    def violations(agentApplication: AgentApplication): Seq[String] =
      agentApplication.applicationState match
        case ApplicationState.SentForRisking | ApplicationState.SentToMinerva => whenAwaitingRiskingOutcome(agentApplication)
        case ApplicationState.RiskingCompleted => whenRiskingCompleted(agentApplication)
        case other => Seq(s"resubmission-in-flight is only valid in SentForRisking, SentToMinerva or RiskingCompleted, found $other")

    private def whenAwaitingRiskingOutcome(agentApplication: AgentApplication): Seq[String] =
      given state: ApplicationState = agentApplication.applicationState
      // riskingOutcomeApplication is already guaranteed by the dispatch in `violations`, so only riskingOutcomeEntity is asserted here.
      violationsAfterSubmission(agentApplication) ++
        requireThat(agentApplication.riskingOutcomeEntity.isDefined, s"riskingOutcomeEntity should be preserved in $state state during resubmission")

  private def whenRiskingCompleted(agentApplication: AgentApplication): Seq[String] =
    given ApplicationState = ApplicationState.RiskingCompleted
    violationsAfterSubmission(agentApplication) ++
      Seq(
        requireThat(agentApplication.riskingOutcomeApplication.isDefined, "riskingOutcomeApplication should be defined in RiskingCompleted state"),
        requireThat(agentApplication.riskingOutcomeEntity.isDefined, "riskingOutcomeEntity should be defined in RiskingCompleted state")
      ).flatten

  private def businessDetailsDefined(agentApplication: AgentApplication): Boolean =
    agentApplication match
      case a: AgentApplicationSoleTrader => a.businessDetails.isDefined
      case a: AgentApplicationLlp => a.businessDetails.isDefined
      case a: AgentApplicationLimitedCompany => a.businessDetails.isDefined
      case a: AgentApplicationGeneralPartnership => a.businessDetails.isDefined
      case a: AgentApplicationLimitedPartnership => a.businessDetails.isDefined
      case a: AgentApplicationScottishLimitedPartnership => a.businessDetails.isDefined
      case a: AgentApplicationScottishPartnership => a.businessDetails.isDefined
