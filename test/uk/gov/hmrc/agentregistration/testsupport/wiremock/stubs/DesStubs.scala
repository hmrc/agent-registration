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
import play.api.libs.json.Json
import uk.gov.hmrc.agentregistration.shared.BusinessPartnerRecordResponse
import uk.gov.hmrc.agentregistration.shared.InternalUserId
import uk.gov.hmrc.agentregistration.shared.Utr
import uk.gov.hmrc.agentregistration.testsupport.testdata.TdAll
import uk.gov.hmrc.agentregistration.testsupport.wiremock.StubMaker

object DesStubs {

  def stubGetBusinessPartnerRecord(
    utr: Utr,
    desRegistrationResponse: BusinessPartnerRecordResponse
  ): StubMapping = StubMaker.make(
    httpMethod = StubMaker.HttpMethod.POST,
    urlPattern = wm.urlMatching(s"/registration/individual/utr/${utr.value}"),
    requestBody = Some(expectedRequestBody),
    responseStatus = Status.OK,
    responseBody = expectedResponseBody(utr, desRegistrationResponse)
  )

  private def expectedResponseBody(
    utr: Utr,
    desRegistrationResponse: BusinessPartnerRecordResponse
  ) =
    // language=JSON
    s"""
      {
        "businessPartnerExists" : true,
        "safeId" : "X00000123456789",
        "agentReferenceNumber" : "",
        "uniqueTaxReference" : "${utr.value}",
        "utr" : "${utr.value}",
        "urn" : "",
        "nino" : "",
        "eori" : "",
        "crn" : "12345678",
        "isAnAgent" : false,
        "isAnASAgent" : false,
        "isAnIndividual" : false,
        "isAnOrganisation" : true,
        "organisation" : {
          "organisationName" : "${desRegistrationResponse.organisationName.getOrElse("")}",
          "isAGroup" : true,
          "organisationType" : " 5T"
        },
        "address" : {
          "addressLine1" : "${desRegistrationResponse.address.addressLine1}",
          "addressLine2" : "${desRegistrationResponse.address.addressLine2.getOrElse("")}",
          "postalCode" : "${desRegistrationResponse.address.postalCode.getOrElse("")}",
          "countryCode" : "GB"
        },
        "contactDetails" : {
          "primaryPhoneNumber" : "${desRegistrationResponse.primaryPhoneNumber.getOrElse("")}",
          "mobileNumber" : "09923 317218",
          "faxNumber" : "09923 317218",
          "emailAddress" : "${desRegistrationResponse.emailAddress.getOrElse("")}"
        }
      }
    """

  private val expectedRequestBody: StringValuePattern = wm.equalToJson(
    // language=JSON
    """
      {
        "requiresNameMatch": false,
        "regime": "ITSA",
        "isAnAgent": false
      }
      |""".stripMargin
  )

  def verifyGetBusinessPartnerRecord(
    utr: Utr,
    count: Int = 1
  ): Unit = StubMaker.verify(
    httpMethod = StubMaker.HttpMethod.POST,
    urlPattern = wm.urlMatching(s"/registration/individual/utr/${utr.value}"),
    count = count
  )

}
