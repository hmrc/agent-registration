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
import play.api.mvc.Request
import uk.gov.hmrc.agentregistration.model.SmuViewerIndividualResponse
import uk.gov.hmrc.agentregistration.repository.AgentApplicationRepo
import uk.gov.hmrc.agentregistration.repository.providedetails.llp.IndividualProvidedDetailsRepo
import uk.gov.hmrc.agentregistration.shared.ApplicationState.Submitted
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetails
import uk.gov.hmrc.agentregistration.testsupport.ControllerSpec
import uk.gov.hmrc.agentregistration.testsupport.testdata.TdAll.tdAll.individualProvidedDetailsId
import uk.gov.hmrc.agentregistration.testsupport.wiremock.stubs.AuthStubs
import uk.gov.hmrc.agentregistration.util.RequestSupport.hc
import uk.gov.hmrc.http.HttpReads.Implicits.given
import uk.gov.hmrc.http.HttpReads
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.http.StringContextOps

class SmuViewerControllerSpec
extends ControllerSpec:

  "find individual by person reference returns Ok and SpecialManagementUnitViewerIndividualResponse as Json body" in:

    given Request[?] = tdAll.backendRequest
    AuthStubs.stubAuthorise()
    val individualProvidedDetailsRepo: IndividualProvidedDetailsRepo = app.injector.instanceOf[IndividualProvidedDetailsRepo]
    val individualProvidedDetails = tdAll.providedDetailsLlp.afterStarted
    individualProvidedDetailsRepo.upsert(individualProvidedDetails).futureValue
    val applicationRepo = app.injector.instanceOf[AgentApplicationRepo]
    val agentApplication = tdAll.llpApplicationAfterCreated.copy(applicationState = Submitted)
    applicationRepo.upsert(agentApplication).futureValue

    individualProvidedDetailsRepo.findById(
      individualProvidedDetails.individualProvidedDetailsId
    ).futureValue.value shouldBe individualProvidedDetails withClue "sanity check"

    applicationRepo.findById(
      agentApplication.agentApplicationId
    ).futureValue.value shouldBe agentApplication withClue "sanity check"

    val response =
      httpClient
        .get(url"$baseUrl/agent-registration/smu-viewer/individual/by-person-reference/${individualProvidedDetailsId.value}")
        .execute[HttpResponse]
        .futureValue
    response.status shouldBe Status.OK
    val individualAddDetailsResponse = response.json.as[SmuViewerIndividualResponse]
    individualAddDetailsResponse.individualProvidedDetailsId shouldBe individualProvidedDetailsId
    individualAddDetailsResponse.linkId shouldBe agentApplication.linkId
