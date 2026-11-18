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

import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.EmailAddress
import uk.gov.hmrc.agentregistration.shared.contactdetails.ApplicantContactDetails
import uk.gov.hmrc.agentregistration.shared.contactdetails.ApplicantName

final case class EmailRecipient(
  applicantName: ApplicantName,
  emailAddress: EmailAddress
)

object EmailRecipient:

  def partition(agentApplications: Seq[AgentApplication]): (Seq[AgentApplication], Seq[AgentApplicationWithRecipient]) = agentApplications.partitionMap:
    agentApplication =>
      resolve(agentApplication).toRight(agentApplication).map(emailRecipient => AgentApplicationWithRecipient(agentApplication, emailRecipient))

  def resolve(agentApplication: AgentApplication): Option[EmailRecipient] = agentApplication.applicantContactDetails.flatMap: applicantContactDetails =>
    verifiedEmailAddress(agentApplication, applicantContactDetails)
      .map(emailAddress => EmailRecipient(applicantContactDetails.applicantName, emailAddress))

  private def verifiedEmailAddress(
    agentApplication: AgentApplication,
    applicantContactDetails: ApplicantContactDetails
  ): Option[EmailAddress] = agentApplication.agentDetails
    .flatMap(_.agentEmailAddress)
    .filter(_.isVerified)
    .map(_.getEmailAddress)
    .orElse(applicantContactDetails.applicantEmailAddress.flatMap(_.verifiedEmailAddress))
