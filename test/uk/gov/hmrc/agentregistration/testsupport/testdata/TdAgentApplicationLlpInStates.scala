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
import uk.gov.hmrc.agentregistration.model.EmailStatus
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.AgentApplicationLlp
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetails

trait TdScenario:

  def seed: String

  protected lazy val td: TdAll = TdAll.make(seed)

  def agentApplication: AgentApplication
  def applicationScheduler: Option[ApplicationScheduler]
  def individuals: Seq[IndividualProvidedDetails]

  protected def schedulerWith(emailStatus: EmailStatus): ApplicationScheduler = td.applicationScheduler.notProcessed.copy(applicationReadyToSubmitEmailStatus =
    emailStatus
  )

  protected def finishedIndividual(individualSeed: String): IndividualProvidedDetails =
    TdAll.makeForIndividual(seed, individualSeed).providedDetails.afterFinished

  protected def startedIndividual(individualSeed: String): IndividualProvidedDetails =
    TdAll.makeForIndividual(seed, individualSeed).providedDetails.afterStarted

trait TdLlpScenario
extends TdScenario:

  def agentApplicationLlp: AgentApplicationLlp
  override def agentApplication: AgentApplication = agentApplicationLlp

object TdAgentApplicationLlpInStates:

  case object readyForEmail
  extends TdLlpScenario:

    override val seed: String = this.toString
    override val agentApplicationLlp: AgentApplicationLlp = td.agentApplicationLlp.afterContactDetailsComplete
    override val applicationScheduler: Option[ApplicationScheduler] = None
    val individualA: IndividualProvidedDetails = finishedIndividual("a")
    val individualB: IndividualProvidedDetails = finishedIndividual("b")
    override val individuals: Seq[IndividualProvidedDetails] = Seq(individualA, individualB)

  /** Eligible AND already has a scheduler record left at `NotProcessed` (e.g. from a partial-write or crash). The finder must still return this because
    * `NotProcessed` is not in the "skip" set — only `Sent` / `Suppressed` are.
    */
  case object readyForEmailWithNotProcessedScheduler
  extends TdLlpScenario:

    override val seed: String = this.toString
    override val agentApplicationLlp: AgentApplicationLlp = td.agentApplicationLlp.afterContactDetailsComplete
    override val applicationScheduler: Option[ApplicationScheduler] = Some(schedulerWith(EmailStatus.NotProcessed))
    override val individuals: Seq[IndividualProvidedDetails] = Seq(finishedIndividual("a"))

  case object emailAlreadySent
  extends TdLlpScenario:

    override val seed: String = this.toString
    override val agentApplicationLlp: AgentApplicationLlp = td.agentApplicationLlp.afterContactDetailsComplete
    override val applicationScheduler: Option[ApplicationScheduler] = Some(schedulerWith(EmailStatus.Sent))
    override val individuals: Seq[IndividualProvidedDetails] = Seq(finishedIndividual("a"))

  case object emailAlreadySuppressed
  extends TdLlpScenario:

    override val seed: String = this.toString
    override val agentApplicationLlp: AgentApplicationLlp = td.agentApplicationLlp.afterContactDetailsComplete
    override val applicationScheduler: Option[ApplicationScheduler] = Some(schedulerWith(EmailStatus.Suppressed))
    override val individuals: Seq[IndividualProvidedDetails] = Seq(finishedIndividual("a"))

  case object notYetGrs
  extends TdLlpScenario:

    override val seed: String = this.toString
    override val agentApplicationLlp: AgentApplicationLlp = td.agentApplicationLlp.afterStarted
    override val applicationScheduler: Option[ApplicationScheduler] = None
    override val individuals: Seq[IndividualProvidedDetails] = Seq(finishedIndividual("a"))

  case object alreadySentForRisking
  extends TdLlpScenario:

    override val seed: String = this.toString
    override val agentApplicationLlp: AgentApplicationLlp = td.agentApplicationLlp.afterDeclarationSubmitted
    override val applicationScheduler: Option[ApplicationScheduler] = None
    override val individuals: Seq[IndividualProvidedDetails] = Seq(finishedIndividual("a"))

  case object noIndividuals
  extends TdLlpScenario:

    override val seed: String = this.toString
    override val agentApplicationLlp: AgentApplicationLlp = td.agentApplicationLlp.afterContactDetailsComplete
    override val applicationScheduler: Option[ApplicationScheduler] = None
    override val individuals: Seq[IndividualProvidedDetails] = Seq.empty

  case object oneIndividualNotFinished
  extends TdLlpScenario:

    override val seed: String = this.toString
    override val agentApplicationLlp: AgentApplicationLlp = td.agentApplicationLlp.afterContactDetailsComplete
    override val applicationScheduler: Option[ApplicationScheduler] = None
    val individualFinished: IndividualProvidedDetails = finishedIndividual("a")
    val individualStarted: IndividualProvidedDetails = startedIndividual("b")
    override val individuals: Seq[IndividualProvidedDetails] = Seq(individualFinished, individualStarted)

  val all: Seq[TdLlpScenario] = Seq(
    readyForEmail,
    readyForEmailWithNotProcessedScheduler,
    emailAlreadySent,
    emailAlreadySuppressed,
    notYetGrs,
    alreadySentForRisking,
    noIndividuals,
    oneIndividualNotFinished
  )
