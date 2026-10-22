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

package uk.gov.hmrc.agentregistration.testonly.controllers

import play.api.http.Status
import play.api.libs.json.Json
import play.api.mvc.Request
import uk.gov.hmrc.agentregistration.repository.AgentApplicationRepo
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.AgentApplicationFormat.restFormat
import uk.gov.hmrc.agentregistration.testsupport.ControllerSpec
import uk.gov.hmrc.agentregistration.util.RequestSupport.hc
import uk.gov.hmrc.http.HttpReads.Implicits.given
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.http.StringContextOps
import play.api.libs.ws.JsonBodyWritables.given

class TestApplicationControllerSpec
extends ControllerSpec:

  override protected def configOverrides: Map[String, Any] = Map(
    "application.router" -> "testOnlyDoNotUseInAppConf.Routes"
  )

  private lazy val repo: AgentApplicationRepo = app.injector.instanceOf[AgentApplicationRepo]

  "upsertApplication replaces an Expired application whatever the transition, so test tooling can end the post-expiry grace period" in:
    given Request[?] = tdAll.backendRequest
    val expiredAgentApplication: AgentApplication = tdAll.agentApplicationLlp.afterExpired
    val agentApplicationToUpsert: AgentApplication = tdAll.agentApplicationLlp.afterExpiredPostExpiryGracePeriodEnded
    repo.upsert(expiredAgentApplication).futureValue

    val response =
      httpClient
        .post(url"$baseUrl/agent-registration/test-only/application")
        .withBody(Json.toJson(agentApplicationToUpsert))
        .execute[HttpResponse]
        .futureValue
    response.status shouldBe Status.OK

    repo.findById(expiredAgentApplication.agentApplicationId).futureValue.value shouldBe agentApplicationToUpsert
