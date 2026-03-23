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

import uk.gov.hmrc.agentregistration.shared.businessdetails.*

trait TdGrsBusinessDetails {
  dependencies: TdBase =>

  object grsBusinessDetails:

    object llp:

      val businessDetails = BusinessDetailsLlp(
        safeId = dependencies.safeId,
        saUtr = dependencies.saUtr,
        companyProfile = dependencies.companyProfile
      )

    object soleTrader:

      val businessDetails = BusinessDetailsSoleTrader(
        safeId = dependencies.safeId,
        saUtr = dependencies.saUtr,
        fullName = dependencies.fullName,
        dateOfBirth = dependencies.dateOfBirth,
        nino = Some(dependencies.nino),
        trn = None
      )

    object ltd:

      val businessDetails: BusinessDetailsLimitedCompany = BusinessDetailsLimitedCompany(
        safeId = dependencies.safeId,
        ctUtr = dependencies.ctUtr,
        companyProfile = dependencies.companyProfile
      )

    object ltdPartnership:

      val businessDetails: BusinessDetailsPartnership = BusinessDetailsPartnership(
        safeId = dependencies.safeId,
        saUtr = dependencies.saUtr,
        postcode = dependencies.postcode,
        companyProfile = dependencies.companyProfile
      )

    object scottishLtdPartnership:

      val businessDetails: BusinessDetailsPartnership = BusinessDetailsPartnership(
        safeId = dependencies.safeId,
        saUtr = dependencies.saUtr,
        postcode = dependencies.postcode,
        companyProfile = dependencies.companyProfile
      )

    object generalPartnership:

      val businessDetails: BusinessDetailsGeneralPartnership = BusinessDetailsGeneralPartnership(
        safeId = dependencies.safeId,
        saUtr = dependencies.saUtr,
        postcode = dependencies.postcode
      )

    object scottishPartnership:

      val businessDetails: BusinessDetailsScottishPartnership = BusinessDetailsScottishPartnership(
        safeId = dependencies.safeId,
        saUtr = dependencies.saUtr,
        postcode = dependencies.postcode
      )

}
