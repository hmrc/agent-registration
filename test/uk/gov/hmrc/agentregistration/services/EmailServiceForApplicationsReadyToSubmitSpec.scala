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

import play.api.mvc.RequestHeader
import uk.gov.hmrc.agentregistration.config.AppConfig
import uk.gov.hmrc.agentregistration.model.EmailTemplateId
import uk.gov.hmrc.agentregistration.model.SendEmailRequest
import uk.gov.hmrc.agentregistration.repository.AgentApplicationRepo
import uk.gov.hmrc.agentregistration.repository.providedetails.llp.IndividualProvidedDetailsRepo
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.PersonReference
import uk.gov.hmrc.agentregistration.shared.UserRole
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetails
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetailsId
import uk.gov.hmrc.agentregistration.shared.individual.ProvidedDetailsState
import uk.gov.hmrc.agentregistration.testsupport.ISpec
import uk.gov.hmrc.agentregistration.testsupport.wiremock.stubs.EmailStubs
import uk.gov.hmrc.agentregistration.util.EmptyRequest

import java.time.LocalDate
import java.time.format.DateTimeFormatter

class EmailServiceForApplicationsReadyToSubmitSpec
extends ISpec:

  private given RequestHeader = EmptyRequest.emptyRequestHeader

  private lazy val service: EmailServiceForApplicationsReadyToSubmit = app.injector.instanceOf[EmailServiceForApplicationsReadyToSubmit]
  private lazy val applicationRepo: AgentApplicationRepo = app.injector.instanceOf[AgentApplicationRepo]
  private lazy val individualRepo: IndividualProvidedDetailsRepo = app.injector.instanceOf[IndividualProvidedDetailsRepo]
  
  private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy").withLocale(java.util.Locale.UK)

  private def expectedEmail(
    application: AgentApplication,
    templateId: EmailTemplateId
  ): SendEmailRequest = SendEmailRequest(
    to = Seq(application.getApplicantContactDetails.getVerifiedEmail),
    templateId = templateId,
    parameters = Map(
      "agentName" -> application.getApplicantContactDetails.applicantName.value,
      "applicationRef" -> application.applicationReference.value,
      "applicationExpiryDate" -> application
        .applicationExpiresAt
        .map(LocalDate.ofInstant(_, AppConfig.zoneId).format(dateFormatter))
        .getOrElse(fail("fixture must have applicationExpiresAt set"))
    )
  )
  
  private def finishedIndividualFor(suffix: String): IndividualProvidedDetails =
    val base = tdAll.providedDetails.precreated
    base.copy(
      _id = IndividualProvidedDetailsId(s"${base._id.value}-$suffix"),
      personReference = PersonReference(s"${base.personReference.value}-$suffix"),
      providedDetailsState = ProvidedDetailsState.Finished
    )

  private def startedIndividualFor(suffix: String): IndividualProvidedDetails =
    finishedIndividualFor(suffix).copy(providedDetailsState = ProvidedDetailsState.Started)

  private val llpReady = tdAll.agentApplicationLlp.afterContactDetailsComplete
  private val llpAlreadyFlagged = llpReady.copy(isApplicationReadyToSubmitEmailSent = Some(true))
  private val llpNotYetGrs = tdAll.agentApplicationLlp.afterStarted
  private val soleTraderOwner = tdAll.agentApplicationSoleTrader.afterContactDetailsComplete // default userRole = Owner
  private val soleTraderNonOwner = soleTraderOwner.copy(userRole = Some(UserRole.Authorised))

  private case class TestCase(
    description: String,
    application: AgentApplication,
    individuals: Seq[IndividualProvidedDetails],
    expectedEmails: Seq[SendEmailRequest],
    expectedFlagAfter: Option[Boolean]
  )

  "processEmails" - {

    List(
      TestCase(
        description = "sends the default template + flips flag when all individuals are Finished (LLP)",
        application = llpReady,
        individuals = Seq(finishedIndividualFor("a"), finishedIndividualFor("b")),
        expectedEmails = Seq(expectedEmail(llpReady, EmailTemplateId.ApplicationReadyToSubmit)),
        expectedFlagAfter = Some(true)
      ),
      TestCase(
        description = "sends the sole-trader-not-owner template for sole trader where the applicant is NOT the business owner",
        application = soleTraderNonOwner,
        individuals = Seq(finishedIndividualFor("a")),
        expectedEmails = Seq(expectedEmail(soleTraderNonOwner, EmailTemplateId.ApplicationReadyToSubmitSoleTraderNotBusinessOwner)),
        expectedFlagAfter = Some(true)
      ),
      TestCase(
        description = "suppresses the email but still flips the flag for sole trader where the applicant IS the business owner",
        application = soleTraderOwner,
        individuals = Seq(finishedIndividualFor("a")),
        expectedEmails = Seq.empty,
        expectedFlagAfter = Some(true)
      ),
      TestCase(
        description = "skips applications where isApplicationReadyToSubmitEmailSent is already true",
        application = llpAlreadyFlagged,
        individuals = Seq(finishedIndividualFor("a")),
        expectedEmails = Seq.empty,
        expectedFlagAfter = Some(true)
      ),
      TestCase(
        description = "skips applications where not every individual is Finished",
        application = llpReady,
        individuals = Seq(finishedIndividualFor("a"), startedIndividualFor("b")),
        expectedEmails = Seq.empty,
        expectedFlagAfter = None
      ),
      TestCase(
        description = "skips applications not in GrsDataReceived state",
        application = llpNotYetGrs,
        individuals = Seq(finishedIndividualFor("a")),
        expectedEmails = Seq.empty,
        expectedFlagAfter = None
      ),
      TestCase(
        description = "skips applications with no linked individuals",
        application = llpReady,
        individuals = Seq.empty,
        expectedEmails = Seq.empty,
        expectedFlagAfter = None
      )
    ).foreach: tc =>
      tc.description in:
        tc.expectedEmails.foreach(EmailStubs.stubSendEmail(_))
        applicationRepo.upsert(tc.application).futureValue
        tc.individuals.foreach(individualRepo.upsert(_).futureValue)

        service.processEmails().futureValue

        EmailStubs.verifySendEmail(count = tc.expectedEmails.size)
        applicationRepo.findById(tc.application.agentApplicationId).futureValue.value.isApplicationReadyToSubmitEmailSent shouldBe tc.expectedFlagAfter
  }
