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
import uk.gov.hmrc.agentregistration.repository.providedetails.llp.MemeberProvidedDetailsRepo
import uk.gov.hmrc.agentregistration.shared.llp.MemberProvidedDetails
import uk.gov.hmrc.agentregistration.testsupport.ControllerSpec
import uk.gov.hmrc.agentregistration.testsupport.testdata.TdAll.tdAll.agentApplicationId
import uk.gov.hmrc.agentregistration.testsupport.wiremock.stubs.providedetails.IndividualAuthStubs
import uk.gov.hmrc.agentregistration.testsupport.wiremock.stubs.AuthStubs
import uk.gov.hmrc.agentregistration.util.RequestSupport.hc
import uk.gov.hmrc.http.HttpReads.Implicits.given
import uk.gov.hmrc.http.HttpReads
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.http.StringContextOps
import play.api.libs.ws.JsonBodyWritables.given

class MemberProvidedDetailsControllerSpec
extends ControllerSpec:

  "find member provided details returns NO_CONTENT if there is no underlying records" in:

    given Request[?] = tdAll.backendRequest
    IndividualAuthStubs.stubAuthorise()

    val response =
      httpClient
        .get(url"$baseUrl/agent-registration/member-provided-details/agent-applicationId/${agentApplicationId.value}")
        .execute[HttpResponse]
        .futureValue
    response.status shouldBe Status.NO_CONTENT
    response.body shouldBe ""
    AuthStubs.verifyAuthorise()

  "find member provided details returns Ok and the MemberProvidedDetails as Json body" in:

    given Request[?] = tdAll.backendRequest

    IndividualAuthStubs.stubAuthorise()
    val repo = app.injector.instanceOf[MemeberProvidedDetailsRepo]
    val memberProvidedDetailsStarted = tdAll.providedDetailsLlp.afterStarted
    repo.upsert(memberProvidedDetailsStarted).futureValue
    repo.findByAgentApplicationId(
      memberProvidedDetailsStarted.agentApplicationId
    ).futureValue.value shouldBe memberProvidedDetailsStarted withClue "sanity check"

    val response =
      httpClient
        .get(url"$baseUrl/agent-registration/member-provided-details/agent-applicationId/${agentApplicationId.value}")
        .execute[HttpResponse]
        .futureValue
    response.status shouldBe Status.OK
    val memberProvidedDetails = response.json.as[MemberProvidedDetails]
    memberProvidedDetails shouldBe memberProvidedDetailsStarted
    IndividualAuthStubs.verifyAuthorise()

  "upsert member provided details to mongo and returns OK" in:

    given Request[?] = tdAll.backendRequest

    IndividualAuthStubs.stubAuthorise()
    val repo = app.injector.instanceOf[MemeberProvidedDetailsRepo]

    repo.findByAgentApplicationId(tdAll.agentApplicationId).futureValue shouldBe None withClue "assuming initially there is no records in mongo "
    val memberProvidedDetailsStarted = tdAll.providedDetailsLlp.afterStarted

    val response =
      httpClient
        .post(url"$baseUrl/agent-registration/member-provided-details")
        .withBody(Json.toJson(memberProvidedDetailsStarted))
        .execute[HttpResponse]
        .futureValue
    response.status shouldBe Status.OK
    response.body shouldBe ""

    repo
      .findByAgentApplicationId(tdAll.agentApplicationId)
      .futureValue.value shouldBe memberProvidedDetailsStarted withClue "after http request there should be records in mongo"
    IndividualAuthStubs.verifyAuthorise()
