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

package uk.gov.hmrc.agentregistration.shared.testdata

import uk.gov.hmrc.agentregistration.shared.businessdetails.BusinessDetailsGeneralPartnership
import uk.gov.hmrc.agentregistration.shared.businessdetails.BusinessDetailsLimitedCompany
import uk.gov.hmrc.agentregistration.shared.businessdetails.BusinessDetailsLlp
import uk.gov.hmrc.agentregistration.shared.businessdetails.BusinessDetailsPartnership
import uk.gov.hmrc.agentregistration.shared.businessdetails.BusinessDetailsScottishPartnership
import uk.gov.hmrc.agentregistration.shared.businessdetails.BusinessDetailsSoleTrader
import uk.gov.hmrc.agentregistration.shared.businessdetails.FullName
import uk.gov.hmrc.agentregistration.shared.grs.JourneyData
import uk.gov.hmrc.agentregistration.shared.grs.JourneyId
import uk.gov.hmrc.agentregistration.shared.grs.Registration
import uk.gov.hmrc.agentregistration.shared.grs.RegistrationStatus
import uk.gov.hmrc.agentregistration.shared.util.Errors.getOrThrowExpectedDataMissing

import java.time.LocalDate

trait TdGrs {
  dependencies: TdBase =>

  object grs:

    val journeyId: JourneyId = JourneyId("1234567890-8888")

    object llp:

      val journeyData: JourneyData = JourneyData(
        fullName = None,
        dateOfBirth = None,
        nino = None,
        trn = None,
        sautr = Some(dependencies.saUtr),
        companyProfile = Some(dependencies.companyProfile),
        ctutr = None,
        postcode = Some(dependencies.postcode),
        identifiersMatch = true,
        registration = Registration(
          registrationStatus = RegistrationStatus.GrsRegistered,
          registeredBusinessPartnerId = Some(dependencies.safeId)
        )
      )

      val businessDetails = BusinessDetailsLlp(
        safeId = journeyData.registration.registeredBusinessPartnerId.getOrThrowExpectedDataMissing("registration.registeredBusinessPartnerId"),
        saUtr = journeyData.sautr.getOrThrowExpectedDataMissing("sautr"),
        companyProfile = journeyData.companyProfile.getOrThrowExpectedDataMissing("companyProfile")
      )

    object soleTrader:

      val fullName: FullName = FullName(firstName = "ST Name", lastName = "ST Lastname")
      val dateOfBirth: LocalDate = LocalDate.of(1990, 1, 2)
      val journeyData: JourneyData = JourneyData(
        fullName = Some(fullName),
        dateOfBirth = Some(dateOfBirth),
        nino = Some(dependencies.nino),
        trn = None,
        sautr = Some(dependencies.saUtr),
        companyProfile = None,
        ctutr = None,
        postcode = None,
        identifiersMatch = true,
        registration = Registration(
          registrationStatus = RegistrationStatus.GrsRegistered,
          registeredBusinessPartnerId = Some(dependencies.safeId)
        )
      )

      val businessDetails = BusinessDetailsSoleTrader(
        safeId = journeyData.registration.registeredBusinessPartnerId.getOrThrowExpectedDataMissing("registration.registeredBusinessPartnerId"),
        saUtr = journeyData.sautr.getOrThrowExpectedDataMissing("sautr"),
        fullName = journeyData.fullName.getOrThrowExpectedDataMissing("fullName"),
        dateOfBirth = journeyData.dateOfBirth.getOrThrowExpectedDataMissing("dateOfBirth"),
        nino = journeyData.nino,
        trn = journeyData.trn
      )

    object ltd:

      val journeyData: JourneyData = JourneyData(
        fullName = None,
        dateOfBirth = None,
        nino = None,
        trn = None,
        sautr = None,
        companyProfile = Some(dependencies.companyProfile),
        ctutr = Some(dependencies.ctUtr),
        postcode = None,
        identifiersMatch = true,
        registration = Registration(
          registrationStatus = RegistrationStatus.GrsRegistered,
          registeredBusinessPartnerId = Some(dependencies.safeId)
        )
      )

