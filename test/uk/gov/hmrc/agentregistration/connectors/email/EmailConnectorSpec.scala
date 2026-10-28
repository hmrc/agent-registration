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

package uk.gov.hmrc.agentregistration.connectors.email

import play.api.mvc.RequestHeader
import play.api.test.FakeRequest
import uk.gov.hmrc.agentregistration.testsupport.ISpec
import uk.gov.hmrc.agentregistration.testsupport.wiremock.stubs.EmailStubs
import uk.gov.hmrc.http.UpstreamErrorResponse

class EmailConnectorSpec
extends ISpec:

  private lazy val emailConnector: EmailConnector = app.injector.instanceOf[EmailConnector]

  "sendEmail completes when the email service responds with 202 Accepted" in:
    given RequestHeader = FakeRequest()
    EmailStubs.stubSendEmail(tdAll.sendEmailRequest)

    emailConnector.sendEmail(tdAll.sendEmailRequest).futureValue shouldBe (())

    EmailStubs.verifySendEmail()

  "sendEmail fails when the email service responds with 500" in:
    given RequestHeader = FakeRequest()
    EmailStubs.stubSendEmailFailure(tdAll.sendEmailRequest)

    val exception: Throwable = emailConnector.sendEmail(tdAll.sendEmailRequest).failed.futureValue

    exception shouldBe a[UpstreamErrorResponse]
    exception.getMessage should include("Failed to send email for template agent_registration_application_expired")
    EmailStubs.verifySendEmail()
