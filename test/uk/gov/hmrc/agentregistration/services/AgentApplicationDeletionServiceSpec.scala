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
import uk.gov.hmrc.agentregistration.shared.BusinessType
import uk.gov.hmrc.agentregistration.shared.amls.AmlsEvidence
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetails
import uk.gov.hmrc.agentregistration.shared.testdata.agentapplication.ExpiryTransformations.*
import uk.gov.hmrc.agentregistration.testsupport.ISpec
import uk.gov.hmrc.agentregistration.testsupport.testdata.AgentApplicationPreconditions.*
import uk.gov.hmrc.agentregistration.testsupport.testdata.AgentApplicationStates
import uk.gov.hmrc.agentregistration.testsupport.wiremock.stubs.ObjectStoreStubs

class AgentApplicationDeletionServiceSpec
extends ISpec:

  private lazy val agentApplicationDeletionService: AgentApplicationDeletionService = app.injector.instanceOf[AgentApplicationDeletionService]
  private lazy val agentApplicationRepo: AgentApplicationRepo = app.injector.instanceOf[AgentApplicationRepo]
  private lazy val individualProvidedDetailsRepo: IndividualProvidedDetailsRepo = app.injector.instanceOf[IndividualProvidedDetailsRepo]

  private given RequestHeader = tdAll.fakeBackendRequest

  private lazy val individualProvidedDetails: IndividualProvidedDetails = tdAll.providedDetails.afterStarted

  BusinessType.values.foreach: businessType =>
    s"for a $businessType application" - {

      lazy val agentApplication: AgentApplicationStates = AgentApplicationStates.forBusinessType(businessType)

      "processDeletions deletes an application whose grace period has ended and that was sent the deletion email, together with its individuals" in:
        val deletionEmailSent: AgentApplication = agentApplication.withVerifiedEmail.makeOverdue.makeExpiredEmailSent.makeDeletionEmailSent
        deletionEmailSent.shouldHaveGracePeriodEndInThePast
        individualProvidedDetails.agentApplicationId shouldBe deletionEmailSent.agentApplicationId withClue
          "precondition: the individual must belong to the application"
        agentApplicationRepo.upsert(deletionEmailSent).futureValue
        individualProvidedDetailsRepo.upsert(individualProvidedDetails).futureValue

        agentApplicationDeletionService.processDeletions().futureValue

        agentApplicationRepo.findById(deletionEmailSent.agentApplicationId).futureValue shouldBe None
        individualProvidedDetailsRepo.findForApplication(deletionEmailSent.agentApplicationId).futureValue shouldBe empty

      "processDeletions deletes an application with no verified email address without waiting for a deletion email, together with its individuals" in:
        val dueForDeletion: AgentApplication = agentApplication.started.makeOverdue.makeExpired.makeDueForDeletion
        dueForDeletion.shouldHaveGracePeriodEndInThePast
        dueForDeletion.applicantContactDetails shouldBe None withClue "precondition: the application must have no applicant contact details"
        dueForDeletion.deletionEmailSentAt shouldBe None withClue "precondition: the deletion email must not be recorded"
        individualProvidedDetails.agentApplicationId shouldBe dueForDeletion.agentApplicationId withClue
          "precondition: the individual must belong to the application"
        agentApplicationRepo.upsert(dueForDeletion).futureValue
        individualProvidedDetailsRepo.upsert(individualProvidedDetails).futureValue

        agentApplicationDeletionService.processDeletions().futureValue

        agentApplicationRepo.findById(dueForDeletion.agentApplicationId).futureValue shouldBe None
        individualProvidedDetailsRepo.findForApplication(dueForDeletion.agentApplicationId).futureValue shouldBe empty

      "processDeletions keeps an application with a verified email address until the deletion email is sent, together with its individuals" in:
        val dueForDeletion: AgentApplication = agentApplication.withVerifiedEmail.makeOverdue.makeExpiredEmailSent.makeDueForDeletion
        dueForDeletion.shouldHaveGracePeriodEndInThePast
        dueForDeletion.deletionEmailSentAt shouldBe None withClue "precondition: the deletion email must not be recorded"
        individualProvidedDetails.agentApplicationId shouldBe dueForDeletion.agentApplicationId withClue
          "precondition: the individual must belong to the application"
        agentApplicationRepo.upsert(dueForDeletion).futureValue
        individualProvidedDetailsRepo.upsert(individualProvidedDetails).futureValue

        agentApplicationDeletionService.processDeletions().futureValue

        agentApplicationRepo.findById(dueForDeletion.agentApplicationId).futureValue shouldBe Some(dueForDeletion)
        individualProvidedDetailsRepo.findForApplication(dueForDeletion.agentApplicationId).futureValue shouldBe List(individualProvidedDetails)

      "processDeletions keeps an application still in the grace period, together with its individuals" in:
        val expired: AgentApplication = agentApplication.started.makeOverdue.makeExpired
        expired.shouldHaveGracePeriodEndInTheFuture
        expired.applicantContactDetails shouldBe None withClue "precondition: no applicant contact details, so only the grace period keeps it"
        individualProvidedDetails.agentApplicationId shouldBe expired.agentApplicationId withClue
          "precondition: the individual must belong to the application"
        agentApplicationRepo.upsert(expired).futureValue
        individualProvidedDetailsRepo.upsert(individualProvidedDetails).futureValue

        agentApplicationDeletionService.processDeletions().futureValue

        agentApplicationRepo.findById(expired.agentApplicationId).futureValue shouldBe Some(expired)
        individualProvidedDetailsRepo.findForApplication(expired.agentApplicationId).futureValue shouldBe List(individualProvidedDetails)
    }

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
