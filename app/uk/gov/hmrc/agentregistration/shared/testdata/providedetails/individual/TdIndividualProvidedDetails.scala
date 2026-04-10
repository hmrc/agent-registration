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

package uk.gov.hmrc.agentregistration.shared.testdata.providedetails.individual

import com.softwaremill.quicklens.modify
import uk.gov.hmrc.agentregistration.shared.StateOfAgreement
import uk.gov.hmrc.agentregistration.shared.StateOfAgreement.Agreed
import uk.gov.hmrc.agentregistration.shared.individual.IndividualNino
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetails
import uk.gov.hmrc.agentregistration.shared.individual.IndividualSaUtr
import uk.gov.hmrc.agentregistration.shared.individual.IndividualVerifiedEmailAddress
import uk.gov.hmrc.agentregistration.shared.individual.ProvidedDetailsState
import uk.gov.hmrc.agentregistration.shared.individual.ProvidedDetailsState.AccessConfirmed
import uk.gov.hmrc.agentregistration.shared.individual.ProvidedDetailsState.Finished
import uk.gov.hmrc.agentregistration.shared.individual.ProvidedDetailsState.Precreated
import uk.gov.hmrc.agentregistration.shared.individual.ProvidedDetailsState.Started
import uk.gov.hmrc.agentregistration.shared.lists.IndividualName
import uk.gov.hmrc.agentregistration.shared.testdata.TdBase

trait TdIndividualProvidedDetails { dependencies: TdBase =>

  object providedDetails:

    val precreated: IndividualProvidedDetails = IndividualProvidedDetails(
      _id = dependencies.individualProvidedDetailsId,
      internalUserId = None,
      individualName = dependencies.individualName,
      createdAt = dependencies.nowAsInstant,
      agentApplicationId = dependencies.agentApplicationId,
      providedDetailsState = Precreated,
      isPersonOfControl = true,
      passedIv = None
    )

    val afterAccessConfirmed: IndividualProvidedDetails = IndividualProvidedDetails(
      _id = dependencies.individualProvidedDetailsId,
      internalUserId = None,
      individualName = dependencies.individualName,
      createdAt = dependencies.nowAsInstant,
      agentApplicationId = dependencies.agentApplicationId,
      providedDetailsState = AccessConfirmed,
      isPersonOfControl = true,
      passedIv = None
    )

    val afterStarted: IndividualProvidedDetails = IndividualProvidedDetails(
      _id = dependencies.individualProvidedDetailsId,
      internalUserId = Some(dependencies.internalUserId),
      individualName = dependencies.individualName,
      createdAt = dependencies.nowAsInstant,
      agentApplicationId = dependencies.agentApplicationId,
      providedDetailsState = Started,
      isPersonOfControl = true,
      passedIv = Some(true)
    )

    val afterTelephoneNumberProvided: IndividualProvidedDetails = afterStarted
      .modify(_.telephoneNumber)
      .setTo(Some(dependencies.telephoneNumber))

    val afterEmailAddressProvided: IndividualProvidedDetails = afterTelephoneNumberProvided
      .modify(_.emailAddress)
      .setTo(Some(IndividualVerifiedEmailAddress(
        emailAddress = dependencies.individualEmailAddress,
        isVerified = false
      )))

    val afterEmailAddressVerified: IndividualProvidedDetails = afterTelephoneNumberProvided
      .modify(_.emailAddress)
      .setTo(Some(IndividualVerifiedEmailAddress(
        emailAddress = dependencies.individualEmailAddress,
        isVerified = true
      )))

    object AfterDateOfBirth:

      val afterDateOfBirthProvided: IndividualProvidedDetails = afterEmailAddressVerified
        .modify(_.individualDateOfBirth)
        .setTo(Some(dependencies.dateOfBirthProvided))

      val afterDateOfBirthFromCitizenDetails: IndividualProvidedDetails = afterEmailAddressVerified
        .modify(_.individualDateOfBirth)
        .setTo(Some(dependencies.dateOfBirthFromCitizenDetails))

    object AfterNino:

      val afterNinoProvided: IndividualProvidedDetails = AfterDateOfBirth.afterDateOfBirthProvided
        .modify(_.individualNino)
        .setTo(Some(dependencies.ninoProvided))

      val afterNinoFromAuth: IndividualProvidedDetails = AfterDateOfBirth.afterDateOfBirthFromCitizenDetails
        .modify(_.individualNino)
        .setTo(Some(dependencies.ninoFromAuth))

      val afterNinoNotProvided: IndividualProvidedDetails = afterEmailAddressVerified
        .modify(_.individualNino)
        .setTo(Some(IndividualNino.NotProvided))

    object AfterSaUtr:

      val afterSaUtrProvided: IndividualProvidedDetails = AfterNino.afterNinoProvided
        .modify(_.individualSaUtr)
        .setTo(Some(dependencies.saUtrProvided))

      val afterSaUtrFromAuth: IndividualProvidedDetails = AfterNino.afterNinoFromAuth
        .modify(_.individualSaUtr)
        .setTo(Some(dependencies.saUtrFromAuth))

      val afterSaUtrFromCitizenDetails: IndividualProvidedDetails = AfterNino.afterNinoFromAuth
        .modify(_.individualSaUtr)
        .setTo(Some(dependencies.saUtrFromCitizenDetails))

      val afterSaUtrNotProvided: IndividualProvidedDetails = AfterNino.afterNinoNotProvided
        .modify(_.individualSaUtr)
        .setTo(Some(IndividualSaUtr.NotProvided))

    val afterApproveAgentApplication: IndividualProvidedDetails = AfterSaUtr.afterSaUtrProvided
      .modify(_.hasApprovedApplication)
      .setTo(Some(true))

    val afterDoNotApproveAgentApplication: IndividualProvidedDetails = AfterSaUtr.afterSaUtrProvided
      .modify(_.hasApprovedApplication)
      .setTo(Some(false))

    val afterHmrcStandardforAgentsAgreed: IndividualProvidedDetails = afterApproveAgentApplication
      .modify(_.hmrcStandardForAgentsAgreed)
      .setTo(StateOfAgreement.Agreed)

    val afterFinished: IndividualProvidedDetails = afterHmrcStandardforAgentsAgreed
      .modify(_.providedDetailsState)
      .setTo(Finished)

    object soleTrader:

      val soleTraderAutopopulatedDetails: IndividualProvidedDetails = IndividualProvidedDetails(
        _id = individualProvidedDetailsId,
        internalUserId = None,
        createdAt = nowAsInstant,
        agentApplicationId = agentApplicationId,
        providedDetailsState = ProvidedDetailsState.AccessConfirmed,
        individualName = IndividualName(fullName.toStringFull),
        isPersonOfControl = true,
        telephoneNumber = Some(telephoneNumber),
        emailAddress = Some(IndividualVerifiedEmailAddress(applicantEmailAddress, isVerified = true)),
        hmrcStandardForAgentsAgreed = Agreed,
        hasApprovedApplication = Some(true),
        passedIv = None
      )

}
