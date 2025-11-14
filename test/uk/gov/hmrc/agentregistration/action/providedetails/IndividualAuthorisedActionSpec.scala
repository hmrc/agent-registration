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

package uk.gov.hmrc.agentregistration.action.providedetails

import play.api.mvc.Request
import play.api.mvc.Result
import play.api.mvc.Results.Ok
import uk.gov.hmrc.agentregistration.testsupport.ISpec
import uk.gov.hmrc.auth.core.MissingBearerToken
//import uk.gov.hmrc.agentregistration.shared.InternalUserId
import uk.gov.hmrc.agentregistration.testsupport.wiremock.stubs.providedetails.IndividualAuthStubs

import scala.concurrent.Future

class IndividualAuthorisedActionSpec
extends ISpec:

  "User must be logged in (request must come with an authorisation bearer token) or else the action throws MissingBearerToken exception" in:
    val individualAuthorisedAction: IndividualAuthorisedAction = app.injector.instanceOf[IndividualAuthorisedAction]
    val notLoggedInRequest: Request[?] = tdAll.requestNotLoggedIn

    individualAuthorisedAction
      .invokeBlock(notLoggedInRequest, _ => fakeResultF)
      .failed
      .futureValue shouldBe MissingBearerToken()

    IndividualAuthStubs.verifyAuthorise(0)

  "Successfully authorise when user is logged in with AffinityGroup.Individual and internalId present" in:
    val individualAuthorisedAction: IndividualAuthorisedAction = app.injector.instanceOf[IndividualAuthorisedAction]
    val backendRequest: Request[?] = tdAll.backendRequest

    IndividualAuthStubs.stubAuthorise(
      responseBody =
        // language=JSON
        s"""
           |{
           |  "authorisedEnrolments": [],
           |  "allEnrolments": [],
           |  "affinityGroup": "Individual",
           |  "internalId": "${tdAll.internalUserId.value}"
           |}
           |""".stripMargin
    )

    val result: Result = Ok("AllGood")

    individualAuthorisedAction
      .invokeBlock(
        backendRequest,
        (r: IndividualAuthorisedRequest[?]) =>
          Future.successful {
            r.internalUserId shouldBe tdAll.internalUserId
            result
          }
      )
      .futureValue shouldBe result

    IndividualAuthStubs.verifyAuthorise()

  "Throw RuntimeException when internalId is missing in retrievals" in:
    val individualAuthorisedAction: IndividualAuthorisedAction = app.injector.instanceOf[IndividualAuthorisedAction]
    val backendRequest: Request[?] = tdAll.backendRequest

    IndividualAuthStubs.stubAuthorise(
      responseBody =
        // language=JSON
        s"""
           |{
           |  "authorisedEnrolments": [],
           |  "allEnrolments": [],
           |  "affinityGroup": "Individual"
           |}
           |""".stripMargin
    )

    val ex = intercept[RuntimeException]:
      individualAuthorisedAction
        .invokeBlock(backendRequest, _ => fakeResultF)
        .futureValue

    ex.getMessage should include("Retrievals for internalId is missing")
    IndividualAuthStubs.verifyAuthorise()

  def fakeResultF: Future[Result] = fail("this should not be executed if test works fine")
