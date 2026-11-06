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

  "normaliseOfficerName should normalise names from Companies House API formats to title case" in:
    val companiesHouseApiNames: Seq[String] = Seq(
      "LEADENHALL-LANE, Taylor",
      "LØVRING, Mathias Brask",
      "O'CONNOR, Anne-Marie",
      "O’NEILL, Siobhán",
      "VAN DER WAALS, Mathias",
      "D'ÁVILA, María-José",
      "ST.JOHN, John"
    )
    val expectedNormalisation: Seq[String] = Seq(
      "Taylor Leadenhall-Lane", // should capitalise the L which follows a hyphen in Leadenhall-Lane
      "Mathias Brask Løvring",
      "Anne-Marie O'Connor", // should capitalise the C which follows a straight apostrophe in O'CONNOR
      "Siobhán O’Neill", // should capitalise the N which follows a smart quote (U+2019) from O’NEILL
      "Mathias Van Der Waals", // should capitalise each word in VAN DER WAALS as the V starts and the others follow white space
      "María-José D'Ávila", // should capitalise the Á which follows a straight apostrophe in D'ÁVILA
      "John St.John" // should capitalise the S which starts and J which follows a period in ST.JOHN
    )
    companiesHouseApiNames.map(CompaniesHouseOfficer.normaliseOfficerName) shouldBe expectedNormalisation
