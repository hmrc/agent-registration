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
import uk.gov.hmrc.agentregistration.shared.DesBusinessAddress
import uk.gov.hmrc.agentregistration.shared.BusinessPartnerRecordResponse
import uk.gov.hmrc.agentregistration.testsupport.ControllerSpec
import uk.gov.hmrc.agentregistration.testsupport.wiremock.stubs.AuthStubs
import uk.gov.hmrc.agentregistration.testsupport.wiremock.stubs.DesStubs
import uk.gov.hmrc.agentregistration.util.RequestSupport.hc
import uk.gov.hmrc.http.HttpReads.Implicits.given
import uk.gov.hmrc.http.HttpReads
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.http.StringContextOps

class BusinessPartnerRecordControllerSpec
extends ControllerSpec:

  val desRegistrationResponse: BusinessPartnerRecordResponse = BusinessPartnerRecordResponse(
    organisationName = Some("Test Company Name"),
    individualName = None,
    address = DesBusinessAddress(
      addressLine1 = "Line 1",
      addressLine2 = Some("Line 2"),
      addressLine3 = None,
      addressLine4 = None,
      postalCode = Some("AB1 2CD"),
      countryCode = "GB"
    ),
    emailAddress = Some(tdAll.email),
    primaryPhoneNumber = Some(tdAll.telephoneNumber)
  )

  "getBusinessPartnerRecord by UTR returns Ok and DesRegistrationResponse as Json body" in:
    given Request[?] = tdAll.backendRequest
    AuthStubs.stubAuthorise()
    DesStubs.stubGetBusinessPartnerRecord(
      utr = tdAll.utr,
      desRegistrationResponse = desRegistrationResponse
    )
    val response =
      httpClient
        .get(url"$baseUrl/agent-registration/business-partner-record/utr/${tdAll.utr.value}")
        .execute[HttpResponse]
        .futureValue
    response.status shouldBe Status.OK
    val responseAsDesRegistrationResponse = response.json.as[BusinessPartnerRecordResponse]
    responseAsDesRegistrationResponse shouldBe desRegistrationResponse
    AuthStubs.verifyAuthorise()
    DesStubs.verifyGetBusinessPartnerRecord(tdAll.utr)

  "getBusinessPartnerRecord by UTR returns NoContent if no records found" in:
    given Request[?] = tdAll.backendRequest
    AuthStubs.stubAuthorise()
    DesStubs.stubGetBusinessPartnerRecordNotFound(
      utr = tdAll.utr
    )
    val response =
      httpClient
        .get(url"$baseUrl/agent-registration/business-partner-record/utr/${tdAll.utr.value}")
        .execute[HttpResponse]
        .futureValue
    response.status shouldBe Status.NO_CONTENT

    response.body shouldBe ""
    AuthStubs.verifyAuthorise()
    DesStubs.verifyGetBusinessPartnerRecord(tdAll.utr)
