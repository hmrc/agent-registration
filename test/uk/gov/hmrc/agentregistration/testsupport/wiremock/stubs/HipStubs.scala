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

import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.matching.StringValuePattern
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.http.Status
import play.api.libs.json.Json
import uk.gov.hmrc.agentregistration.shared.BusinessPartnerRecordResponse
import uk.gov.hmrc.agentregistration.shared.Utr
import uk.gov.hmrc.agentregistration.testsupport.wiremock.StubMaker

object HipStubs:

  private val identifierSearchUrl = "/mulesoft/customer/v2/api/individuals/identifier-search"
  private val organisationIdentifierSearchUrl = "/mulesoft/customer/v2/api/organisations/identifier-search"

  def stubIdentifierSearchSuccess(
    vrns: List[String] = Nil,
    payeRefs: List[String] = Nil
  ): StubMapping =
    val identifiers =
      vrns.map(vrn =>
        Json.obj(
          "identifier" -> Json.obj("type" -> "VRN", "value" -> vrn),
          "sourceSystems" -> Json.arr(Json.obj("thirdPartyKey" -> vrn, "name" -> "ETMP"))
        )
      ) ++ payeRefs.map(ref =>
        Json.obj(
          "identifier" -> Json.obj("type" -> "EMPREF", "value" -> ref),
          "sourceSystems" -> Json.arr(Json.obj("thirdPartyKey" -> ref, "name" -> "ETMP"))
        )
      )
    StubMaker.make(
      httpMethod = StubMaker.HttpMethod.POST,
      urlPattern = urlPathEqualTo(identifierSearchUrl),
      responseStatus = 200,
      responseBody =
        Json.obj(
          "status" -> Json.obj("code" -> "001", "description" -> "Match found, all results returned"),
          "results" -> Json.arr(
            Json.obj(
              "accountId" -> "001UE00000BDvkLYAT",
              "registryMarker" -> Json.obj("indicator" -> "GREEN", "reasons" -> Json.arr()),
              "identifiers" -> identifiers,
              "givenName" -> "JOHN",
              "familyName" -> "SMITH",
              "deceased" -> false,
              "dateOfBirth" -> "1980-01-01",
              "addresses" -> Json.arr(),
              "telephoneNumbers" -> Json.arr(),
              "emails" -> Json.arr(),
              "hodAccounts" -> Json.arr(),
              "productSubscriptions" -> Json.arr()
            )
          )
        ).toString
    )

  def stubIdentifierSearchNoMatch(): StubMapping = StubMaker.make(
    httpMethod = StubMaker.HttpMethod.POST,
    urlPattern = urlPathEqualTo(identifierSearchUrl),
    responseStatus = 200,
    responseBody =
      Json.obj(
        "status" -> Json.obj("code" -> "004", "description" -> "No match found"),
        "results" -> Json.arr()
      ).toString
  )

  def stubIdentifierSearchError(status: Int): StubMapping = StubMaker.make(
    httpMethod = StubMaker.HttpMethod.POST,
    urlPattern = urlPathEqualTo(identifierSearchUrl),
    responseStatus = status,
    responseBody =
      Json.obj(
        "correlationId" -> "277694eb-a037-46b1-8370-dabb7607eb16",
        "applicationName" -> "ucr-customer-xapi-v1",
        "applicationVersion" -> "2.1.1",
        "applicationEnvironment" -> "Test",
        "timestamp" -> "2024-09-01T16:10:02.446Z",
        "infoMessage" -> Json.obj(
          "errorCode" -> status,
          "errorDescription" -> "A system error has occurred",
          "errorOn" -> "ucr-customer-xapi-v1",
          "errorType" -> "MULE:EXPRESSION"
        )
      ).toString
  )

  def verifyIdentifierSearch(count: Int = 1): Unit = StubMaker.verify(
    httpMethod = StubMaker.HttpMethod.POST,
    urlPattern = urlPathEqualTo(identifierSearchUrl),
    count = count
  )

  def stubOrganisationIdentifierSearchSuccess(
    vrns: List[String] = Nil,
    payeRefs: List[String] = Nil
  ): StubMapping =
    val identifiers =
      vrns.map(vrn =>
        Json.obj(
          "identifier" -> Json.obj(
            "type" -> "VRN",
            "value" -> vrn,
            "invalidFormat" -> false
          ),
          "sourceSystems" -> Json.arr(Json.obj("thirdPartyKey" -> vrn, "name" -> "ETMP"))
        )
      ) ++ payeRefs.map(ref =>
        Json.obj(
          "identifier" -> Json.obj(
            "type" -> "EMPREF",
            "value" -> ref,
            "invalidFormat" -> false
          ),
          "sourceSystems" -> Json.arr(Json.obj("thirdPartyKey" -> ref, "name" -> "ETMP"))
        )
      )
    StubMaker.make(
      httpMethod = StubMaker.HttpMethod.POST,
      urlPattern = urlPathEqualTo(organisationIdentifierSearchUrl),
      responseStatus = 200,
      responseBody =
        Json.obj(
          "status" -> Json.obj("code" -> "001", "description" -> "Match found, all results returned"),
          "results" -> Json.arr(
            Json.obj(
              "accountId" -> "001UE00000BDvk7YAD",
              "entityId" -> "543218976",
              "registryMarker" -> Json.obj("indicator" -> "GREEN", "reasons" -> Json.arr()),
              "organisationName" -> "Jones & Sons",
              "identifiers" -> identifiers,
              "addresses" -> Json.arr(),
              "telephoneNumbers" -> Json.arr(),
              "emails" -> Json.arr(),
              "hodAccounts" -> Json.arr(),
              "productSubscriptions" -> Json.arr()
            )
          )
        ).toString
    )

  def stubOrganisationIdentifierSearchNoMatch(): StubMapping = StubMaker.make(
    httpMethod = StubMaker.HttpMethod.POST,
    urlPattern = urlPathEqualTo(organisationIdentifierSearchUrl),
    responseStatus = 200,
    responseBody =
      Json.obj(
        "status" -> Json.obj("code" -> "004", "description" -> "No match found"),
        "results" -> Json.arr()
      ).toString
  )

  def stubOrganisationIdentifierSearchError(status: Int): StubMapping = StubMaker.make(
    httpMethod = StubMaker.HttpMethod.POST,
    urlPattern = urlPathEqualTo(organisationIdentifierSearchUrl),
    responseStatus = status,
    responseBody =
      Json.obj(
        "correlationId" -> "277694eb-a037-46b1-8370-dabb7607eb16",
        "applicationName" -> "ucr-customer-xapi-v1",
        "applicationVersion" -> "2.1.1",
        "applicationEnvironment" -> "Test",
        "timestamp" -> "2024-09-01T16:10:02.446Z",
        "infoMessage" -> Json.obj(
          "errorCode" -> status,
          "errorDescription" -> "A system error has occurred",
          "errorOn" -> "ucr-customer-xapi-v1",
          "errorType" -> "MULE:EXPRESSION"
        )
      ).toString
  )

  def verifyOrganisationIdentifierSearch(count: Int = 1): Unit = StubMaker.verify(
    httpMethod = StubMaker.HttpMethod.POST,
    urlPattern = urlPathEqualTo(organisationIdentifierSearchUrl),
    count = count
  )

  def stubGetBusinessPartnerRecord(
    utr: Utr,
    hipRegistrationResponse: BusinessPartnerRecordResponse
  ): StubMapping = StubMaker.make(
    httpMethod = StubMaker.HttpMethod.POST,
    urlPattern = urlMatching(s"/RESTAdapter/registration/utr/${utr.value}"),
    requestBody = Some(expectedRequestBody),
    responseStatus = Status.OK,
    responseBody = expectedBprResponseBody(hipRegistrationResponse)
  )

  def stubGetBusinessPartnerRecordNotFound(
    utr: Utr
  ): StubMapping = StubMaker.make(
    httpMethod = StubMaker.HttpMethod.POST,
    urlPattern = urlMatching(s"/RESTAdapter/registration/utr/${utr.value}"),
    requestBody = Some(expectedRequestBody),
    responseStatus = Status.UNPROCESSABLE_ENTITY,
    responseBody = expectedBprNotFoundResponseBody
  )

  def verifyGetBusinessPartnerRecord(
    utr: Utr,
    count: Int = 1
  ): Unit = StubMaker.verify(
    httpMethod = StubMaker.HttpMethod.POST,
    urlPattern = urlMatching(s"/RESTAdapter/registration/utr/${utr.value}"),
    count = count
  )

  private val expectedBprNotFoundResponseBody =
    // language=JSON
    """
      |{
      |  "errors": {
      |    "code": "002",
      |    "processingDate": "2022-01-31T09:26:17Z",
      |    "text": "No match found"
      |  }
      |}
      |""".stripMargin

  private def expectedBprResponseBody(
    hipRegistrationResponse: BusinessPartnerRecordResponse
  ) = {
    // language=JSON
    s"""
       |{
       |  "success": {
       |    "address" : {
       |      "addressLine1" : "${hipRegistrationResponse.address.addressLine1}",
       |      "addressLine2" : "${hipRegistrationResponse.address.addressLine2.getOrElse("")}",
       |      "postalCode" : "${hipRegistrationResponse.address.postalCode.getOrElse("")}",
       |      "countryCode" : "GB"
       |    },
       |    "agentReferenceNumber" : "${hipRegistrationResponse.agentReferenceNumber.map(_.value).getOrElse("")}",
       |    "contactDetails" : {
       |      "primaryPhoneNumber" : "${hipRegistrationResponse.primaryPhoneNumber.getOrElse("")}",
       |      "secondaryPhoneNumber" : "09923 317218",
       |      "faxNumber" : "09923 317218",
       |      "emailAddress" : "${hipRegistrationResponse.emailAddress.getOrElse("")}"
       |    },
       |    "organisation" : {
       |      "organisationName" : "${hipRegistrationResponse.organisationName.getOrElse("")}",
       |      "isAGroup" : true,
       |      "organisationType" : " 5T"
       |    },
       |    "isAnASAgent": false,
       |    "isAnAgent": false,
       |    "isAnIndividual": false,
       |    "isEditable": true,
       |    "safeId" : "X00000123456789",
       |    "sapNumber": "1234567890"
       |  }
       |}
       |""".stripMargin
  }

  private val expectedRequestBody: StringValuePattern = equalToJson(
    // language=JSON
    """
        {
          "requiresNameMatch": false,
          "regime": "ITSA",
          "isAnAgent": false
        }
      |""".stripMargin
  )
