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

package uk.gov.hmrc.agentregistration.controllers

import play.api.http.Status
import play.api.libs.json.Json
import play.api.mvc.Request
import uk.gov.hmrc.agentregistration.repository.AgentApplicationRepo
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.testsupport.ControllerSpec
import uk.gov.hmrc.agentregistration.testsupport.wiremock.stubs.AuthStubs
import uk.gov.hmrc.agentregistration.util.RequestSupport.hc
import uk.gov.hmrc.http.HttpReads.Implicits.given
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.http.StringContextOps
import play.api.libs.ws.JsonBodyWritables.given

class ApplicationControllerSpec
extends ControllerSpec:

  "find application returns NO_CONTENT if there is no underlying records" in:
    given Request[?] = tdAll.backendRequest
    AuthStubs.stubAuthorise()
    val response =
      httpClient
        .get(url"$baseUrl/agent-registration/application")
        .execute[HttpResponse]
        .futureValue
    response.status shouldBe Status.NO_CONTENT
    response.body shouldBe ""
    AuthStubs.verifyAuthorise()

  "find application returns Ok and the Application as Json body" in:
    given Request[?] = tdAll.backendRequest
    AuthStubs.stubAuthorise()
    val repo = app.injector.instanceOf[AgentApplicationRepo]
    val exampleAgentApplication = tdAll.llpApplicationAfterCreated
    repo.upsert(exampleAgentApplication).futureValue
    repo.findByInternalUserId(exampleAgentApplication.internalUserId).futureValue.value shouldBe exampleAgentApplication withClue "sanity check"

    val response =
      httpClient
        .get(url"$baseUrl/agent-registration/application")
        .execute[HttpResponse]
        .futureValue
    response.status shouldBe Status.OK
    val agentApplication = response.json.as[AgentApplication]
    agentApplication shouldBe exampleAgentApplication
    AuthStubs.verifyAuthorise()

  "upsert application upserts application to mongo and returns OK" in:
    given Request[?] = tdAll.backendRequest
    AuthStubs.stubAuthorise()
    val repo = app.injector.instanceOf[AgentApplicationRepo]

    repo.findByInternalUserId(tdAll.internalUserId).futureValue shouldBe None withClue "assuming initially there is no records in mongo "
    val exampleAgentApplication: AgentApplication = tdAll.llpApplicationAfterCreated

    val response =
      httpClient
        .post(url"$baseUrl/agent-registration/application")
        .withBody(Json.toJson(exampleAgentApplication))
        .execute[HttpResponse]
        .futureValue
    response.status shouldBe Status.OK
    response.body shouldBe ""

    repo.findByInternalUserId(
      tdAll.internalUserId
    ).futureValue.value shouldBe exampleAgentApplication withClue "after http request there should be records in mongo"
    AuthStubs.verifyAuthorise()

  "find application by linkId returns NO_CONTENT if there is no underlying records" in:

    given Request[?] = tdAll.backendRequest
    val response =
      httpClient
        .get(url"$baseUrl/agent-registration/application/linkId/${tdAll.linkId.value}")
        .execute[HttpResponse]
        .futureValue
    response.status shouldBe Status.NO_CONTENT
    response.body shouldBe ""

  "find application by linkId returns Ok and the Application as Json body" in:

    given Request[?] = tdAll.backendRequest

    val repo = app.injector.instanceOf[AgentApplicationRepo]
    val exampleAgentApplication = tdAll.llpApplicationAfterCreated
    repo.upsert(exampleAgentApplication).futureValue
    repo.findByInternalUserId(exampleAgentApplication.internalUserId).futureValue.value shouldBe exampleAgentApplication withClue "sanity check"

    val response =
      httpClient
        .get(url"$baseUrl/agent-registration/application/linkId/${tdAll.linkId.value}")
        .execute[HttpResponse]
        .futureValue
    response.status shouldBe Status.OK
    val agentApplication = response.json.as[AgentApplication]
    agentApplication shouldBe exampleAgentApplication

  "find application by agentApplicationId returns NO_CONTENT if there is no underlying records" in:

    given Request[?] = tdAll.backendRequest
    AuthStubs.stubAuthoriseIndividual()

    val response =
      httpClient
        .get(url"$baseUrl/agent-registration/application/by-agent-application-id/${tdAll.agentApplicationId.value}")
        .execute[HttpResponse]
        .futureValue
    response.status shouldBe Status.NO_CONTENT
    response.body shouldBe ""
    AuthStubs.verifyAuthorise()

  "find application by agentApplicationId returns Ok and the Application as Json body" in:

    given Request[?] = tdAll.backendRequest
    AuthStubs.stubAuthoriseIndividual()

    val repo = app.injector.instanceOf[AgentApplicationRepo]
    val exampleAgentApplication = tdAll.llpApplicationAfterCreated
    repo.upsert(exampleAgentApplication).futureValue
    repo.findById(exampleAgentApplication.agentApplicationId).futureValue.value shouldBe exampleAgentApplication withClue "sanity check"

    val response =
      httpClient
        .get(url"$baseUrl/agent-registration/application/by-agent-application-id/${tdAll.agentApplicationId.value}")
        .execute[HttpResponse]
        .futureValue
    response.status shouldBe Status.OK
    val agentApplication = response.json.as[AgentApplication]
    agentApplication shouldBe exampleAgentApplication
    AuthStubs.verifyAuthorise()
