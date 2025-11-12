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

package uk.gov.hmrc.agentregistration.testsupport.wiremock.stubs

import com.github.tomakehurst.wiremock.client.WireMock as wm
import com.github.tomakehurst.wiremock.matching.StringValuePattern
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.http.Status
import uk.gov.hmrc.agentregistration.shared.InternalUserId
import uk.gov.hmrc.agentregistration.testsupport.testdata.TdAll
import uk.gov.hmrc.agentregistration.testsupport.wiremock.StubMaker

object AuthStubs {

  def stubAuthorise(
    responseBody: String = responseBodyAsCleanAgent()
  ): StubMapping = StubMaker.make(
    httpMethod = StubMaker.HttpMethod.POST,
    urlPattern = wm.urlMatching("/auth/authorise"),
    requestBody = Some(expectedRequestBodyAgent),
    responseStatus = Status.OK,
    responseBody = responseBody
  )

  def stubAuthoriseIndividual(
    responseBody: String = responseBodyAsIndividual()
  ): StubMapping = StubMaker.make(
    httpMethod = StubMaker.HttpMethod.POST,
    urlPattern = wm.urlMatching("/auth/authorise"),
    requestBody = Some(expectedRequestBodyIndividual),
    responseStatus = Status.OK,
    responseBody = responseBody
  )

  def verifyAuthorise(count: Int = 1): Unit = StubMaker.verify(
    httpMethod = StubMaker.HttpMethod.POST,
    urlPattern = wm.urlMatching("/auth/authorise"),
    count = count
  )

  def responseBodyAsCleanAgent(internalUserId: InternalUserId = TdAll.tdAll.internalUserId): String =
    // language=JSON
    s"""
       {
         "authorisedEnrolments": [],
         "allEnrolments": [],
         "credentialRole": "User",
         "groupIdentifier": "3E7R-E0V0-5V4N-Q5S0",
         "agentInformation": {},
         "internalId": "${internalUserId.value}"
       }
    """

  private def responseBodyAsIndividual(
    internalUserId: InternalUserId = TdAll.tdAll.internalUserId
  ): String = {
    // language=JSON
    """
    {
     "allEnrolments": [{
       "key": "MTD-IT",
       "identifiers": [{
         "key": "AnyIdentifier",
         "value": "AnyValue"
       }]
     }],
     "groupIdentifier": "3E7R-E0V0-5V4N-Q5S0",
     "affinityGroup": "Individual",
     "confidenceLevel": 250,
     "internalId": "${internalUserId.value}"
    }
    """
  }

  private val expectedRequestBodyAgent: StringValuePattern = wm.equalToJson(
    // language=JSON
    """
    {
      "authorise": [
        {
          "authProviders": [
            "GovernmentGateway"
          ]
        },
        {
          "affinityGroup": "Agent"
        }
      ],
      "retrieve": [
        "allEnrolments",
        "credentialRole",
        "internalId"
      ]
    }
    """
  )

  private val expectedRequestBodyIndividual: StringValuePattern = wm.equalToJson(
    // language=JSON
    """
    {
      "authorise": [
      {
        "authProviders": [
          "GovernmentGateway"
        ]
      },
      {
        "affinityGroup": "Individual"
      }
      ],
      "retrieve": [
        "allEnrolments",
        "internalId"
      ]
    }
    """
  )

}
