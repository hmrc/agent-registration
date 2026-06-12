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

package uk.gov.hmrc.agentregistration.testsupport.wiremock.stubs

import com.github.tomakehurst.wiremock.client.WireMock as wm
import com.github.tomakehurst.wiremock.client.WireMock.equalToJson
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.libs.json.Json
import uk.gov.hmrc.agentregistration.model.SendEmailRequest
import uk.gov.hmrc.agentregistration.testsupport.wiremock.StubMaker

object EmailStubs:

  private val sendEmailUrl: String = "/hmrc/email"

  def stubSendEmail(sendEmailRequest: SendEmailRequest): StubMapping = stub(sendEmailRequest, responseStatus = 202)

  def stubSendEmailFailure(sendEmailRequest: SendEmailRequest): StubMapping = stub(sendEmailRequest, responseStatus = 500)

  private def stub(
    sendEmailRequest: SendEmailRequest,
    responseStatus: Int
  ): StubMapping = StubMaker.make(
    httpMethod = StubMaker.HttpMethod.POST,
    urlPattern = wm.urlEqualTo(sendEmailUrl),
    requestBody = Some(equalToJson(Json.prettyPrint(Json.toJson(sendEmailRequest)))),
    responseStatus = responseStatus
  )

  def verifySendEmail(count: Int = 1): Unit = StubMaker.verify(
    httpMethod = StubMaker.HttpMethod.POST,
    urlPattern = wm.urlEqualTo(sendEmailUrl),
    count = count
  )
