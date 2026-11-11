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

import uk.gov.hmrc.agentregistration.repository.AgentApplicationRepo
import uk.gov.hmrc.agentregistration.repository.providedetails.llp.IndividualProvidedDetailsRepo
import play.api.http.Status
import play.api.mvc.RequestHeader
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.amls.AmlsEvidence
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetails
import uk.gov.hmrc.agentregistration.testsupport.ISpec
import uk.gov.hmrc.agentregistration.testsupport.wiremock.stubs.ObjectStoreStubs

class AgentApplicationDeletionServiceSpec
extends ISpec:

  private lazy val agentApplicationDeletionService: AgentApplicationDeletionService = app.injector.instanceOf[AgentApplicationDeletionService]
  private lazy val agentApplicationRepo: AgentApplicationRepo = app.injector.instanceOf[AgentApplicationRepo]
  private lazy val individualProvidedDetailsRepo: IndividualProvidedDetailsRepo = app.injector.instanceOf[IndividualProvidedDetailsRepo]

  private given RequestHeader = tdAll.fakeBackendRequest

  "processDeletions deletes an Expired application whose postExpiryGracePeriodEndsAt has passed, together with its individuals, without waiting for a deletion email when there is no verified email address" in:
    val agentApplication: AgentApplication = tdAll.agentApplicationLlp.afterExpiredPostExpiryGracePeriodEnded
    val individualProvidedDetails: IndividualProvidedDetails = tdAll.providedDetails.afterStarted
    agentApplication.postExpiryGracePeriodEndsAt.value.isBefore(tdAll.nowAsInstant) shouldBe true withClue
      "precondition: postExpiryGracePeriodEndsAt must be in the past"
    individualProvidedDetails.agentApplicationId shouldBe agentApplication.agentApplicationId withClue
      "precondition: the individual must belong to the application"
    agentApplicationRepo.upsert(agentApplication).futureValue
    individualProvidedDetailsRepo.upsert(individualProvidedDetails).futureValue

    agentApplicationDeletionService.processDeletions().futureValue

    agentApplicationRepo.findById(agentApplication.agentApplicationId).futureValue shouldBe None
    individualProvidedDetailsRepo.findForApplication(agentApplication.agentApplicationId).futureValue shouldBe empty

  "processDeletions keeps an Expired application whose postExpiryGracePeriodEndsAt is still in the future, together with its individuals" in:
    val agentApplication: AgentApplication = tdAll.agentApplicationLlp.afterExpired
    val individualProvidedDetails: IndividualProvidedDetails = tdAll.providedDetails.afterStarted
    agentApplication.postExpiryGracePeriodEndsAt.value.isAfter(tdAll.nowAsInstant) shouldBe true withClue
      "precondition: postExpiryGracePeriodEndsAt must be in the future"
    individualProvidedDetails.agentApplicationId shouldBe agentApplication.agentApplicationId withClue
      "precondition: the individual must belong to the application"
    agentApplicationRepo.upsert(agentApplication).futureValue
    individualProvidedDetailsRepo.upsert(individualProvidedDetails).futureValue

    agentApplicationDeletionService.processDeletions().futureValue

    agentApplicationRepo.findById(agentApplication.agentApplicationId).futureValue shouldBe Some(agentApplication)
    individualProvidedDetailsRepo.findForApplication(agentApplication.agentApplicationId).futureValue shouldBe List(individualProvidedDetails)

  "processDeletions deletes the AMLS evidence of an Expired application whose postExpiryGracePeriodEndsAt has passed, together with the application and its individuals" in:
    val agentApplication: AgentApplication = tdAll.agentApplicationLlp.afterExpiredWithAmlsEvidencePostExpiryGracePeriodEnded
    val individualProvidedDetails: IndividualProvidedDetails = tdAll.providedDetails.afterStarted
    val amlsEvidence: AmlsEvidence = agentApplication.getAmlsDetails.amlsEvidence.value
    agentApplicationRepo.upsert(agentApplication).futureValue
    individualProvidedDetailsRepo.upsert(individualProvidedDetails).futureValue
    ObjectStoreStubs.stubListAmlsEvidence(amlsEvidence)
    ObjectStoreStubs.stubDeleteAmlsEvidence(amlsEvidence)

    agentApplicationDeletionService.processDeletions().futureValue

    ObjectStoreStubs.verifyDeleteAmlsEvidence(amlsEvidence)
    agentApplicationRepo.findById(agentApplication.agentApplicationId).futureValue shouldBe None
    individualProvidedDetailsRepo.findForApplication(agentApplication.agentApplicationId).futureValue shouldBe empty

  "processDeletions deletes an Expired application whose postExpiryGracePeriodEndsAt has passed, together with its individuals, when its AMLS evidence is already gone from object-store" in:
    val agentApplication: AgentApplication = tdAll.agentApplicationLlp.afterExpiredWithAmlsEvidencePostExpiryGracePeriodEnded
    val individualProvidedDetails: IndividualProvidedDetails = tdAll.providedDetails.afterStarted
    val amlsEvidence: AmlsEvidence = agentApplication.getAmlsDetails.amlsEvidence.value
    agentApplicationRepo.upsert(agentApplication).futureValue
    individualProvidedDetailsRepo.upsert(individualProvidedDetails).futureValue
    ObjectStoreStubs.stubListAmlsEvidenceEmpty(amlsEvidence)

    agentApplicationDeletionService.processDeletions().futureValue

    ObjectStoreStubs.verifyDeleteAmlsEvidence(amlsEvidence, count = 0)
    agentApplicationRepo.findById(agentApplication.agentApplicationId).futureValue shouldBe None
    individualProvidedDetailsRepo.findForApplication(agentApplication.agentApplicationId).futureValue shouldBe empty

  "processDeletions keeps an Expired application whose postExpiryGracePeriodEndsAt has passed, together with its individuals, when deleting its AMLS evidence fails, so the next run retries" in:
    val agentApplication: AgentApplication = tdAll.agentApplicationLlp.afterExpiredWithAmlsEvidencePostExpiryGracePeriodEnded
    val individualProvidedDetails: IndividualProvidedDetails = tdAll.providedDetails.afterStarted
    val amlsEvidence: AmlsEvidence = agentApplication.getAmlsDetails.amlsEvidence.value
    agentApplicationRepo.upsert(agentApplication).futureValue
    individualProvidedDetailsRepo.upsert(individualProvidedDetails).futureValue
    ObjectStoreStubs.stubListAmlsEvidence(amlsEvidence)
    ObjectStoreStubs.stubDeleteAmlsEvidence(amlsEvidence, responseStatus = Status.INTERNAL_SERVER_ERROR)

    agentApplicationDeletionService.processDeletions().futureValue

    agentApplicationRepo.findById(agentApplication.agentApplicationId).futureValue shouldBe Some(agentApplication)
    individualProvidedDetailsRepo.findForApplication(agentApplication.agentApplicationId).futureValue shouldBe List(individualProvidedDetails)

  "processDeletions deletes an Expired application past postExpiryGracePeriodEndsAt once its deletion email has been sent" in:
    val agentApplication: AgentApplication = tdAll.agentApplicationLlp.afterContactDetailsCompleteExpiredPostExpiryGracePeriodEndedDeletionEmailSent
    agentApplicationRepo.upsert(agentApplication).futureValue

    agentApplicationDeletionService.processDeletions().futureValue

    agentApplicationRepo.findById(agentApplication.agentApplicationId).futureValue shouldBe None

  "processDeletions keeps an Expired application past postExpiryGracePeriodEndsAt that has a verified email but no deletion email yet, so the email is sent first" in:
    val agentApplication: AgentApplication = tdAll.agentApplicationLlp.afterContactDetailsCompleteExpiredPostExpiryGracePeriodEnded
    agentApplicationRepo.upsert(agentApplication).futureValue

    agentApplicationDeletionService.processDeletions().futureValue

    agentApplicationRepo.findById(agentApplication.agentApplicationId).futureValue shouldBe Some(agentApplication)
