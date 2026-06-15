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

package uk.gov.hmrc.agentregistration.testsupport.testdata

import uk.gov.hmrc.agentregistration.model.ApplicationScheduler
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.AgentApplicationSoleTrader
import uk.gov.hmrc.agentregistration.shared.UserRole
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetails

trait TdSoleTraderScenario
extends TdScenario:

  def agentApplicationSoleTrader: AgentApplicationSoleTrader
  override def agentApplication: AgentApplication = agentApplicationSoleTrader

object TdAgentApplicationSoleTraderInStates:

  case object readyForEmailAsOwner
  extends TdSoleTraderScenario:

    override val seed: String = this.toString
    override val agentApplicationSoleTrader: AgentApplicationSoleTrader =
      td.agentApplicationSoleTrader.afterContactDetailsComplete // default userRole = Owner
    override val applicationScheduler: Option[ApplicationScheduler] = None
    override val individuals: Seq[IndividualProvidedDetails] = Seq(finishedIndividual("a"))

  case object readyForEmailAsNonOwner
  extends TdSoleTraderScenario:

    override val seed: String = this.toString
    override val agentApplicationSoleTrader: AgentApplicationSoleTrader =
      td.agentApplicationSoleTrader.afterContactDetailsComplete.copy(userRole = Some(UserRole.Authorised))
    override val applicationScheduler: Option[ApplicationScheduler] = None
    override val individuals: Seq[IndividualProvidedDetails] = Seq(finishedIndividual("a"))

  val all: Seq[TdSoleTraderScenario] = Seq(
    readyForEmailAsOwner,
    readyForEmailAsNonOwner
  )
