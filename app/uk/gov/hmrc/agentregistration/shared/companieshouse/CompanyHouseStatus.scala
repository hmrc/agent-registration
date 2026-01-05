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

import play.api.libs.json.Format
import play.api.libs.json.JsError
import play.api.libs.json.JsResult
import play.api.libs.json.JsString
import play.api.libs.json.JsSuccess
import play.api.libs.json.JsValue
import uk.gov.hmrc.agentregistration.shared.CompanyStatusCheckResult
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.===

enum CompanyHouseStatus(val key: String):

  case Active
  extends CompanyHouseStatus("active")
  case Dissolved
  extends CompanyHouseStatus("dissolved")
  case Liquidation
  extends CompanyHouseStatus("liquidation")
  case Receivership
  extends CompanyHouseStatus("receivership")
  case Administration
  extends CompanyHouseStatus("administration")
  case VoluntaryArrangement
  extends CompanyHouseStatus("voluntary-arrangement")
  case ConvertedClosed
  extends CompanyHouseStatus("converted-closed")
  case InsolvencyProceedings
  extends CompanyHouseStatus("insolvency-proceedings")
  case Registered
  extends CompanyHouseStatus("registered")
  case Removed
  extends CompanyHouseStatus("removed")
  case Closed
  extends CompanyHouseStatus("closed")
  case Open
  extends CompanyHouseStatus("open")

object CompanyHouseStatus:

  given Format[CompanyHouseStatus] =
    new Format[CompanyHouseStatus] {
      override def reads(json: JsValue): JsResult[CompanyHouseStatus] =
        json match {
          case JsString(s) =>
            CompanyHouseStatus.values.find(_.key === s) match {
              case Some(status) => JsSuccess(status)
              case None => JsError(s"Unknown company status: $s")
            }
          case _ => JsError("Expected a string for company status")
        }

      override def writes(o: CompanyHouseStatus): JsValue = JsString(o.key)
    }

  extension (status: CompanyHouseStatus)
    def toCompanyStatusCheckResult: CompanyStatusCheckResult =
      status match
        case Active | Administration | VoluntaryArrangement | Registered | Open => CompanyStatusCheckResult.Allow
        case _ => CompanyStatusCheckResult.Block
