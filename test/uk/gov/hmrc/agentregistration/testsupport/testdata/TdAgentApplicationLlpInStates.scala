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

import uk.gov.hmrc.agentregistration.shared.AgentApplicationId
import uk.gov.hmrc.agentregistration.shared.AgentApplicationLlp
import uk.gov.hmrc.agentregistration.shared.ApplicationReference
import uk.gov.hmrc.agentregistration.shared.ApplicationState
import uk.gov.hmrc.agentregistration.shared.InternalUserId
import uk.gov.hmrc.agentregistration.shared.LinkId
import uk.gov.hmrc.agentregistration.shared.PersonReference
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetails
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetailsId
import uk.gov.hmrc.agentregistration.shared.individual.ProvidedDetailsState

trait TdLlpScenario:

  protected val tdAll: TdAll = TdAll.tdAll

  def seed: String
  def applicationState: ApplicationState
  def isApplicationReadyToSubmitEmailSent: Option[Boolean]
  def individuals: Seq[IndividualProvidedDetails]

  def agentApplicationLlp: AgentApplicationLlp =
    val agentApplicationLlpBase: AgentApplicationLlp = tdAll.agentApplicationLlp.afterContactDetailsComplete
    agentApplicationLlpBase.copy(
      _id = AgentApplicationId(s"${agentApplicationLlpBase._id.value}-$seed"),
      internalUserId = InternalUserId(s"${agentApplicationLlpBase.internalUserId.value}-$seed"),
      linkId = LinkId(s"${agentApplicationLlpBase.linkId.value}-$seed"),
      applicationReference = ApplicationReference(s"${agentApplicationLlpBase.applicationReference.value}-$seed"),
      applicationState = applicationState,
      isApplicationReadyToSubmitEmailSent = isApplicationReadyToSubmitEmailSent
    )

  protected def finishedIndividual(suffix: String): IndividualProvidedDetails =
    val individualProvidedDetailsBase: IndividualProvidedDetails = tdAll.providedDetails.precreated
    individualProvidedDetailsBase.copy(
      _id = IndividualProvidedDetailsId(s"${individualProvidedDetailsBase._id.value}-$seed-$suffix"),
      personReference = PersonReference(s"${individualProvidedDetailsBase.personReference.value}-$seed-$suffix"),
      agentApplicationId = agentApplicationLlp._id,
      providedDetailsState = ProvidedDetailsState.Finished
    )

  protected def startedIndividual(suffix: String): IndividualProvidedDetails =
    finishedIndividual(suffix).copy(providedDetailsState = ProvidedDetailsState.Started)

object TdAgentApplicationLlpInStates:

  case object readyForEmail
  extends TdLlpScenario:
    override val seed: String = this.toString
    override val applicationState: ApplicationState = ApplicationState.GrsDataReceived
    override val isApplicationReadyToSubmitEmailSent: Option[Boolean] = None
    val individualA: IndividualProvidedDetails = finishedIndividual("a")
    val individualB: IndividualProvidedDetails = finishedIndividual("b")
    override val individuals: Seq[IndividualProvidedDetails] = Seq(individualA, individualB)

  case object emailAlreadySent
  extends TdLlpScenario:
    override val seed: String = this.toString
    override val applicationState: ApplicationState = ApplicationState.GrsDataReceived
    override val isApplicationReadyToSubmitEmailSent: Option[Boolean] = Some(true)
    override val individuals: Seq[IndividualProvidedDetails] = Seq(finishedIndividual("a"))

  case object emailAlreadySuppressed
  extends TdLlpScenario:
    override val seed: String = this.toString
    override val applicationState: ApplicationState = ApplicationState.GrsDataReceived
    override val isApplicationReadyToSubmitEmailSent: Option[Boolean] = Some(false)
    override val individuals: Seq[IndividualProvidedDetails] = Seq(finishedIndividual("a"))

  case object notYetGrs
  extends TdLlpScenario:
    override val seed: String = this.toString
    override val applicationState: ApplicationState = ApplicationState.Started
    override val isApplicationReadyToSubmitEmailSent: Option[Boolean] = None
    override val individuals: Seq[IndividualProvidedDetails] = Seq(finishedIndividual("a"))

  case object alreadySentForRisking
  extends TdLlpScenario:
    override val seed: String = this.toString
    override val applicationState: ApplicationState = ApplicationState.SentForRisking
    override val isApplicationReadyToSubmitEmailSent: Option[Boolean] = None
    override val individuals: Seq[IndividualProvidedDetails] = Seq(finishedIndividual("a"))

  case object noIndividuals
  extends TdLlpScenario:
    override val seed: String = this.toString
    override val applicationState: ApplicationState = ApplicationState.GrsDataReceived
    override val isApplicationReadyToSubmitEmailSent: Option[Boolean] = None
    override val individuals: Seq[IndividualProvidedDetails] = Seq.empty

  case object oneIndividualNotFinished
  extends TdLlpScenario:
    override val seed: String = this.toString
    override val applicationState: ApplicationState = ApplicationState.GrsDataReceived
    override val isApplicationReadyToSubmitEmailSent: Option[Boolean] = None
    val individualFinished: IndividualProvidedDetails = finishedIndividual("a")
    val individualStarted: IndividualProvidedDetails = startedIndividual("b")
    override val individuals: Seq[IndividualProvidedDetails] = Seq(individualFinished, individualStarted)

  val all: Seq[TdLlpScenario] = Seq(
    readyForEmail,
    emailAlreadySent,
    emailAlreadySuppressed,
    notYetGrs,
    alreadySentForRisking,
    noIndividuals,
    oneIndividualNotFinished
  )
