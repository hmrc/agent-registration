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
import uk.gov.hmrc.agentregistration.shared.agentdetails.*
import uk.gov.hmrc.agentregistration.shared.testdata.TdBase
import uk.gov.hmrc.agentregistration.shared.util.Errors.*

trait TdSectionAgentDetails {
  dependencies: TdBase =>

  class TdAgentApplicationWithSectionAgentDetails(baseForSectionAgentDetails: AgentApplication):

    object sectionAgentDetails:

      object whenUsingExistingCompanyName:

        val afterBusinessNameProvided: AgentApplication = baseForSectionAgentDetails
          .modify(_.agentDetails)
          .setTo(Some(AgentDetails(
            businessName = AgentBusinessName(
              agentBusinessName = dependencies.companyName,
              otherAgentBusinessName = None
            )
          )))
        val afterBprTelephoneNumberSelected: AgentApplication = afterBusinessNameProvided
          .modify(_.agentDetails.each.telephoneNumber)
          .setTo(Some(AgentTelephoneNumberHelper.afterBprTelephoneNumberSelected))
        val afterContactTelephoneSelected: AgentApplication = afterBusinessNameProvided
          .modify(_.agentDetails.each.telephoneNumber)
          .setTo(Some(AgentTelephoneNumberHelper.afterContactTelephoneSelected))
        val afterOtherTelephoneNumberProvided: AgentApplication = afterBusinessNameProvided
          .modify(_.agentDetails.each.telephoneNumber)
          .setTo(Some(AgentTelephoneNumberHelper.afterOtherTelephoneNumberProvided))
        val afterContactEmailAddressSelected: AgentApplication = afterContactTelephoneSelected
          .modify(_.agentDetails.each.agentEmailAddress)
          .setTo(Some(AgentEmailAddressHelper.afterContactEmailAddressSelected))
        val afterBprEmailAddressSelected: AgentApplication = afterContactTelephoneSelected
          .modify(_.agentDetails.each.agentEmailAddress)
          .setTo(Some(AgentEmailAddressHelper.afterBprEmailAddressSelected))
        val afterOtherEmailAddressSelected: AgentApplication = afterContactTelephoneSelected
          .modify(_.agentDetails.each.agentEmailAddress)
          .setTo(Some(AgentEmailAddressHelper.afterOtherEmailAddressSelected))
        val afterVerifiedEmailAddressSelected: AgentApplication = afterContactTelephoneSelected
          .modify(_.agentDetails.each.agentEmailAddress)
          .setTo(Some(AgentEmailAddressHelper.afterVerifiedEmailAddressSelected))
        val afterChroAddressSelected: AgentApplication = afterVerifiedEmailAddressSelected
          .modify(_.agentDetails.each.agentCorrespondenceAddress)
          .setTo(Some(dependencies.chroAddress))
        val afterBprAddressSelected: AgentApplication = afterVerifiedEmailAddressSelected
          .modify(_.agentDetails.each.agentCorrespondenceAddress)
          .setTo(Some(
            AgentCorrespondenceAddress(
              addressLine1 = dependencies.bprRegisteredAddress.addressLine1,
              addressLine2 = dependencies.bprRegisteredAddress.addressLine2,
              addressLine3 = dependencies.bprRegisteredAddress.addressLine3,
              addressLine4 = dependencies.bprRegisteredAddress.addressLine4,
              postalCode = dependencies.bprRegisteredAddress.postalCode,
              countryCode = dependencies.bprRegisteredAddress.countryCode
            )
          ))
        val afterOtherAddressProvided: AgentApplication = afterVerifiedEmailAddressSelected
          .modify(_.agentDetails.each.agentCorrespondenceAddress)
          .setTo(Some(
            AgentCorrespondenceAddress(
              addressLine1 = dependencies.getConfirmedAddressResponse.lines.headOption.getOrThrowExpectedDataMissing("getConfirmedAddressResponse.line.head"),
              addressLine2 = dependencies.getConfirmedAddressResponse.lines.lift(1),
              addressLine3 = dependencies.getConfirmedAddressResponse.lines.lift(2),
              addressLine4 = dependencies.getConfirmedAddressResponse.lines.lift(3),
              postalCode = dependencies.getConfirmedAddressResponse.postcode,
              countryCode = dependencies.getConfirmedAddressResponse.country.code
            )
          ))

      object whenProvidingNewBusinessName:

        val newBusinessName: String = "New Agent Business Llp"
        val afterBusinessNameProvided: AgentApplication = baseForSectionAgentDetails
          .modify(_.agentDetails)
          .setTo(Some(AgentDetails(
            businessName = AgentBusinessName(
              agentBusinessName = "other",
              otherAgentBusinessName = Some(newBusinessName)
            )
          )))
        val afterBprTelephoneNumberSelected: AgentApplication = afterBusinessNameProvided
          .modify(_.agentDetails.each.telephoneNumber)
          .setTo(Some(AgentTelephoneNumberHelper.afterBprTelephoneNumberSelected))
        val afterContactTelephoneSelected: AgentApplication = afterBusinessNameProvided
          .modify(_.agentDetails.each.telephoneNumber)
          .setTo(Some(AgentTelephoneNumberHelper.afterContactTelephoneSelected))
        val afterOtherTelephoneNumberProvided: AgentApplication = afterBusinessNameProvided
          .modify(_.agentDetails.each.telephoneNumber)
          .setTo(Some(AgentTelephoneNumberHelper.afterOtherTelephoneNumberProvided))

    private object AgentTelephoneNumberHelper:

      val afterBprTelephoneNumberSelected: AgentTelephoneNumber = AgentTelephoneNumber(
        agentTelephoneNumber = dependencies.bprPrimaryTelephoneNumber,
        otherAgentTelephoneNumber = None
      )

      val afterContactTelephoneSelected: AgentTelephoneNumber = AgentTelephoneNumber(
        agentTelephoneNumber = dependencies.telephoneNumber.value,
        otherAgentTelephoneNumber = None
      )

      val afterOtherTelephoneNumberProvided: AgentTelephoneNumber = AgentTelephoneNumber(
        agentTelephoneNumber = "other",
        otherAgentTelephoneNumber = Some(dependencies.newTelephoneNumber)
      )

    private object AgentEmailAddressHelper:

      val afterContactEmailAddressSelected: AgentVerifiedEmailAddress = AgentVerifiedEmailAddress(
        emailAddress = AgentEmailAddress(
          agentEmailAddress = dependencies.applicantEmailAddress.value,
          otherAgentEmailAddress = None
        ),
        isVerified = true
      )

      val afterBprEmailAddressSelected: AgentVerifiedEmailAddress = AgentVerifiedEmailAddress(
        emailAddress = AgentEmailAddress(
          agentEmailAddress = dependencies.bprEmailAddress,
          otherAgentEmailAddress = None
        ),
        isVerified = true
      )

      val afterOtherEmailAddressSelected: AgentVerifiedEmailAddress = AgentVerifiedEmailAddress(
        emailAddress = AgentEmailAddress(
          agentEmailAddress = "other",
          otherAgentEmailAddress = Some(dependencies.newEmailAddress)
        ),
        isVerified = false
      )

      val afterVerifiedEmailAddressSelected: AgentVerifiedEmailAddress = AgentVerifiedEmailAddress(
        emailAddress = AgentEmailAddress(
          agentEmailAddress = "other",
          otherAgentEmailAddress = Some(dependencies.newEmailAddress)
        ),
        isVerified = true
      )

}
