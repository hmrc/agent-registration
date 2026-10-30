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

package uk.gov.hmrc.agentregistration.services.email

import com.softwaremill.quicklens.modify
import play.api.mvc.RequestHeader
import uk.gov.hmrc.agentregistration.config.AppConfig
import uk.gov.hmrc.agentregistration.connectors.email.EmailConnector
import uk.gov.hmrc.agentregistration.connectors.email.EmailTemplateId
import uk.gov.hmrc.agentregistration.connectors.email.SendEmailRequest
import uk.gov.hmrc.agentregistration.repository.AgentApplicationRepo
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.util.Errors.getOrThrowExpectedDataMissing
import uk.gov.hmrc.agentregistration.util.ProcessInSequence
import uk.gov.hmrc.agentregistration.util.RequestAwareLogging

import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.jdk.DurationConverters.*

@Singleton
class EmailServiceForExpiryReminders @Inject() (
  emailConnector: EmailConnector,
  agentApplicationRepo: AgentApplicationRepo,
  appConfig: AppConfig,
  clock: Clock
)(using ExecutionContext)
extends RequestAwareLogging:

  private val emailTemplateId: EmailTemplateId = EmailTemplateId.ApplicationExpiryReminder

  def processEmails()(using RequestHeader): Future[Unit] =
    val now: Instant = Instant.now(clock)
    val expiryReminderPeriodEndsAt: Instant = now.plus(appConfig.expiryReminderPeriod.toJava)
    agentApplicationRepo
      .findReadyForExpiryReminderEmail(now, expiryReminderPeriodEndsAt)
      .flatMap: agentApplications =>
        val (withoutRecipient: Seq[AgentApplication], withRecipient: Seq[AgentApplicationWithRecipient]) = EmailRecipient.partition(agentApplications)
        logger.info(
          s"Found ${agentApplications.size} unsubmitted applications due for the $emailTemplateId email, ${withoutRecipient.size} without a verified email address skipped"
        )
        ProcessInSequence
          .processAllInSequence(withRecipient)(process):
            case (throwable, agentApplicationWithRecipient) =>
              logger.error(s"Failed to send $emailTemplateId email for ${agentApplicationWithRecipient.agentApplication.applicationReference.value}", throwable)
          .map: emailsSentSuccessCount =>
            logger.info(s"Sent $emailsSentSuccessCount/${withRecipient.size} $emailTemplateId emails")

  private def process(agentApplicationWithRecipient: AgentApplicationWithRecipient)(using RequestHeader): Future[Unit] =
    val agentApplication: AgentApplication = agentApplicationWithRecipient.agentApplication
    val sendEmailRequest: SendEmailRequest = makeSendEmailRequest(agentApplication, agentApplicationWithRecipient.emailRecipient)
    logger.info(s"Sending $emailTemplateId email for ${agentApplication.applicationReference.value}")
    for
      _ <- emailConnector.sendEmail(sendEmailRequest)
      _ <- markEmailSent(agentApplication)
      _ = logger.info(s"Sent $emailTemplateId email for ${agentApplication.applicationReference.value}")
    yield ()

  private def markEmailSent(agentApplication: AgentApplication)(using RequestHeader): Future[Unit] = agentApplicationRepo
    .findById(agentApplication.agentApplicationId)
    .flatMap:
      case Some(latestAgentApplication) =>
        agentApplicationRepo.upsert(latestAgentApplication.modify(_.expiryReminderEmailSentAt).setTo(Some(Instant.now(clock))))
      case None =>
        logger.warn(s"Application ${agentApplication.applicationReference.value} no longer exists, expiryReminderEmailSentAt not recorded")
        Future.successful(())

  def makeSendEmailRequest(
    agentApplication: AgentApplication,
    emailRecipient: EmailRecipient
  ): SendEmailRequest =
    val applicationExpiresAt: Instant = agentApplication.applicationExpiresAt.getOrThrowExpectedDataMissing("applicationExpiresAt")
    SendEmailRequest(
      to = Seq(emailRecipient.emailAddress),
      templateId = emailTemplateId,
      parameters = Map(
        "agentName" -> emailRecipient.applicantName.value,
        "applicationRef" -> agentApplication.applicationReference.value,
        "expiryDate" -> EmailDateFormatter.format(applicationExpiresAt),
        "deletionDate" -> EmailDateFormatter.format(applicationExpiresAt.plus(appConfig.postExpiryGracePeriod.toJava))
      )
    )
