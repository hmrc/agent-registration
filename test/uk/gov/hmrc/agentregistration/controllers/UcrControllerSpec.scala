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

package uk.gov.hmrc.agentregistration.controllers

import play.api.http.Status
import play.api.mvc.Request
import uk.gov.hmrc.agentregistration.shared.UcrIdentifiers
import uk.gov.hmrc.agentregistration.testsupport.ControllerSpec
import uk.gov.hmrc.agentregistration.testsupport.wiremock.stubs.AuthStubs
import uk.gov.hmrc.agentregistration.testsupport.wiremock.stubs.HipStubs
import uk.gov.hmrc.agentregistration.util.RequestSupport.hc
import uk.gov.hmrc.http.HttpReads.Implicits.given
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.http.StringContextOps

class UcrControllerSpec
extends ControllerSpec:

  "searchByNino" - {

    "returns Ok with VRNs and PAYE refs when HIP returns a match" in:
      given Request[?] = tdAll.backendRequest
      AuthStubs.stubAuthorise()
      HipStubs.stubIdentifierSearchSuccess(
        vrns = List("462783770"),
        payeRefs = List("123/A45678")
      )
      val response =
        httpClient
          .get(url"$baseUrl/agent-registration/unified-customer-registry/individual/nino/${tdAll.nino.value}")
          .execute[HttpResponse]
          .futureValue
      response.status shouldBe Status.OK
      val ucrIdentifiers = response.json.as[UcrIdentifiers]
      ucrIdentifiers.hasIdentifiers.shouldBe(true)
      ucrIdentifiers.vrns.map(_.value).shouldBe(List("462783770"))
      ucrIdentifiers.payeRefs.map(_.value).shouldBe(List("123/A45678"))
      AuthStubs.verifyAuthorise()
      HipStubs.verifyIdentifierSearch()

    "returns Ok with empty lists when HIP returns no match" in:
      given Request[?] = tdAll.backendRequest
      AuthStubs.stubAuthorise()
      HipStubs.stubIdentifierSearchNoMatch()
      val response =
        httpClient
          .get(url"$baseUrl/agent-registration/unified-customer-registry/individual/nino/${tdAll.nino.value}")
          .execute[HttpResponse]
          .futureValue
      response.status shouldBe Status.OK
      val ucrIdentifiers = response.json.as[UcrIdentifiers]
      ucrIdentifiers.hasIdentifiers.shouldBe(false)
      ucrIdentifiers.vrns.shouldBe(List.empty)
      ucrIdentifiers.payeRefs.shouldBe(List.empty)
      AuthStubs.verifyAuthorise()
      HipStubs.verifyIdentifierSearch()

    "returns 500 when HIP returns an error" in:
      given Request[?] = tdAll.backendRequest
      AuthStubs.stubAuthorise()
      HipStubs.stubIdentifierSearchError(500)
      val response =
        httpClient
          .get(url"$baseUrl/agent-registration/unified-customer-registry/individual/nino/${tdAll.nino.value}")
          .execute[HttpResponse]
          .futureValue
      response.status shouldBe Status.INTERNAL_SERVER_ERROR
      AuthStubs.verifyAuthorise()
      HipStubs.verifyIdentifierSearch()

  }

  "searchBySaUtr" - {

    "returns Ok with VRNs and PAYE refs when HIP returns a match" in:
      given Request[?] = tdAll.backendRequest
      AuthStubs.stubAuthorise()
      HipStubs.stubIdentifierSearchSuccess(
        vrns = List("462783770"),
        payeRefs = List("123/A45678")
      )
      val response =
        httpClient
          .get(url"$baseUrl/agent-registration/unified-customer-registry/individual/sa-utr/${tdAll.saUtr.value}")
          .execute[HttpResponse]
          .futureValue
      response.status shouldBe Status.OK
      val ucrIdentifiers = response.json.as[UcrIdentifiers]
      ucrIdentifiers.hasIdentifiers.shouldBe(true)
      ucrIdentifiers.vrns.map(_.value).shouldBe(List("462783770"))
      ucrIdentifiers.payeRefs.map(_.value).shouldBe(List("123/A45678"))
      AuthStubs.verifyAuthorise()
      HipStubs.verifyIdentifierSearch()

    "returns Ok with empty lists when HIP returns no match" in:
      given Request[?] = tdAll.backendRequest
      AuthStubs.stubAuthorise()
      HipStubs.stubIdentifierSearchNoMatch()
      val response =
        httpClient
          .get(url"$baseUrl/agent-registration/unified-customer-registry/individual/sa-utr/${tdAll.saUtr.value}")
          .execute[HttpResponse]
          .futureValue
      response.status shouldBe Status.OK
      val ucrIdentifiers = response.json.as[UcrIdentifiers]
      ucrIdentifiers.hasIdentifiers.shouldBe(false)
      AuthStubs.verifyAuthorise()
      HipStubs.verifyIdentifierSearch()

  }

  "searchOrganisationByUtr" - {

    "returns Ok with VRNs and PAYE refs when HIP returns a match" in:
      given Request[?] = tdAll.backendRequest
      AuthStubs.stubAuthorise()
      HipStubs.stubOrganisationIdentifierSearchSuccess(
        vrns = List("462783770"),
        payeRefs = List("123/A45678")
      )
      val response =
        httpClient
          .get(url"$baseUrl/agent-registration/unified-customer-registry/organisation/utr/${tdAll.utr.value}")
          .execute[HttpResponse]
          .futureValue
      response.status shouldBe Status.OK
      val ucrIdentifiers = response.json.as[UcrIdentifiers]
      ucrIdentifiers.hasIdentifiers.shouldBe(true)
      ucrIdentifiers.vrns.map(_.value).shouldBe(List("462783770"))
      ucrIdentifiers.payeRefs.map(_.value).shouldBe(List("123/A45678"))
      AuthStubs.verifyAuthorise()
      HipStubs.verifyOrganisationIdentifierSearch()

    "returns Ok with empty lists when HIP returns no match" in:
      given Request[?] = tdAll.backendRequest
      AuthStubs.stubAuthorise()
      HipStubs.stubOrganisationIdentifierSearchNoMatch()
      val response =
        httpClient
          .get(url"$baseUrl/agent-registration/unified-customer-registry/organisation/utr/${tdAll.utr.value}")
          .execute[HttpResponse]
          .futureValue
      response.status shouldBe Status.OK
      val ucrIdentifiers = response.json.as[UcrIdentifiers]
      ucrIdentifiers.hasIdentifiers.shouldBe(false)
      ucrIdentifiers.vrns.shouldBe(List.empty)
      ucrIdentifiers.payeRefs.shouldBe(List.empty)
      AuthStubs.verifyAuthorise()
      HipStubs.verifyOrganisationIdentifierSearch()

    "returns 500 when HIP returns an error" in:
      given Request[?] = tdAll.backendRequest
      AuthStubs.stubAuthorise()
      HipStubs.stubOrganisationIdentifierSearchError(500)
      val response =
        httpClient
          .get(url"$baseUrl/agent-registration/unified-customer-registry/organisation/utr/${tdAll.utr.value}")
          .execute[HttpResponse]
          .futureValue
      response.status shouldBe Status.INTERNAL_SERVER_ERROR
      AuthStubs.verifyAuthorise()
      HipStubs.verifyOrganisationIdentifierSearch()

  }
