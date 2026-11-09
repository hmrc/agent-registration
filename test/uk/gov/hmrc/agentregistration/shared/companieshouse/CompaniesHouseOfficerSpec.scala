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

import play.api.libs.json.JsValue
import play.api.libs.json.Json
import uk.gov.hmrc.agentregistration.testsupport.UnitSpec
import uk.gov.hmrc.agentregistration.testsupport.testdata.TdAll

class CompaniesHouseOfficerSpec
extends UnitSpec:

  private val tdAll: TdAll = TdAll()

  "serialize and deserialize CompaniesHouseOfficer" in:
    val companiesHouseOfficer: CompaniesHouseOfficer = tdAll.companiesHouseOfficer
    val companiesHouseOfficerJson: JsValue = Json.parse(
      // language=JSON
      """{
           "name": "Taylor Leadenhall-Lane",
           "dateOfBirth": {
             "day": 12,
             "month": 11,
             "year": 1990
           },
           "officerRole": "llp-member"
         }"""
    )
    Json.toJson(companiesHouseOfficer) shouldBe companiesHouseOfficerJson
    companiesHouseOfficerJson.as[CompaniesHouseOfficer] shouldBe companiesHouseOfficer
