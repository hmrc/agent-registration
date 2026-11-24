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

package uk.gov.hmrc.agentregistration.services

import play.api.http.Status
import play.api.mvc.RequestHeader
import uk.gov.hmrc.agentregistration.repository.AgentApplicationRepo
import uk.gov.hmrc.agentregistration.repository.providedetails.llp.IndividualProvidedDetailsRepo
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.AgentApplicationId
import uk.gov.hmrc.agentregistration.shared.BusinessType
import uk.gov.hmrc.agentregistration.shared.amls.AmlsEvidence
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetails
import uk.gov.hmrc.agentregistration.shared.testdata.agentapplication.ExpiryTransformations.*
import uk.gov.hmrc.agentregistration.testsupport.ISpec
import uk.gov.hmrc.agentregistration.testsupport.testdata.AgentApplicationStates
import uk.gov.hmrc.agentregistration.testsupport.wiremock.stubs.ObjectStoreStubs

class AgentApplicationDeletionServiceSpec
extends ISpec:

  private lazy val agentApplicationDeletionService: AgentApplicationDeletionService = app.injector.instanceOf[AgentApplicationDeletionService]
  private lazy val agentApplicationRepo: AgentApplicationRepo = app.injector.instanceOf[AgentApplicationRepo]
  private lazy val individualProvidedDetailsRepo: IndividualProvidedDetailsRepo = app.injector.instanceOf[IndividualProvidedDetailsRepo]

  private given RequestHeader = tdAll.fakeBackendRequest

  private lazy val individualProvidedDetails: IndividualProvidedDetails = tdAll.providedDetails.afterStarted

  private final case class TestCase(
    description: String,
    agentApplication: AgentApplication,
    expectedAgentApplicationAfterRun: Option[AgentApplication],
    expectedIndividualsAfterRun: List[IndividualProvidedDetails]
  )

  BusinessType.values.foreach: businessType =>
    val agentApplicationStates: AgentApplicationStates = AgentApplicationStates.forBusinessType(businessType)

    Seq(
      TestCase(
        description = "deletes an application whose grace period has ended and that was sent the deletion email, together with its individuals",
        agentApplication = agentApplicationStates.withVerifiedEmail.makeOverdue.makeExpiredEmailSent.makeDeletionEmailSent,
        expectedAgentApplicationAfterRun = None,
        expectedIndividualsAfterRun = List.empty
      ),
      TestCase(
        description = "deletes an application with no verified email address without waiting for a deletion email, together with its individuals",
        agentApplication = agentApplicationStates.started.makeOverdue.makeExpired.makeDueForDeletion,
        expectedAgentApplicationAfterRun = None,
        expectedIndividualsAfterRun = List.empty
      ),
      TestCase(
        description = "keeps an application with a verified email address until the deletion email is sent, together with its individuals",
        agentApplication = agentApplicationStates.withVerifiedEmail.makeOverdue.makeExpiredEmailSent.makeDueForDeletion,
        expectedAgentApplicationAfterRun = Some(agentApplicationStates.withVerifiedEmail.makeOverdue.makeExpiredEmailSent.makeDueForDeletion),
        expectedIndividualsAfterRun = List(individualProvidedDetails)
      ),
      TestCase(
        description = "keeps an application still in the grace period, together with its individuals",
        agentApplication = agentApplicationStates.started.makeOverdue.makeExpired,
        expectedAgentApplicationAfterRun = Some(agentApplicationStates.started.makeOverdue.makeExpired),
        expectedIndividualsAfterRun = List(individualProvidedDetails)
      )
    ).foreach: testCase =>
      s"processDeletions ${testCase.description} ($businessType)" in:
        val agentApplicationId: AgentApplicationId = testCase.agentApplication.agentApplicationId
        individualProvidedDetails.agentApplicationId shouldBe agentApplicationId withClue "precondition: the individual must belong to the application"
        agentApplicationRepo.upsert(testCase.agentApplication).futureValue
        individualProvidedDetailsRepo.upsert(individualProvidedDetails).futureValue

        agentApplicationDeletionService.processDeletions().futureValue

        agentApplicationRepo.findById(agentApplicationId).futureValue shouldBe testCase.expectedAgentApplicationAfterRun
        individualProvidedDetailsRepo.findForApplication(agentApplicationId).futureValue shouldBe testCase.expectedIndividualsAfterRun

  private lazy val deletionEmailSentWithAmlsEvidence: AgentApplication =
    tdAll.agentApplicationLlp.afterAmlsCompleteWithEvidence.makeOverdue.makeExpiredEmailSent.makeDeletionEmailSent

  private lazy val amlsEvidence: AmlsEvidence = deletionEmailSentWithAmlsEvidence.getAmlsDetails.amlsEvidence.value

  "processDeletions deletes the AMLS evidence together with the application and its individuals" in:
    agentApplicationRepo.upsert(deletionEmailSentWithAmlsEvidence).futureValue
    individualProvidedDetailsRepo.upsert(individualProvidedDetails).futureValue
    ObjectStoreStubs.stubListAmlsEvidence(amlsEvidence)
    ObjectStoreStubs.stubDeleteAmlsEvidence(amlsEvidence)

    agentApplicationDeletionService.processDeletions().futureValue

    ObjectStoreStubs.verifyDeleteAmlsEvidence(amlsEvidence)
    agentApplicationRepo.findById(deletionEmailSentWithAmlsEvidence.agentApplicationId).futureValue shouldBe None
    individualProvidedDetailsRepo.findForApplication(deletionEmailSentWithAmlsEvidence.agentApplicationId).futureValue shouldBe empty

  "processDeletions deletes the application and its individuals when the AMLS evidence is already gone from object-store" in:
    agentApplicationRepo.upsert(deletionEmailSentWithAmlsEvidence).futureValue
    individualProvidedDetailsRepo.upsert(individualProvidedDetails).futureValue
    ObjectStoreStubs.stubListAmlsEvidenceEmpty(amlsEvidence)

    agentApplicationDeletionService.processDeletions().futureValue

    ObjectStoreStubs.verifyDeleteAmlsEvidence(amlsEvidence, count = 0)
    agentApplicationRepo.findById(deletionEmailSentWithAmlsEvidence.agentApplicationId).futureValue shouldBe None
    individualProvidedDetailsRepo.findForApplication(deletionEmailSentWithAmlsEvidence.agentApplicationId).futureValue shouldBe empty

  "processDeletions keeps the application and its individuals when deleting the AMLS evidence fails, so the next run retries" in:
    agentApplicationRepo.upsert(deletionEmailSentWithAmlsEvidence).futureValue
    individualProvidedDetailsRepo.upsert(individualProvidedDetails).futureValue
    ObjectStoreStubs.stubListAmlsEvidence(amlsEvidence)
    ObjectStoreStubs.stubDeleteAmlsEvidence(amlsEvidence, responseStatus = Status.INTERNAL_SERVER_ERROR)

    agentApplicationDeletionService.processDeletions().futureValue

    agentApplicationRepo.findById(deletionEmailSentWithAmlsEvidence.agentApplicationId).futureValue shouldBe Some(deletionEmailSentWithAmlsEvidence)
    individualProvidedDetailsRepo.findForApplication(deletionEmailSentWithAmlsEvidence.agentApplicationId).futureValue shouldBe List(individualProvidedDetails)
