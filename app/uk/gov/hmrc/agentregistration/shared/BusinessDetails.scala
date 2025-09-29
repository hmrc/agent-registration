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

package uk.gov.hmrc.agentregistration.shared

import play.api.libs.json.Format
import play.api.libs.json.Json
import play.api.libs.json.Reads
import uk.gov.hmrc.agentregistration.shared.BusinessType.*

import java.time.LocalDate

sealed trait BusinessDetails:

  val safeId: SafeId
  val businessType: BusinessType // Duplicated from AgentApplication to simplify json reads

object BusinessDetails:
  given Format[BusinessDetails] = Format(
    { json =>
      (json \ "businessType").as[BusinessType] match {
        case LimitedCompany => Json.fromJson[LimitedCompanyDetails](json)
        case SoleTrader => Json.fromJson[SoleTraderDetails](json)
        case GeneralPartnership | LimitedLiabilityPartnership | LimitedPartnership | ScottishLimitedPartnership | ScottishPartnership =>
          Json.fromJson[PartnershipDetails](json)
      }
    },
    {
      case limitedCompany: LimitedCompanyDetails => Json.toJson(limitedCompany)
      case soleTrader: SoleTraderDetails => Json.toJson(soleTrader)
      case partnership: PartnershipDetails => Json.toJson(partnership)
    }
  )

final case class LimitedCompanyDetails(
  safeId: SafeId,
  businessType: BusinessType = LimitedCompany,
  companyProfile: CompanyProfile
)
extends BusinessDetails

object LimitedCompanyDetails:
  given Format[LimitedCompanyDetails] = Json.format[LimitedCompanyDetails]

final case class SoleTraderDetails(
  safeId: SafeId,
  businessType: BusinessType = SoleTrader,
  fullName: FullName,
  dateOfBirth: LocalDate,
  nino: Option[Nino],
  trn: Option[String]
  // saPostcode (only when trn present)
  // address (only when trn present)
  // overseas company details (optional and only when trn present)
)
extends BusinessDetails
//  def getNinoOrTrn: String = nino.orElse(trn).getOrElse(throw new RuntimeException("Sole trader missing nino and trn"))

object SoleTraderDetails:
  given Format[SoleTraderDetails] = Json.format[SoleTraderDetails]

final case class PartnershipDetails(
  safeId: SafeId,
  businessType: BusinessType,
  companyProfile: Option[CompanyProfile],
  postcode: String
)
extends BusinessDetails

object PartnershipDetails:
  given Format[PartnershipDetails] = Json.format[PartnershipDetails]

final case class FullName(
  firstName: String,
  lastName: String
)

object FullName:
  given Format[FullName] = Json.format[FullName]

final case class CompanyProfile(
  companyNumber: String,
  companyName: String,
  dateOfIncorporation: Option[LocalDate] // for some reason an optional field on companies house
  // unsanitisedCHROAddress: Option[Address]
)

object CompanyProfile:
  given Format[CompanyProfile] = Json.format[CompanyProfile]
