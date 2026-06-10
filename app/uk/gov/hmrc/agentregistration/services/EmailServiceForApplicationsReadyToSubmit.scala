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

import com.softwaremill.quicklens.*
import play.api.mvc.RequestHeader
import uk.gov.hmrc.agentregistration.config.AppConfig
import uk.gov.hmrc.agentregistration.connectors.EmailConnector
import uk.gov.hmrc.agentregistration.model.EmailTemplateId
import uk.gov.hmrc.agentregistration.model.SendEmailRequest
import uk.gov.hmrc.agentregistration.repository.AgentApplicationRepo
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.BusinessType
import uk.gov.hmrc.agentregistration.shared.UserRole
import uk.gov.hmrc.agentregistration.util.EmptyRequest
import uk.gov.hmrc.agentregistration.util.ProcessInSequence
import uk.gov.hmrc.agentregistration.util.RequestAwareLogging

import java.time.format.DateTimeFormatter
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@Singleton
class EmailServiceForApplicationsReadyToSubmit @Inject() (
  emailConnector: EmailConnector,
  agentApplicationRepo: AgentApplicationRepo
)(using ExecutionContext)
extends RequestAwareLogging:

  def processEmails()(using RequestHeader): Future[Unit] =
    for
      applications: Seq[AgentApplication] <- agentApplicationRepo.findReadyForReadyToSubmitEmail()
      applicationsCount: Int = applications.size
      _ = logger.info(s"Found $applicationsCount application(s) ready to be sent the application-ready-to-submit email")
      processedCount <- ProcessInSequence
        .processAllInSequence(applications)(process):
          case (ex, application) =>
            logger.error(s"Failed to process ready-to-submit email for ${application.applicationReference.value}", ex)
      _ = logger.info(s"Processed $processedCount/$applicationsCount application-ready-to-submit emails")
    yield ()

  private def process(application: AgentApplication)(using RequestHeader): Future[Unit] =
    val flagged: AgentApplication = application.modify(_.isApplicationReadyToSubmitEmailSent).setTo(Some(true))
    selectTemplate(application) match
      case Some(templateId) =>
        for
          sendEmailRequest <- Future.successful(makeSendEmailRequest(application, templateId))
          _ = logger.info(s"Sending $templateId email for ${application.applicationReference.value}")
          _ <- emailConnector.sendEmail(sendEmailRequest)
          _ <- agentApplicationRepo.upsert(flagged)
          _ = logger.info(s"Sent $templateId email for ${application.applicationReference.value} and recorded send")
        yield ()
      case None =>
        logger.info(s"Suppressing application-ready-to-submit email for ${application.applicationReference.value} (sole trader + applicant is business owner)")
        agentApplicationRepo.upsert(flagged)

  private def selectTemplate(application: AgentApplication): Option[EmailTemplateId] = application.businessType match
    case BusinessType.SoleTrader if application.userRole.contains(UserRole.Owner) => None
    case BusinessType.SoleTrader => Some(EmailTemplateId.ApplicationReadyToSubmitSoleTraderNotBusinessOwner)
    case _ => Some(EmailTemplateId.ApplicationReadyToSubmit)

  private def makeSendEmailRequest(
    application: AgentApplication,
    templateId: EmailTemplateId
  ): SendEmailRequest = SendEmailRequest(
    to = Seq(application.getApplicantContactDetails.getVerifiedEmail),
    templateId = templateId,
    parameters = Map(
      "agentName" -> application.getApplicantContactDetails.applicantName.value,
      "applicationRef" -> application.applicationReference.value,
      "applicationExpiryDate" -> formatApplicationExpiryDate(application)
    )
  )

  private def formatApplicationExpiryDate(application: AgentApplication): String = application
    .applicationExpiresAt
    .map(instant => LocalDate.ofInstant(instant, AppConfig.zoneId).format(EmailServiceForApplicationsReadyToSubmit.dateFormatter))
    .getOrElse("")

object EmailServiceForApplicationsReadyToSubmit:
  private val dateFormatter: DateTimeFormatter = DateTimeFormatter
    .ofPattern("d MMMM yyyy")
    .withLocale(java.util.Locale.UK)
