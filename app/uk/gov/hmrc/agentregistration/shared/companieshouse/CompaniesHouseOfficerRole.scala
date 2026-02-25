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

enum CompaniesHouseOfficerRole(val value: String):

  case Director
  extends CompaniesHouseOfficerRole("director")
  case LlpMember
  extends CompaniesHouseOfficerRole("llp-member")
  case Unsupported(override val value: String)
  extends CompaniesHouseOfficerRole(value)

object CompaniesHouseOfficerRole:

  given Format[CompaniesHouseOfficerRole] =
    new Format[CompaniesHouseOfficerRole]:
      override def reads(json: JsValue): JsResult[CompaniesHouseOfficerRole] =
        json match
          case JsString(s) =>
            s match
              case Director.value => JsSuccess(Director)
              case LlpMember.value => JsSuccess(LlpMember)
              case other => JsSuccess(Unsupported(other))
          case _ => JsError("Expected a string for officer role")

      override def writes(role: CompaniesHouseOfficerRole): JsValue = JsString(role.value)

  extension (agentApplication: AgentApplication.IsIncorporated)
    def getCompaniesHouseOfficerRole: CompaniesHouseOfficerRole =
      agentApplication match
        case _: AgentApplicationLlp => LlpMember
        case _: AgentApplicationLimitedCompany => Director
        case _: AgentApplicationLimitedPartnership => Director
        case _: AgentApplicationScottishLimitedPartnership => Director
