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

package uk.gov.hmrc.agentregistration.shared.lists

import play.api.libs.json.JsValue
import play.api.libs.json.Json
import uk.gov.hmrc.agentregistration.testsupport.UnitSpec
import uk.gov.hmrc.agentregistration.testsupport.testdata.TdAll

class IndividualNameSpec
extends UnitSpec:

  private val tdAll: TdAll = TdAll()

  "serialize and deserialize IndividualName" in:
    val individualName: IndividualName = tdAll.individualName
    val individualNameJson: JsValue = Json.parse(
      // language=JSON
      "\"Test Name\""
    )
    Json.toJson(individualName) shouldBe individualNameJson
    individualNameJson.as[IndividualName] shouldBe individualName

  "isValidName regex should permit unicode letters, unicode diacritic marks, spaces, straight apostrophes, smart right quotes, hyphens and periods in names" in:
    val validNames: Seq[String] = Seq(
      "Taylor Leadenhall-Lane",
      "Mathias Brask Løvring",
      "Anne-Marie O'Connor",
      "Siobhán O’Neill",
      "Mathias Brask Van Der Waals",
      "María-José D'Ávila"
    )
    validNames.forall(IndividualName(_).isValidName) shouldBe true

  "isValidName regex should reject all characters except letters, diacritics, spaces, apostrophes, hyphens, and periods" in:
    val invalidNames: Seq[String] = Seq(
      "John Doe!",
      "Jane Doe@",
      "Alice Johnson$",
      "Michael Brown%",
      "Emily Davis^",
      "WilL14m Wilson",
      "William Wilson&",
      "Olivia Taylor*",
      "James Anderson(",
      "Sophia Thomas)",
      "Benjamin Jackson_",
      "Ava White+",
      "Elijah Harris=",
      "Charlotte Martin{",
      "Lucas Thompson}",
      "Amelia Garcia[",
      "Mason Martinez]",
      "Harper Robinson|",
      "Ethan Clark\\",
      "Abigail Rodriguez:",
      "Alexander Lewis;",
      "Ella Lee\"",
      "Scarlett Hall<",
      "Matthew Allen>",
      "Victoria Young,",
      "Grace King?"
    )
    invalidNames.forall(IndividualName(_).isValidName) shouldBe false
