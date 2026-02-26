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

package uk.gov.hmrc.agentregistration.shared.companieshouse

import play.api.libs.json.*
import uk.gov.hmrc.agentregistration.shared.*
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.===

enum CompaniesHouseOfficerRole(val role: String):

  // CIC
  case CicManager
  extends CompaniesHouseOfficerRole("cic-manager")

  // Directors
  case Director
  extends CompaniesHouseOfficerRole("director")
  case CorporateDirector
  extends CompaniesHouseOfficerRole("corporate-director")
  case NomineeDirector
  extends CompaniesHouseOfficerRole("nominee-director")
  case CorporateNomineeDirector
  extends CompaniesHouseOfficerRole("corporate-nominee-director")

  // Secretaries (individual + corporate + nominees)
  case Secretary
  extends CompaniesHouseOfficerRole("secretary")
  case CorporateSecretary
  extends CompaniesHouseOfficerRole("corporate-secretary")
  case NomineeSecretary
  extends CompaniesHouseOfficerRole("nominee-secretary")
  case CorporateNomineeSecretary
  extends CompaniesHouseOfficerRole("corporate-nominee-secretary")

  // LLP (individual + corporate)
  case LlpMember
  extends CompaniesHouseOfficerRole("llp-member")
  case LlpDesignatedMember
  extends CompaniesHouseOfficerRole("llp-designated-member")
  case CorporateLlpMember
  extends CompaniesHouseOfficerRole("corporate-llp-member")
  case CorporateLlpDesignatedMember
  extends CompaniesHouseOfficerRole("corporate-llp-designated-member")

  // Partnerships (Limited Partnership / Scottish Limited Partnership)
  case GeneralPartnerLimitedPartnership
  extends CompaniesHouseOfficerRole("general-partner-in-a-limited-partnership")
  case LimitedPartnerLimitedPartnership
  extends CompaniesHouseOfficerRole("limited-partner-in-a-limited-partnership")

  // People authorised
  case PersonAuthorisedToAccept
  extends CompaniesHouseOfficerRole("person-authorised-to-accept")
  case PersonAuthorisedToRepresent
  extends CompaniesHouseOfficerRole("person-authorised-to-represent")
  case PersonAuthorisedToRepresentAndAccept
  extends CompaniesHouseOfficerRole("person-authorised-to-represent-and-accept")

  // Managing
  case ManagingOfficer
  extends CompaniesHouseOfficerRole("managing-officer")
  case CorporateManagingOfficer
  extends CompaniesHouseOfficerRole("corporate-managing-officer")
  case ManagerOfAnEeig
  extends CompaniesHouseOfficerRole("manager-of-an-eeig")
  case CorporateManagerOfAnEeig
  extends CompaniesHouseOfficerRole("corporate-manager-of-an-eeig")

  // governance bodies
  case MemberOfAManagementOrgan
  extends CompaniesHouseOfficerRole("member-of-a-management-organ")
  case CorporateMemberOfAManagementOrgan
  extends CompaniesHouseOfficerRole("corporate-member-of-a-management-organ")
  case MemberOfASupervisoryOrgan
  extends CompaniesHouseOfficerRole("member-of-a-supervisory-organ")
  case CorporateMemberOfASupervisoryOrgan
  extends CompaniesHouseOfficerRole("corporate-member-of-a-supervisory-organ")
  case MemberOfAnAdministrativeOrgan
  extends CompaniesHouseOfficerRole("member-of-an-administrative-organ")
  case CorporateMemberOfAnAdministrativeOrgan
  extends CompaniesHouseOfficerRole("corporate-member-of-an-administrative-organ")

  // Other special roles
  case JudicialFactor
  extends CompaniesHouseOfficerRole("judicial-factor")
  case ReceiverAndManager
  extends CompaniesHouseOfficerRole("receiver-and-manager")

object CompaniesHouseOfficerRole:

  given Format[CompaniesHouseOfficerRole] =
    new Format[CompaniesHouseOfficerRole] {
      override def reads(json: JsValue): JsResult[CompaniesHouseOfficerRole] =
        json match {
          case JsString(s) =>
            CompaniesHouseOfficerRole.values.find(_.role === s) match {
              case Some(role) => JsSuccess(role)
              case None => JsError(s"Unknown Companies house officer role: $s")
            }
          case _ => JsError("Expected a string for company status")
        }

      override def writes(o: CompaniesHouseOfficerRole): JsValue = JsString(o.role)
    }

  extension (agentApplication: AgentApplication.IsIncorporated)
    def getCompaniesHouseOfficerRole: Set[CompaniesHouseOfficerRole] =
      agentApplication match
        case _: AgentApplicationLimitedCompany =>
          Set(
            // Directors
            Director,
            CorporateDirector,
            NomineeDirector,
            CorporateNomineeDirector,
            // Secretaries
            Secretary,
            CorporateSecretary,
            NomineeSecretary,
            CorporateNomineeSecretary,
            // People authorised
            PersonAuthorisedToRepresent,
            PersonAuthorisedToAccept,
            PersonAuthorisedToRepresentAndAccept
          )

        case _: AgentApplicationLlp =>
          Set(
            // LLP (individual + corporate)
            LlpMember,
            LlpDesignatedMember,
            CorporateLlpMember,
            CorporateLlpDesignatedMember,
            // People authorised
            PersonAuthorisedToRepresent,
            PersonAuthorisedToAccept,
            PersonAuthorisedToRepresentAndAccept
          )

        case _: AgentApplicationLimitedPartnership =>
          Set(
            // Partnerships (Limited Partnership / Scottish Limited Partnership)
            GeneralPartnerLimitedPartnership,
            LimitedPartnerLimitedPartnership,
            // People authorised
            PersonAuthorisedToRepresent,
            PersonAuthorisedToAccept,
            PersonAuthorisedToRepresentAndAccept
          )

        case _: AgentApplicationScottishLimitedPartnership =>
          Set(
            // Partnerships (Limited Partnership / Scottish Limited Partnership)
            GeneralPartnerLimitedPartnership,
            LimitedPartnerLimitedPartnership,
            // People authorised
            PersonAuthorisedToRepresent,
            PersonAuthorisedToAccept,
            PersonAuthorisedToRepresentAndAccept
          )

//Roles that do not match to any category
/*
  case ManagingOfficer          extends CompaniesHouseOfficerRole("managing-officer")
  case CorporateManagingOfficer extends CompaniesHouseOfficerRole("corporate-managing-officer")
  case ManagerOfAnEeig          extends CompaniesHouseOfficerRole("manager-of-an-eeig")
  case CorporateManagerOfAnEeig extends CompaniesHouseOfficerRole("corporate-manager-of-an-eeig")

 */

//Not defined in proxy
/*
cic-manager
manager-of-an-eeig
corporate-manager-of-an-eeig
member-of-a-management-organ
corporate-member-of-a-management-organ
member-of-a-supervisory-organ
corporate-member-of-a-supervisory-organ
member-of-an-administrative-organ
corporate-member-of-an-administrative-organ
judicial-factor
receiver-and-manager
 */