      val businessDetails: BusinessDetailsLimitedCompany = BusinessDetailsLimitedCompany(
        safeId = journeyData.registration.registeredBusinessPartnerId.getOrThrowExpectedDataMissing("registration.registeredBusinessPartnerId"),
        ctUtr = journeyData.ctutr.getOrThrowExpectedDataMissing("ctutr"),
        companyProfile = journeyData.companyProfile.getOrThrowExpectedDataMissing("companyProfile")
      )

    object ltdPartnership:

      val journeyData: JourneyData = JourneyData(
        fullName = None,
        dateOfBirth = None,
        nino = None,
        trn = None,
        sautr = Some(dependencies.saUtr),
        companyProfile = Some(dependencies.companyProfile),
        ctutr = None,
        postcode = Some(dependencies.postcode),
        identifiersMatch = true,
        registration = Registration(
          registrationStatus = RegistrationStatus.GrsRegistered,
          registeredBusinessPartnerId = Some(dependencies.safeId)
        )
      )

      val businessDetails: BusinessDetailsPartnership = BusinessDetailsPartnership(
        safeId = journeyData.registration.registeredBusinessPartnerId.getOrThrowExpectedDataMissing("registration.registeredBusinessPartnerId"),
        saUtr = journeyData.sautr.getOrThrowExpectedDataMissing("saUtr"),
        postcode = journeyData.postcode.getOrThrowExpectedDataMissing("postcode"),
        companyProfile = journeyData.companyProfile.getOrThrowExpectedDataMissing("companyProfile")
      )

    object scottishLtdPartnership:

      val journeyData: JourneyData = JourneyData(
        fullName = None,
        dateOfBirth = None,
        nino = None,
        trn = None,
        sautr = Some(dependencies.saUtr),
        companyProfile = Some(dependencies.companyProfile),
        ctutr = None,
        postcode = Some(dependencies.postcode),
        identifiersMatch = true,
        registration = Registration(
          registrationStatus = RegistrationStatus.GrsRegistered,
          registeredBusinessPartnerId = Some(dependencies.safeId)
        )
      )

      val businessDetails: BusinessDetailsPartnership = BusinessDetailsPartnership(
        safeId = journeyData.registration.registeredBusinessPartnerId.getOrThrowExpectedDataMissing("registration.registeredBusinessPartnerId"),
        saUtr = journeyData.sautr.getOrThrowExpectedDataMissing("saUtr"),
        postcode = journeyData.postcode.getOrThrowExpectedDataMissing("postcode"),
        companyProfile = journeyData.companyProfile.getOrThrowExpectedDataMissing("companyProfile")
      )

    object generalPartnership:

      val journeyData: JourneyData = JourneyData(
        fullName = None,
        dateOfBirth = None,
        nino = None,
        trn = None,
        sautr = Some(dependencies.saUtr),
        companyProfile = None,
        ctutr = None,
        postcode = Some(dependencies.postcode),
        identifiersMatch = true,
        registration = Registration(
          registrationStatus = RegistrationStatus.GrsRegistered,
          registeredBusinessPartnerId = Some(dependencies.safeId)
        )
      )

      val businessDetails: BusinessDetailsGeneralPartnership = BusinessDetailsGeneralPartnership(
        safeId = journeyData.registration.registeredBusinessPartnerId.getOrThrowExpectedDataMissing("registration.registeredBusinessPartnerId"),
        saUtr = journeyData.sautr.getOrThrowExpectedDataMissing("sautr"),
        postcode = journeyData.postcode.getOrThrowExpectedDataMissing("postcode")
      )

    object scottishPartnership:

      val journeyData: JourneyData = JourneyData(
        fullName = None,
        dateOfBirth = None,
        nino = None,
        trn = None,
        sautr = Some(dependencies.saUtr),
        companyProfile = None,
        ctutr = None,
        postcode = Some(dependencies.postcode),
        identifiersMatch = true,
        registration = Registration(
          registrationStatus = RegistrationStatus.GrsRegistered,
          registeredBusinessPartnerId = Some(dependencies.safeId)
        )
      )

      val businessDetails: BusinessDetailsScottishPartnership = BusinessDetailsScottishPartnership(
        safeId = journeyData.registration.registeredBusinessPartnerId.getOrThrowExpectedDataMissing("registration.registeredBusinessPartnerId"),
        saUtr = journeyData.sautr.getOrThrowExpectedDataMissing("sautr"),
        postcode = journeyData.postcode.getOrThrowExpectedDataMissing("postcode")
      )

}
