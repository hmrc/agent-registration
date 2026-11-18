/*
 * Copyright 2023 HM Revenue & Customs
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

import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.BusinessType
import uk.gov.hmrc.agentregistration.shared.testdata.agentapplication.ExpiryTransformations.*

/** The applications of one business type: one in each application state, and one that can be sent an email. */
final case class AgentApplicationStates(
  started: AgentApplication,
  grsDataReceived: AgentApplication,
  withVerifiedEmail: AgentApplication,
  sentForRisking: AgentApplication,
  sentToMinerva: AgentApplication,
  riskingCompleted: AgentApplication
):

  val expired: AgentApplication = started.makeOverdue.makeExpired
  val havingExpiryDate: Seq[AgentApplication] = Seq(started, grsDataReceived)
  val all: Seq[AgentApplication] = Seq(
    started,
    grsDataReceived,
    withVerifiedEmail,
    sentForRisking,
    sentToMinerva,
    riskingCompleted,
    expired
  )

object AgentApplicationStates:

  private lazy val tdAll: TdAll = TdAll.tdAll

  // exhaustive on purpose: a new business type does not compile until it has its applications here
  def forBusinessType(businessType: BusinessType): AgentApplicationStates =
    businessType match
      case BusinessType.SoleTrader =>
        AgentApplicationStates(
          started = tdAll.agentApplicationSoleTrader.afterStarted,
          grsDataReceived = tdAll.agentApplicationSoleTrader.afterGrsDataReceived,
          withVerifiedEmail = tdAll.agentApplicationSoleTrader.afterContactDetailsComplete,
          sentForRisking = tdAll.agentApplicationSoleTrader.afterDeclarationSubmitted,
          sentToMinerva = tdAll.agentApplicationSoleTrader.afterSentToMinerva,
          riskingCompleted = tdAll.agentApplicationSoleTrader.afterRiskingCompletedApproved
        )
      case BusinessType.LimitedCompany =>
        AgentApplicationStates(
          started = tdAll.agentApplicationLimitedCompany.afterStarted,
          grsDataReceived = tdAll.agentApplicationLimitedCompany.afterGrsDataReceived,
          withVerifiedEmail = tdAll.agentApplicationLimitedCompany.afterContactDetailsComplete,
          sentForRisking = tdAll.agentApplicationLimitedCompany.afterDeclarationSubmitted,
          sentToMinerva = tdAll.agentApplicationLimitedCompany.afterSentToMinerva,
          riskingCompleted = tdAll.agentApplicationLimitedCompany.afterRiskingCompletedApproved
        )
      case BusinessType.Partnership.GeneralPartnership =>
        AgentApplicationStates(
          started = tdAll.agentApplicationGeneralPartnership.afterStarted,
          grsDataReceived = tdAll.agentApplicationGeneralPartnership.afterGrsDataReceived,
          withVerifiedEmail = tdAll.agentApplicationGeneralPartnership.afterContactDetailsComplete,
          sentForRisking = tdAll.agentApplicationGeneralPartnership.afterDeclarationSubmitted,
          sentToMinerva = tdAll.agentApplicationGeneralPartnership.afterSentToMinerva,
          riskingCompleted = tdAll.agentApplicationGeneralPartnership.afterRiskingCompletedApproved
        )
      case BusinessType.Partnership.LimitedLiabilityPartnership =>
        AgentApplicationStates(
          started = tdAll.agentApplicationLlp.afterStarted,
          grsDataReceived = tdAll.agentApplicationLlp.afterGrsDataReceived,
          withVerifiedEmail = tdAll.agentApplicationLlp.afterContactDetailsComplete,
          sentForRisking = tdAll.agentApplicationLlp.afterDeclarationSubmitted,
          sentToMinerva = tdAll.agentApplicationLlp.afterSentToMinerva,
          riskingCompleted = tdAll.agentApplicationLlp.afterRiskingCompletedApproved
        )
      case BusinessType.Partnership.LimitedPartnership =>
        AgentApplicationStates(
          started = tdAll.agentApplicationLimitedPartnership.afterStarted,
          grsDataReceived = tdAll.agentApplicationLimitedPartnership.afterGrsDataReceived,
          withVerifiedEmail = tdAll.agentApplicationLimitedPartnership.afterContactDetailsComplete,
          sentForRisking = tdAll.agentApplicationLimitedPartnership.afterDeclarationSubmitted,
          sentToMinerva = tdAll.agentApplicationLimitedPartnership.afterSentToMinerva,
          riskingCompleted = tdAll.agentApplicationLimitedPartnership.afterRiskingCompletedApproved
        )
      case BusinessType.Partnership.ScottishLimitedPartnership =>
        AgentApplicationStates(
          started = tdAll.agentApplicationScottishLimitedPartnership.afterStarted,
          grsDataReceived = tdAll.agentApplicationScottishLimitedPartnership.afterGrsDataReceived,
          withVerifiedEmail = tdAll.agentApplicationScottishLimitedPartnership.afterContactDetailsComplete,
          sentForRisking = tdAll.agentApplicationScottishLimitedPartnership.afterDeclarationSubmitted,
          sentToMinerva = tdAll.agentApplicationScottishLimitedPartnership.afterSentToMinerva,
          riskingCompleted = tdAll.agentApplicationScottishLimitedPartnership.afterRiskingCompletedApproved
        )
      case BusinessType.Partnership.ScottishPartnership =>
        AgentApplicationStates(
          started = tdAll.agentApplicationScottishPartnership.afterStarted,
          grsDataReceived = tdAll.agentApplicationScottishPartnership.afterGrsDataReceived,
          withVerifiedEmail = tdAll.agentApplicationScottishPartnership.afterContactDetailsComplete,
          sentForRisking = tdAll.agentApplicationScottishPartnership.afterDeclarationSubmitted,
          sentToMinerva = tdAll.agentApplicationScottishPartnership.afterSentToMinerva,
          riskingCompleted = tdAll.agentApplicationScottishPartnership.afterRiskingCompletedApproved
        )
