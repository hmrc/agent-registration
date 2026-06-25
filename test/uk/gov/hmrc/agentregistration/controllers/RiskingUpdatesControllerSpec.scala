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
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetailsId
import uk.gov.hmrc.agentregistration.shared.risking.EntityFailure
import uk.gov.hmrc.agentregistration.shared.risking.EntityFix
import uk.gov.hmrc.agentregistration.shared.risking.IndividualFailure
import uk.gov.hmrc.agentregistration.shared.risking.IndividualFailures
import uk.gov.hmrc.agentregistration.shared.risking.IndividualFix
import uk.gov.hmrc.agentregistration.shared.risking.RiskingOutcomeApplication
import uk.gov.hmrc.agentregistration.shared.risking.RiskingOutcomeEntity
import uk.gov.hmrc.agentregistration.shared.risking.RiskingOutcomeIndividual
import uk.gov.hmrc.agentregistration.shared.risking.RiskingOutcomeRequest
import uk.gov.hmrc.agentregistration.testsupport.ControllerSpec
import uk.gov.hmrc.agentregistration.util.RequestSupport.hc
import uk.gov.hmrc.http.HttpReads.Implicits.given
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.http.StringContextOps

import java.time.LocalDate

class RiskingUpdatesControllerSpec
extends ControllerSpec:

  val agentApplicationRepo: AgentApplicationRepo = app.injector.instanceOf[AgentApplicationRepo]
  val individualProvidedDetailsRepo: IndividualProvidedDetailsRepo = app.injector.instanceOf[IndividualProvidedDetailsRepo]

  private val agentApplicationSentForRisking = tdAll.agentApplicationLlp.afterSentForRisking
  private val applicationReference = agentApplicationSentForRisking.applicationReference
  private val riskingCompletedDate: LocalDate = LocalDate.of(2026, 6, 24)
  private val correctiveActionExpiryDate: LocalDate = riskingCompletedDate.plusDays(45)

  private val emptyFailuresRequest: RiskingOutcomeRequest = RiskingOutcomeRequest(
    riskingCompletedDate = riskingCompletedDate,
    correctiveActionExpiryDate = None,
    applicationOutcome = RiskingOutcomeApplication.Outcome.Approved,
    entityFailures = Seq.empty,
    individualFailures = Seq(
      IndividualFailures(
        personReference = tdAll.personReference,
        failures = Seq.empty
      )
    )
  )

  private case class OutcomeTestCase(
    description: String,
    entityFailures: Seq[EntityFailure],
    individualFailures: Seq[IndividualFailure],
    correctiveActionExpiryDate: Option[LocalDate],
    expectedApplicationOutcome: RiskingOutcomeApplication.Outcome,
    expectedRiskingOutcomeEntity: RiskingOutcomeEntity,
    expectedRiskingOutcomeIndividual: RiskingOutcomeIndividual
  )

  "receiveRiskingOutcome derives outcomes from failures, persists them, flips state to RiskingCompleted, and updates each individual" - {

    List(
      OutcomeTestCase(
        description = "empty failures yield Approved end-to-end",
        entityFailures = Seq.empty,
        individualFailures = Seq.empty,
        correctiveActionExpiryDate = None,
        expectedApplicationOutcome = RiskingOutcomeApplication.Outcome.Approved,
        expectedRiskingOutcomeEntity = RiskingOutcomeEntity.Approved,
        expectedRiskingOutcomeIndividual = RiskingOutcomeIndividual.Approved
      ),
      OutcomeTestCase(
        description = "a fixable entity failure yields FailedFixable end-to-end with the corresponding fix and corrective action expiry preserved",
        entityFailures = Seq(EntityFailure._4._1),
        individualFailures = Seq.empty,
        correctiveActionExpiryDate = Some(correctiveActionExpiryDate),
        expectedApplicationOutcome = RiskingOutcomeApplication.Outcome.FailedFixable,
        expectedRiskingOutcomeEntity = RiskingOutcomeEntity.FailedFixable(fixes = Seq(EntityFix._4._1(isConfirmed = None))),
        expectedRiskingOutcomeIndividual = RiskingOutcomeIndividual.Approved
      ),
      OutcomeTestCase(
        description = "a non-fixable entity failure yields FailedNonFixable end-to-end",
        entityFailures = Seq(EntityFailure._7),
        individualFailures = Seq.empty,
        correctiveActionExpiryDate = Some(correctiveActionExpiryDate),
        expectedApplicationOutcome = RiskingOutcomeApplication.Outcome.FailedNonFixable,
        expectedRiskingOutcomeEntity = RiskingOutcomeEntity.FailedNonFixable(failures = Seq(EntityFailure._7)),
        expectedRiskingOutcomeIndividual = RiskingOutcomeIndividual.Approved
      ),
      OutcomeTestCase(
        description = "entity Approved but a fixable individual failure yields FailedFixable application outcome",
        entityFailures = Seq.empty,
        individualFailures = Seq(IndividualFailure._4._1),
        correctiveActionExpiryDate = Some(correctiveActionExpiryDate),
        expectedApplicationOutcome = RiskingOutcomeApplication.Outcome.FailedFixable,
        expectedRiskingOutcomeEntity = RiskingOutcomeEntity.Approved,
        expectedRiskingOutcomeIndividual = RiskingOutcomeIndividual.FailedFixable(fixes = Seq(IndividualFix._4._1(isConfirmed = None)))
      ),
      OutcomeTestCase(
        description = "entity Approved but a non-fixable individual failure yields FailedNonFixable application outcome",
        entityFailures = Seq.empty,
        individualFailures = Seq(IndividualFailure._6),
        correctiveActionExpiryDate = Some(correctiveActionExpiryDate),
        expectedApplicationOutcome = RiskingOutcomeApplication.Outcome.FailedNonFixable,
        expectedRiskingOutcomeEntity = RiskingOutcomeEntity.Approved,
        expectedRiskingOutcomeIndividual = RiskingOutcomeIndividual.FailedNonFixable(failures = Seq(IndividualFailure._6))
      )
    ).foreach: tc =>
      tc.description in:
        given Request[?] = tdAll.backendRequest
        val individualProvidedDetails = tdAll.providedDetails.afterFinished
        agentApplicationRepo.upsert(agentApplicationSentForRisking).futureValue
        individualProvidedDetailsRepo.upsert(individualProvidedDetails).futureValue
        agentApplicationRepo.findByApplicationReference(
          applicationReference
        ).futureValue.value shouldBe agentApplicationSentForRisking withClue "application exists"
        individualProvidedDetailsRepo.findByPersonReference(
          tdAll.personReference
        ).futureValue.value shouldBe individualProvidedDetails withClue "individual exists"

        val request = RiskingOutcomeRequest(
          riskingCompletedDate = riskingCompletedDate,
          correctiveActionExpiryDate = tc.correctiveActionExpiryDate,
          applicationOutcome = tc.expectedApplicationOutcome,
          entityFailures = tc.entityFailures,
          individualFailures = Seq(
            IndividualFailures(
              personReference = tdAll.personReference,
              failures = tc.individualFailures
            )
          )
        )

        val response =
          httpClient
            .post(url"$baseUrl/agent-registration/risking-updates/risking-outcome/${applicationReference.value}")
            .withBody(Json.toJson(request))
            .execute[HttpResponse]
            .futureValue

        response.status shouldBe Status.OK
        response.body shouldBe ""

        val updatedApplication = agentApplicationRepo.findByApplicationReference(applicationReference).futureValue.value
        updatedApplication.applicationState shouldBe ApplicationState.RiskingCompleted
        updatedApplication.riskingOutcomeApplication shouldBe Some(RiskingOutcomeApplication(
          riskingCompletedDate = riskingCompletedDate,
          outcome = tc.expectedApplicationOutcome,
          correctiveActionExpiryDate = tc.correctiveActionExpiryDate
        ))
        updatedApplication.riskingOutcomeEntity shouldBe Some(tc.expectedRiskingOutcomeEntity)

        val updatedIndividual = individualProvidedDetailsRepo.findByPersonReference(tdAll.personReference).futureValue.value
        updatedIndividual.riskingOutcomeIndividual shouldBe Some(tc.expectedRiskingOutcomeIndividual)
  }

  "receiveRiskingOutcome applies the correct outcome per-individual when multiple individuals are in the request" in:
    given Request[?] = tdAll.backendRequest
    val individual1 = tdAll.providedDetails.afterFinished
    val individual2PersonReference = PersonReference("PREF2")
    val individual2 = tdAll.providedDetails.afterFinished.copy(
      _id = IndividualProvidedDetailsId("individual-provided-details-id-2"),
      personReference = individual2PersonReference
    )
    agentApplicationRepo.upsert(agentApplicationSentForRisking).futureValue
    individualProvidedDetailsRepo.upsert(individual1).futureValue
    individualProvidedDetailsRepo.upsert(individual2).futureValue

    val request = RiskingOutcomeRequest(
      riskingCompletedDate = riskingCompletedDate,
      correctiveActionExpiryDate = Some(correctiveActionExpiryDate),
      applicationOutcome = RiskingOutcomeApplication.Outcome.FailedFixable,
      entityFailures = Seq.empty,
      individualFailures = Seq(
        IndividualFailures(personReference = tdAll.personReference, failures = Seq.empty),
        IndividualFailures(personReference = individual2PersonReference, failures = Seq(IndividualFailure._4._1))
      )
    )

    val response =
      httpClient
        .post(url"$baseUrl/agent-registration/risking-updates/risking-outcome/${applicationReference.value}")
        .withBody(Json.toJson(request))
        .execute[HttpResponse]
        .futureValue

    response.status shouldBe Status.OK

    val updatedIndividual1 = individualProvidedDetailsRepo.findByPersonReference(tdAll.personReference).futureValue.value
    updatedIndividual1.riskingOutcomeIndividual shouldBe Some(RiskingOutcomeIndividual.Approved)

    val updatedIndividual2 = individualProvidedDetailsRepo.findByPersonReference(individual2PersonReference).futureValue.value
    updatedIndividual2.riskingOutcomeIndividual shouldBe Some(
      RiskingOutcomeIndividual.FailedFixable(fixes = Seq(IndividualFix._4._1(isConfirmed = None)))
    )

  "receiveRiskingOutcome returns NOT_FOUND if no application exists for the given applicationReference" in:
    given Request[?] = tdAll.backendRequest
    agentApplicationRepo.findByApplicationReference(applicationReference).futureValue shouldBe None withClue "no application found"

    val response =
      httpClient
        .post(url"$baseUrl/agent-registration/risking-updates/risking-outcome/${applicationReference.value}")
        .withBody(Json.toJson(emptyFailuresRequest))
        .execute[HttpResponse]
        .futureValue

    response.status shouldBe Status.NOT_FOUND

  "receiveRiskingOutcome fails when an individual referenced by personReference does not exist" in:
    given Request[?] = tdAll.backendRequest
    agentApplicationRepo.upsert(agentApplicationSentForRisking).futureValue
    agentApplicationRepo.findByApplicationReference(
      applicationReference
    ).futureValue.value shouldBe agentApplicationSentForRisking withClue "application exists"

    val requestWithMissingIndividual = emptyFailuresRequest.copy(
      individualFailures = Seq(
        IndividualFailures(
          personReference = PersonReference("PREF_MISSING"),
          failures = Seq.empty
        )
      )
    )

    val response =
      httpClient
        .post(url"$baseUrl/agent-registration/risking-updates/risking-outcome/${applicationReference.value}")
        .withBody(Json.toJson(requestWithMissingIndividual))
        .execute[HttpResponse]
        .futureValue

    response.status shouldBe Status.INTERNAL_SERVER_ERROR
