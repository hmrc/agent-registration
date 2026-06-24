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

package uk.gov.hmrc.agentregistration.controllers

import play.api.http.Status
import play.api.libs.json.Json
import play.api.libs.ws.JsonBodyWritables.given
import play.api.mvc.Request
import uk.gov.hmrc.agentregistration.repository.AgentApplicationRepo
import uk.gov.hmrc.agentregistration.repository.providedetails.llp.IndividualProvidedDetailsRepo
import uk.gov.hmrc.agentregistration.shared.ApplicationState
import uk.gov.hmrc.agentregistration.shared.PersonReference
import uk.gov.hmrc.agentregistration.shared.risking.IndividualOutcome
import uk.gov.hmrc.agentregistration.shared.risking.RiskingOutcomeApplication
import uk.gov.hmrc.agentregistration.shared.risking.RiskingOutcomeEntity
import uk.gov.hmrc.agentregistration.shared.risking.RiskingOutcomeIndividual
import uk.gov.hmrc.agentregistration.shared.risking.RiskingOutcomeRequest
import uk.gov.hmrc.agentregistration.testsupport.ControllerSpec
import uk.gov.hmrc.agentregistration.util.RequestSupport.hc
import uk.gov.hmrc.http.HttpReads.Implicits.given
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.http.StringContextOps

class RiskingUpdatesControllerSpec
extends ControllerSpec:

  val agentApplicationRepo: AgentApplicationRepo = app.injector.instanceOf[AgentApplicationRepo]
  val individualProvidedDetailsRepo: IndividualProvidedDetailsRepo = app.injector.instanceOf[IndividualProvidedDetailsRepo]

  private val agentApplicationSentForRisking = tdAll.agentApplicationLlp.afterSentForRisking
  private val applicationReference = agentApplicationSentForRisking.applicationReference
  private val riskingOutcomeRequest = RiskingOutcomeRequest(
    riskingOutcomeApplication = tdAll.riskingOutcomeApplication(outcome = RiskingOutcomeApplication.Outcome.Approved),
    riskingOutcomeEntity = RiskingOutcomeEntity.Approved,
    individualOutcomes = Seq(
      IndividualOutcome(
        personReference = tdAll.personReference,
        riskingOutcomeIndividual = RiskingOutcomeIndividual.Approved
      )
    )
  )

  "receiveRiskingOutcome updates the application with outcomes, flips state to RiskingCompleted, and updates each individual" in:
    given Request[?] = tdAll.backendRequest
    val individualProvidedDetails = tdAll.providedDetails.afterFinished
    agentApplicationRepo.upsert(agentApplicationSentForRisking).futureValue
    individualProvidedDetailsRepo.upsert(individualProvidedDetails).futureValue
    agentApplicationRepo.findByApplicationReference(
      applicationReference
    ).futureValue.value shouldBe agentApplicationSentForRisking withClue "application exists"
    individualProvidedDetailsRepo.findByPersonReference(tdAll.personReference).futureValue.value shouldBe individualProvidedDetails withClue "individual exists"

    val response =
      httpClient
        .post(url"$baseUrl/agent-registration/risking-updates/risking-outcome/${applicationReference.value}")
        .withBody(Json.toJson(riskingOutcomeRequest))
        .execute[HttpResponse]
        .futureValue

    response.status shouldBe Status.OK
    response.body shouldBe ""

    val updatedApplication = agentApplicationRepo.findByApplicationReference(applicationReference).futureValue.value
    updatedApplication.applicationState shouldBe ApplicationState.RiskingCompleted
    updatedApplication.riskingOutcomeApplication shouldBe Some(riskingOutcomeRequest.riskingOutcomeApplication)
    updatedApplication.riskingOutcomeEntity shouldBe Some(riskingOutcomeRequest.riskingOutcomeEntity)

    val updatedIndividual = individualProvidedDetailsRepo.findByPersonReference(tdAll.personReference).futureValue.value
    updatedIndividual.riskingOutcomeIndividual shouldBe Some(RiskingOutcomeIndividual.Approved)

  "receiveRiskingOutcome returns NOT_FOUND if no application exists for the given applicationReference" in:
    given Request[?] = tdAll.backendRequest
    agentApplicationRepo.findByApplicationReference(applicationReference).futureValue shouldBe None withClue "no application found"

    val response =
      httpClient
        .post(url"$baseUrl/agent-registration/risking-updates/risking-outcome/${applicationReference.value}")
        .withBody(Json.toJson(riskingOutcomeRequest))
        .execute[HttpResponse]
        .futureValue

    response.status shouldBe Status.NOT_FOUND

  "receiveRiskingOutcome fails when an individual referenced by personReference does not exist" in:
    given Request[?] = tdAll.backendRequest
    agentApplicationRepo.upsert(agentApplicationSentForRisking).futureValue
    agentApplicationRepo.findByApplicationReference(
      applicationReference
    ).futureValue.value shouldBe agentApplicationSentForRisking withClue "application exists"

    val requestWithMissingIndividual = riskingOutcomeRequest.copy(
      individualOutcomes = Seq(
        IndividualOutcome(
          personReference = PersonReference("PREF_MISSING"),
          riskingOutcomeIndividual = RiskingOutcomeIndividual.Approved
        )
      )
    )

    val response =
      httpClient
        .post(url"$baseUrl/agent-registration/risking-updates/risking-outcome/${applicationReference.value}")
        .withBody(Json.toJson(requestWithMissingIndividual))
        .execute[HttpResponse]
        .futureValue

    // TODO - confirm is OK
    response.status shouldBe Status.INTERNAL_SERVER_ERROR
