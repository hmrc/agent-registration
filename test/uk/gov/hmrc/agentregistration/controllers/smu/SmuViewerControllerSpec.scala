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

package uk.gov.hmrc.agentregistration.controllers.smu

import play.api.http.Status
import play.api.mvc.Request
import uk.gov.hmrc.agentregistration.model.smu.SmuIndividualResponse
import uk.gov.hmrc.agentregistration.repository.AgentApplicationRepo
import uk.gov.hmrc.agentregistration.repository.providedetails.llp.IndividualProvidedDetailsRepo
import uk.gov.hmrc.agentregistration.shared.ApplicationState.SentForRisking
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetails
import uk.gov.hmrc.agentregistration.shared.risking.ApplicationReference
import uk.gov.hmrc.agentregistration.testsupport.ControllerSpec
import uk.gov.hmrc.agentregistration.testsupport.testdata.TdAll.tdAll.personReference
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
    val individualProvidedDetails = tdAll.providedDetails.afterStarted.copy(personReference = personReference)
    individualProvidedDetailsRepo.upsert(individualProvidedDetails).futureValue
    val applicationRepo = app.injector.instanceOf[AgentApplicationRepo]
    val agentApplication = tdAll.agentApplicationLlp.afterStarted.copy(applicationState = SentForRisking)
    applicationRepo.upsert(agentApplication).futureValue

    individualProvidedDetailsRepo.findById(
      individualProvidedDetails.individualProvidedDetailsId
    ).futureValue.value shouldBe individualProvidedDetails withClue "sanity check"

    applicationRepo.findById(
      agentApplication.agentApplicationId
    ).futureValue.value shouldBe agentApplication withClue "sanity check"

    val response: HttpResponse =
      httpClient
        .get(url"$baseUrl/agent-registration/smu-viewer/individual/by-person-reference/${personReference.value}")
        .execute[HttpResponse]
        .futureValue
    response.status shouldBe Status.OK
    val smuViewerIndividualResponse: SmuIndividualResponse = response.json.as[SmuIndividualResponse]
    smuViewerIndividualResponse.personReference shouldBe personReference
    smuViewerIndividualResponse.applicationReference shouldBe ApplicationReference("AGENTAPPLICA")
    smuViewerIndividualResponse.individualProvidedDetailsId shouldBe individualProvidedDetails.individualProvidedDetailsId
    smuViewerIndividualResponse.individualName shouldBe individualProvidedDetails.individualName
    smuViewerIndividualResponse.isPersonOfControl shouldBe individualProvidedDetails.isPersonOfControl
    smuViewerIndividualResponse.internalUserId shouldBe individualProvidedDetails.internalUserId
    smuViewerIndividualResponse.providedDetailsState shouldBe individualProvidedDetails.providedDetailsState
    smuViewerIndividualResponse.individualDateOfBirth shouldBe individualProvidedDetails.individualDateOfBirth
    smuViewerIndividualResponse.telephoneNumber shouldBe individualProvidedDetails.telephoneNumber
    smuViewerIndividualResponse.emailAddress shouldBe individualProvidedDetails.emailAddress
    smuViewerIndividualResponse.individualNino shouldBe individualProvidedDetails.individualNino
    smuViewerIndividualResponse.individualSaUtr shouldBe individualProvidedDetails.individualSaUtr
    smuViewerIndividualResponse.individualVrns shouldBe individualProvidedDetails.vrns
    smuViewerIndividualResponse.individualPayeRefs shouldBe individualProvidedDetails.payeRefs
    smuViewerIndividualResponse.passedIv shouldBe individualProvidedDetails.passedIv
    smuViewerIndividualResponse.hmrcStandardForAgentsAgreed shouldBe individualProvidedDetails.hmrcStandardForAgentsAgreed
    smuViewerIndividualResponse.hasApprovedApplication shouldBe individualProvidedDetails.hasApprovedApplication
    smuViewerIndividualResponse.linkId shouldBe agentApplication.linkId
    smuViewerIndividualResponse.groupId shouldBe agentApplication.groupId
    smuViewerIndividualResponse.applicationState shouldBe agentApplication.applicationState
    smuViewerIndividualResponse.businessType shouldBe agentApplication.businessType
    smuViewerIndividualResponse.userRole shouldBe agentApplication.userRole
    smuViewerIndividualResponse.applicantContactDetails shouldBe agentApplication.applicantContactDetails
    smuViewerIndividualResponse.amlsDetails shouldBe agentApplication.amlsDetails
    smuViewerIndividualResponse.agentDetails shouldBe agentApplication.agentDetails
    smuViewerIndividualResponse.refusalToDealWithCheckResult shouldBe agentApplication.refusalToDealWithCheckResult
    smuViewerIndividualResponse.numberOfIndividuals shouldBe agentApplication.numberOfIndividuals
    smuViewerIndividualResponse.hasOtherRelevantIndividuals shouldBe agentApplication.hasOtherRelevantIndividuals
    smuViewerIndividualResponse.entityVrns shouldBe agentApplication.vrns
    smuViewerIndividualResponse.entityPayeRefs shouldBe agentApplication.payeRefs
    smuViewerIndividualResponse.businessDetails shouldBe agentApplication.businessDetails
    smuViewerIndividualResponse.companyProfile shouldBe agentApplication.asLlpApplication.businessDetails.map(_.companyProfile)
    smuViewerIndividualResponse.deceasedCheckResult shouldBe None
    smuViewerIndividualResponse.companyStatusCheckResult shouldBe agentApplication.companyStatusCheckResult
