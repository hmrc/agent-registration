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
import uk.gov.hmrc.agentregistration.connectors.EmailConnector
import uk.gov.hmrc.agentregistration.model.ApplicationScheduler
import uk.gov.hmrc.agentregistration.model.EmailStatus
import uk.gov.hmrc.agentregistration.model.EmailTemplateId
import uk.gov.hmrc.agentregistration.model.SendEmailRequest
import uk.gov.hmrc.agentregistration.repository.AgentApplicationRepo
import uk.gov.hmrc.agentregistration.repository.ApplicationSchedulerRepo
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.ApplicationReference
import uk.gov.hmrc.agentregistration.shared.UserRole
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.===
import uk.gov.hmrc.agentregistration.util.ProcessInSequence
import uk.gov.hmrc.agentregistration.util.RequestAwareLogging

import java.time.format.DateTimeFormatter
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@Singleton
class EmailServiceForApplicationsReadyToSubmit @Inject() (
  emailConnector: EmailConnector,
  agentApplicationRepo: AgentApplicationRepo,
  applicationSchedulerRepo: ApplicationSchedulerRepo
)(using ExecutionContext)
extends RequestAwareLogging:

  def processEmails()(using RequestHeader): Future[Unit] =
    for
      agentApplications: Seq[AgentApplication] <- agentApplicationRepo.findReadyForReadyToSubmitEmail()
      agentApplicationsCount: Int = agentApplications.size
      _ = logger.info(s"Found $agentApplicationsCount application(s) ready to be sent the application-ready-to-submit email")
      processedCount <-
        ProcessInSequence
          .processAllInSequence(agentApplications)(process):
            case (ex, agentApplication) => logger.error(s"Failed to process ready-to-submit email for ${agentApplication.applicationReference.value}", ex)
      _ = logger.info(s"Processed $processedCount/$agentApplicationsCount application-ready-to-submit emails")
    yield ()

  private def process(agentApplication: AgentApplication)(using RequestHeader): Future[Unit] =
    selectTemplate(agentApplication) match
      case Some(templateId) =>
        val sendEmailRequest = makeSendEmailRequest(agentApplication, templateId)
        logger.info(s"Sending $templateId email for ${agentApplication.applicationReference.value}")
        for
          _ <- emailConnector.sendEmail(sendEmailRequest)
          _ <- recordReadyToSubmitEmailStatus(agentApplication.applicationReference, EmailStatus.Sent)
        yield logger.info(s"Sent $templateId email for ${agentApplication.applicationReference.value}")
      case None =>
        logger.info(s"Skipping email for ${agentApplication.applicationReference.value} (sole trader who is the business owner)")
        recordReadyToSubmitEmailStatus(agentApplication.applicationReference, EmailStatus.Suppressed)

  private def recordReadyToSubmitEmailStatus(
    applicationReference: ApplicationReference,
    emailStatus: EmailStatus
  ): Future[Unit] = applicationSchedulerRepo
    .findById(applicationReference)
    .flatMap: maybeApplicationScheduler =>
      applicationSchedulerRepo.upsert(
        maybeApplicationScheduler
          .getOrElse(ApplicationScheduler.makeNew(applicationReference))
          .copy(
            applicationReadyToSubmitEmailStatus = emailStatus,
            lastUpdated = Instant.now(Clock.systemUTC())
          )
      )

  private def selectTemplate(agentApplication: AgentApplication): Option[EmailTemplateId] =
    agentApplication match
      case _: AgentApplication.IsSoleTrader if agentApplication.getUserRole === UserRole.Owner => None
      case _: AgentApplication.IsSoleTrader => Some(EmailTemplateId.ApplicationReadyToSubmitSoleTraderNotBusinessOwner)
      case _: AgentApplication.IsNotSoleTrader => Some(EmailTemplateId.ApplicationReadyToSubmit)

  private def makeSendEmailRequest(
    agentApplication: AgentApplication,
    templateId: EmailTemplateId
  ): SendEmailRequest = SendEmailRequest(
    to = Seq(agentApplication.getApplicantContactDetails.getVerifiedEmail),
    templateId = templateId,
    parameters = Map(
      "agentName" -> agentApplication.getApplicantContactDetails.applicantName.value,
      "applicationRef" -> agentApplication.applicationReference.value,
      "applicationExpiryDate" -> LocalDate
        .ofInstant(agentApplication.getApplicationExpiresAt, EmailServiceForApplicationsReadyToSubmit.displayZone)
        .format(EmailServiceForApplicationsReadyToSubmit.dateFormatter)
    )
  )

object EmailServiceForApplicationsReadyToSubmit:

  // dates shown to the applicant in the email body — UK-local zone, not UTC, to avoid late-evening-BST rollover surprises
  private val displayZone: ZoneId = ZoneId.of("Europe/London")
  private val dateFormatter: DateTimeFormatter = DateTimeFormatter
    .ofPattern("d MMMM yyyy")
    .withLocale(java.util.Locale.UK)
