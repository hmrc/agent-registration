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

package uk.gov.hmrc.agentregistration.controllers.providedetails.llp

import play.api.http.Status
import play.api.libs.json.Json
import play.api.mvc.Request
import uk.gov.hmrc.agentregistration.repository.providedetails.llp.IndividualProvidedDetailsRepo
import uk.gov.hmrc.agentregistration.testsupport.ControllerSpec
import uk.gov.hmrc.agentregistration.testsupport.testdata.TdAll.tdAll.agentApplicationId
import uk.gov.hmrc.agentregistration.testsupport.testdata.TdAll.tdAll.internalUserId
import uk.gov.hmrc.agentregistration.testsupport.wiremock.stubs.providedetails.IndividualAuthStubs
import uk.gov.hmrc.agentregistration.testsupport.wiremock.stubs.AuthStubs
import uk.gov.hmrc.agentregistration.util.RequestSupport.hc
import uk.gov.hmrc.http.HttpReads.Implicits.given
import uk.gov.hmrc.http.HttpReads
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.http.StringContextOps
import play.api.libs.ws.JsonBodyWritables.given
import uk.gov.hmrc.agentregistration.shared.AgentApplicationId
import uk.gov.hmrc.agentregistration.shared.llp.IndividualProvidedDetails
import uk.gov.hmrc.agentregistration.shared.llp.IndividualProvidedDetailsId

class IndividualProvidedDetailsControllerSpec
extends ControllerSpec:

  "find member provided details returns NO_CONTENT if there is no underlying records" in:

    given Request[?] = tdAll.backendRequest
    IndividualAuthStubs.stubAuthorise()

    val response =
      httpClient
        .get(url"$baseUrl/agent-registration/member-provided-details/by-agent-applicationId/${agentApplicationId.value}")
        .execute[HttpResponse]
        .futureValue
    response.status shouldBe Status.NO_CONTENT
    response.body shouldBe ""
    AuthStubs.verifyAuthorise()

  "find individual provided details returns Ok and the IndividualProvidedDetails as Json body" in:

    given Request[?] = tdAll.backendRequest

    IndividualAuthStubs.stubAuthorise()
    val individualProvidedDetailsRepo: IndividualProvidedDetailsRepo = app.injector.instanceOf[IndividualProvidedDetailsRepo]
    val individualProvidedDetailsStarted = tdAll.providedDetailsLlp.afterStarted
    individualProvidedDetailsRepo.upsert(individualProvidedDetailsStarted).futureValue
    individualProvidedDetailsRepo.findById(
      individualProvidedDetailsStarted.individualProvidedDetailsId
    ).futureValue.value shouldBe individualProvidedDetailsStarted withClue "sanity check"

    val response =
      httpClient
        .get(url"$baseUrl/agent-registration/member-provided-details/by-agent-applicationId/${agentApplicationId.value}")
        .execute[HttpResponse]
        .futureValue
    response.status shouldBe Status.OK
    val individualProvidedDetails = response.json.as[IndividualProvidedDetails]
    individualProvidedDetails shouldBe individualProvidedDetailsStarted
    IndividualAuthStubs.verifyAuthorise()

  "find all individual provided details for auth user returns Ok and the MemberProvidedDetails as Json body" in:

    given Request[?] = tdAll.backendRequest

    IndividualAuthStubs.stubAuthorise()
    val repo = app.injector.instanceOf[IndividualProvidedDetailsRepo]
    val individualProvidedDetailsStarted = List(
      tdAll.providedDetailsLlp.afterStarted,
      tdAll.providedDetailsLlp.afterStarted.copy(
        _id = IndividualProvidedDetailsId("member-provided-details-id-67890"),
        agentApplicationId = AgentApplicationId(value = "another-agent-application-id")
      )
    )

    individualProvidedDetailsStarted.foreach { individualProvidedDetails =>
      repo.upsert(individualProvidedDetails).futureValue
    }

    repo.findByInternalUserId(internalUserId)
      .futureValue shouldBe individualProvidedDetailsStarted withClue "sanity check"

    val response: HttpResponse =
      httpClient
        .get(url"$baseUrl/agent-registration/member-provided-details")
        .execute[HttpResponse]
        .futureValue
    response.status shouldBe Status.OK
    val individualProvidedDetails = response.json.as[List[IndividualProvidedDetails]]
    individualProvidedDetails shouldBe individualProvidedDetailsStarted
    IndividualAuthStubs.verifyAuthorise()

  "upsert individual provided details to mongo and returns OK" in:

    given Request[?] = tdAll.backendRequest

    IndividualAuthStubs.stubAuthorise()
    val individualProvidedDetailsRepo: IndividualProvidedDetailsRepo = app.injector.instanceOf[IndividualProvidedDetailsRepo]

    val individualProvidedDetailsStarted: IndividualProvidedDetails = tdAll.providedDetailsLlp.afterStarted
    individualProvidedDetailsRepo.findById(
      individualProvidedDetailsStarted.individualProvidedDetailsId
    ).futureValue shouldBe None withClue "assuming initially there is no records in mongo "

    val response: HttpResponse =
      httpClient
        .post(url"$baseUrl/agent-registration/member-provided-details")
        .withBody(Json.toJson(individualProvidedDetailsStarted))
        .execute[HttpResponse]
        .futureValue
    response.status shouldBe Status.OK
    response.body shouldBe ""

    individualProvidedDetailsRepo
      .findById(individualProvidedDetailsStarted.individualProvidedDetailsId)
      .futureValue.value shouldBe individualProvidedDetailsStarted withClue "after http request there should be records in mongo"
    IndividualAuthStubs.verifyAuthorise()
