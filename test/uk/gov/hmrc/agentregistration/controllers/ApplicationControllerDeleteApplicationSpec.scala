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
import uk.gov.hmrc.agentregistration.repository.AgentApplicationRepo
import uk.gov.hmrc.agentregistration.repository.providedetails.llp.IndividualProvidedDetailsRepo
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.amls.AmlsEvidence
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetails
import uk.gov.hmrc.agentregistration.testsupport.ControllerSpec
import uk.gov.hmrc.agentregistration.testsupport.wiremock.stubs.AuthStubs
import uk.gov.hmrc.agentregistration.testsupport.wiremock.stubs.ObjectStoreStubs
import uk.gov.hmrc.agentregistration.util.RequestSupport.hc
import uk.gov.hmrc.http.HttpReads.Implicits.given
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.http.StringContextOps

class ApplicationControllerDeleteApplicationSpec
extends ControllerSpec:

  override protected def configOverrides: Map[String, Any] = Map[String, Any](
    "scheduler.deletion.enabled" -> true
  )

  "delete application deletes the application, its individuals and its AMLS evidence, and returns NO_CONTENT" in:
    given Request[?] = tdAll.backendRequest
    AuthStubs.stubAuthorise()
    val agentApplicationRepo: AgentApplicationRepo = app.injector.instanceOf[AgentApplicationRepo]
    val individualProvidedDetailsRepo: IndividualProvidedDetailsRepo = app.injector.instanceOf[IndividualProvidedDetailsRepo]
    val agentApplication: AgentApplication = tdAll.agentApplicationLlp.afterExpiredWithAmlsEvidencePostExpiryGracePeriodEnded
    val individualProvidedDetails: IndividualProvidedDetails = tdAll.providedDetails.afterStarted
    val amlsEvidence: AmlsEvidence = agentApplication.getAmlsDetails.amlsEvidence.value
    agentApplicationRepo.upsert(agentApplication).futureValue
    individualProvidedDetailsRepo.upsert(individualProvidedDetails).futureValue
    ObjectStoreStubs.stubListAmlsEvidence(amlsEvidence)
    ObjectStoreStubs.stubDeleteAmlsEvidence(amlsEvidence)

    val response: HttpResponse =
      httpClient
        .delete(url"$baseUrl/agent-registration/application")
        .execute[HttpResponse]
        .futureValue

    response.status shouldBe Status.NO_CONTENT
    response.body shouldBe ""
    ObjectStoreStubs.verifyDeleteAmlsEvidence(amlsEvidence)
    agentApplicationRepo.findById(agentApplication.agentApplicationId).futureValue shouldBe None
    individualProvidedDetailsRepo.findForApplication(agentApplication.agentApplicationId).futureValue shouldBe empty
    AuthStubs.verifyAuthorise()

  "delete application returns NO_CONTENT when there is no application" in:
    given Request[?] = tdAll.backendRequest
    AuthStubs.stubAuthorise()

    val response: HttpResponse =
      httpClient
        .delete(url"$baseUrl/agent-registration/application")
        .execute[HttpResponse]
        .futureValue

    response.status shouldBe Status.NO_CONTENT
    response.body shouldBe ""
    AuthStubs.verifyAuthorise()
