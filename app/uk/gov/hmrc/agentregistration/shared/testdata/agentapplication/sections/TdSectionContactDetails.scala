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

package uk.gov.hmrc.agentregistration.shared.testdata.agentapplication.sections

import com.softwaremill.quicklens.*
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.AgentApplicationLlp
import uk.gov.hmrc.agentregistration.shared.contactdetails.*
import uk.gov.hmrc.agentregistration.shared.testdata.TdBase

trait TdSectionContactDetails {
  dependencies: TdBase =>

  class TdAgentApplicationWithSectionContactDetails(baseForSectionContactDetails: AgentApplication):

    object sectionContactDetails:

      private object ApplicantNameHelper:

        val afterNameDeclared: ApplicantName = ApplicantName("Miss Alexa Fantastic")

      val afterNameDeclared: AgentApplication = baseForSectionContactDetails
        .modify(_.applicantContactDetails)
        .setTo(Some(ApplicantContactDetails(
          applicantName = ApplicantNameHelper.afterNameDeclared
        )))

      val afterTelephoneNumberProvided: AgentApplication = afterNameDeclared
        .modify(_.applicantContactDetails.each.telephoneNumber)
        .setTo(Some(dependencies.telephoneNumber))

      val afterEmailAddressProvided: AgentApplication = afterTelephoneNumberProvided
        .modify(_.applicantContactDetails.each.applicantEmailAddress)
        .setTo(Some(ApplicantEmailAddressHelper.afterEmailAddressProvided))

      val afterEmailAddressVerified: AgentApplication = afterEmailAddressProvided
        .modify(_.applicantContactDetails.each.applicantEmailAddress)
        .setTo(Some(ApplicantEmailAddressHelper.afterEmailAddressVerified))

  private object ApplicantEmailAddressHelper:

    val afterEmailAddressProvided: ApplicantEmailAddress = ApplicantEmailAddress(
      emailAddress = dependencies.applicantEmailAddress,
      isVerified = false
    )
    val afterEmailAddressVerified: ApplicantEmailAddress = afterEmailAddressProvided.copy(isVerified = true)

}
