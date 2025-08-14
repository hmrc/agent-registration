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

package uk.gov.hmrc.agentregistration.action

import play.api.mvc.Request
import play.api.mvc.Result
import uk.gov.hmrc.agentregistration.testsupport.ISpec
import uk.gov.hmrc.agentregistration.testsupport.wiremock.stubs.AuthStubs
import uk.gov.hmrc.auth.core.InternalError
import uk.gov.hmrc.auth.core.MissingBearerToken
import uk.gov.hmrc.auth.core.UnsupportedCredentialRole

import scala.concurrent.Future

class AuthorisedActionSpec
extends ISpec:

  "User must be logged in (request must come with an authorisation bearer token) or else the action throws MissingBearerToken exception" in:
    val authorisedAction: AuthorisedAction = app.injector.instanceOf[AuthorisedAction]
    val notLoggedInRequest: Request[?] = tdAll.requestNotLoggedIn
    authorisedAction
      .invokeBlock(notLoggedInRequest, _ => fakeResultF)
      .failed
      .futureValue shouldBe MissingBearerToken()
    AuthStubs.verifyAuthorise(0)

  "Credential role must be User or Admin or else the action throws UnsupportedCredentialRole exception" in:
    val authorisedAction: AuthorisedAction = app.injector.instanceOf[AuthorisedAction]
    val notLoggedInRequest: Request[?] = tdAll.request
    AuthStubs.stubAuthorise(
      responseBody =
        // language=JSON
        s"""
           |{
           |  "authorisedEnrolments": [],
           |  "allEnrolments": [],
           |  "credentialRole": "Assistant",
           |  "groupIdentifier": "3E7R-E0V0-5V4N-Q5S0",
           |  "agentInformation": {},
           |  "internalId": "${tdAll.internalUserId.value}"
           |}
           |""".stripMargin
    )

    authorisedAction
      .invokeBlock(notLoggedInRequest, _ => fakeResultF)
      .failed
      .futureValue shouldBe UnsupportedCredentialRole(s"UnsupportedCredentialRole: Some(Assistant)")
    AuthStubs.verifyAuthorise()

  "active HMRC-AS-AGENT enrolment must not be assigned to user or else the action throws AuthorisationException exception" in:
    val authorisedAction: AuthorisedAction = app.injector.instanceOf[AuthorisedAction]
    val notLoggedInRequest: Request[?] = tdAll.request
    AuthStubs.stubAuthorise(
      responseBody =
        // language=JSON
        s"""
           |{
           |  "authorisedEnrolments": [],
           |  "allEnrolments": [
           |    {
           |      "key": "HMRC-AS-AGENT",
           |      "identifiers": [
           |        {
           |          "key": "AgentReferenceNumber",
           |          "value": "GARN6552483"
           |        }
           |      ],
           |      "state": "Activated"
           |    }
           |  ],
           |  "credentialRole": "User",
           |  "groupIdentifier": "3E7R-E0V0-5V4N-Q5S0",
           |  "agentInformation": {},
           |  "internalId": "${tdAll.internalUserId.value}"
           |}
           |""".stripMargin
    )

    authorisedAction
      .invokeBlock(notLoggedInRequest, _ => fakeResultF)
      .failed
      .futureValue shouldBe InternalError(s"Enrolment Enrolment(HMRC-AS-AGENT,List(),Activated,None) is assigned to user")

    AuthStubs.verifyAuthorise()

  def fakeResultF: Future[Result] = fail("this should not be executed if test works fine")
