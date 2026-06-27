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
import uk.gov.hmrc.agentregistration.model.EmailStatus
import uk.gov.hmrc.agentregistration.model.EmailTemplateId
import uk.gov.hmrc.agentregistration.model.SendEmailRequest
import uk.gov.hmrc.agentregistration.repository.AgentApplicationRepo
import uk.gov.hmrc.agentregistration.repository.ApplicationSchedulerRepo
import uk.gov.hmrc.agentregistration.repository.providedetails.llp.IndividualProvidedDetailsRepo
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.testsupport.ISpec
import uk.gov.hmrc.agentregistration.testsupport.testdata.TdAgentApplicationLlpInStates
import uk.gov.hmrc.agentregistration.testsupport.testdata.TdAgentApplicationSoleTraderInStates
import uk.gov.hmrc.agentregistration.testsupport.testdata.TdScenario
import uk.gov.hmrc.agentregistration.testsupport.wiremock.stubs.EmailStubs
import uk.gov.hmrc.agentregistration.util.EmptyRequest

import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class EmailServiceForApplicationsReadyToSubmitSpec
extends ISpec:

  private given RequestHeader = EmptyRequest.emptyRequestHeader

  private lazy val emailServiceForApplicationsReadyToSubmit: EmailServiceForApplicationsReadyToSubmit = app.injector.instanceOf[
    EmailServiceForApplicationsReadyToSubmit
  ]
  private lazy val agentApplicationRepo: AgentApplicationRepo = app.injector.instanceOf[AgentApplicationRepo]
  private lazy val applicationSchedulerRepo: ApplicationSchedulerRepo = app.injector.instanceOf[ApplicationSchedulerRepo]
  private lazy val individualProvidedDetailsRepo: IndividualProvidedDetailsRepo = app.injector.instanceOf[IndividualProvidedDetailsRepo]

  private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy").withLocale(java.util.Locale.UK)

  private def expectedEmail(
    agentApplication: AgentApplication,
    templateId: EmailTemplateId
  ): SendEmailRequest = SendEmailRequest(
    to = Seq(agentApplication.getApplicantContactDetails.getVerifiedEmail),
    templateId = templateId,
    parameters = Map(
      "agentName" -> agentApplication.getApplicantContactDetails.applicantName.value,
      "applicationRef" -> agentApplication.applicationReference.value,
      "applicationExpiryDate" -> LocalDate.ofInstant(agentApplication.applicationExpiresAt.value, ZoneId.of("Europe/London")).format(dateFormatter)
    )
  )

  private case class TestCase(
    description: String,
    scenario: TdScenario,
    expectedEmails: Seq[SendEmailRequest],
    expectedEmailStatusAfter: Option[EmailStatus]
  )

  "processEmails" - {

    List(
      TestCase(
        description = "sends the default template + records Sent when all individuals are Finished (LLP)",
        scenario = TdAgentApplicationLlpInStates.readyForEmail,
        expectedEmails = Seq(expectedEmail(TdAgentApplicationLlpInStates.readyForEmail.agentApplication, EmailTemplateId.ApplicationReadyToSubmit)),
        expectedEmailStatusAfter = Some(EmailStatus.Sent)
      ),
      TestCase(
        description = "sends the sole-trader-not-owner template + records Sent for sole trader where applicant is NOT the business owner",
        scenario = TdAgentApplicationSoleTraderInStates.readyForEmailAsNonOwner,
        expectedEmails = Seq(
          expectedEmail(
            TdAgentApplicationSoleTraderInStates.readyForEmailAsNonOwner.agentApplication,
            EmailTemplateId.ApplicationReadyToSubmitSoleTraderNotBusinessOwner
          )
        ),
        expectedEmailStatusAfter = Some(EmailStatus.Sent)
      ),
      TestCase(
        description = "records Suppressed and does not send email for sole trader who IS the business owner",
        scenario = TdAgentApplicationSoleTraderInStates.readyForEmailAsOwner,
        expectedEmails = Seq.empty,
        expectedEmailStatusAfter = Some(EmailStatus.Suppressed)
      ),
      TestCase(
        description = "skips applications already recorded as Sent",
        scenario = TdAgentApplicationLlpInStates.emailAlreadySent,
        expectedEmails = Seq.empty,
        expectedEmailStatusAfter = Some(EmailStatus.Sent)
      ),
      TestCase(
        description = "skips applications already recorded as Suppressed",
        scenario = TdAgentApplicationLlpInStates.emailAlreadySuppressed,
        expectedEmails = Seq.empty,
        expectedEmailStatusAfter = Some(EmailStatus.Suppressed)
      ),
      TestCase(
        description = "skips applications where not every individual is Finished",
        scenario = TdAgentApplicationLlpInStates.oneIndividualNotFinished,
        expectedEmails = Seq.empty,
        expectedEmailStatusAfter = None
      ),
      TestCase(
        description = "skips applications not in GrsDataReceived state",
        scenario = TdAgentApplicationLlpInStates.notYetGrs,
        expectedEmails = Seq.empty,
        expectedEmailStatusAfter = None
      ),
      TestCase(
        description = "skips applications already sent for risking",
        scenario = TdAgentApplicationLlpInStates.alreadySentForRisking,
        expectedEmails = Seq.empty,
        expectedEmailStatusAfter = None
      ),
      TestCase(
        description = "skips applications with no linked individuals",
        scenario = TdAgentApplicationLlpInStates.noIndividuals,
        expectedEmails = Seq.empty,
        expectedEmailStatusAfter = None
      )
    ).foreach: testCase =>
      testCase.description in:
        testCase.expectedEmails.foreach(EmailStubs.stubSendEmail(_))
        agentApplicationRepo.upsert(testCase.scenario.agentApplication).futureValue
        testCase.scenario.individuals.foreach(individualProvidedDetailsRepo.upsert(_).futureValue)
        testCase.scenario.applicationScheduler.foreach(applicationSchedulerRepo.upsert(_).futureValue)

        emailServiceForApplicationsReadyToSubmit.processEmails().futureValue

        EmailStubs.verifySendEmail(count = testCase.expectedEmails.size)
        applicationSchedulerRepo
          .findById(testCase.scenario.agentApplication.applicationReference)
          .futureValue
          .map(_.applicationReadyToSubmitEmailStatus) shouldBe testCase.expectedEmailStatusAfter
  }
