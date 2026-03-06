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
import uk.gov.hmrc.agentregistration.shared.CheckResult
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.===

enum CompaniesHouseStatus(val key: String):

  case Active
  extends CompaniesHouseStatus("active")
  case Dissolved
  extends CompaniesHouseStatus("dissolved")
  case Liquidation
  extends CompaniesHouseStatus("liquidation")
  case Receivership
  extends CompaniesHouseStatus("receivership")
  case Administration
  extends CompaniesHouseStatus("administration")
  case VoluntaryArrangement
  extends CompaniesHouseStatus("voluntary-arrangement")
  case ConvertedClosed
  extends CompaniesHouseStatus("converted-closed")
  case InsolvencyProceedings
  extends CompaniesHouseStatus("insolvency-proceedings")
  case Registered
  extends CompaniesHouseStatus("registered")
  case Removed
  extends CompaniesHouseStatus("removed")
  case Closed
  extends CompaniesHouseStatus("closed")
  case Open
  extends CompaniesHouseStatus("open")

object CompaniesHouseStatus:

  given Format[CompaniesHouseStatus] =
    new Format[CompaniesHouseStatus] {
      override def reads(json: JsValue): JsResult[CompaniesHouseStatus] =
        json match {
          case JsString(s) =>
            CompaniesHouseStatus.values.find(_.key === s) match {
              case Some(status) => JsSuccess(status)
              case None => JsError(s"Unknown company status: $s")
            }
          case _ => JsError("Expected a string for company status")
        }

      override def writes(o: CompaniesHouseStatus): JsValue = JsString(o.key)
    }

  extension (status: CompaniesHouseStatus)
    def toCheckResult: CheckResult =
      status match
        case Active | Administration | VoluntaryArrangement | Registered | Open => CheckResult.Pass
        case _ => CheckResult.Fail
