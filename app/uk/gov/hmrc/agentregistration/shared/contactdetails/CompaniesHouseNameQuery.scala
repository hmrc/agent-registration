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

package uk.gov.hmrc.agentregistration.shared.contactdetails

import play.api.libs.json.Format
import play.api.libs.json.Json

final case class CompaniesHouseNameQuery(
  firstName: String,
  lastName: String
)

object CompaniesHouseNameQuery:

  implicit val format: Format[CompaniesHouseNameQuery] = Json.format[CompaniesHouseNameQuery]
  // TODO: this is a basic sense check in the absence of any business requirements and is as used in:
  // https://github.com/hmrc/pension-scheme-return-frontend/blob/d4075c7805e8975c8d5cfaf3174362dd5fe92c81/app/forms/NameDOBFormProvider.scala#L30
  private val nameRegex = "^[a-zA-Z\\-' ]+$"
  def isValidName(name: String): Boolean = name.matches(nameRegex)
  def unapply(q: CompaniesHouseNameQuery): Option[(String, String)] = Some((q.firstName, q.lastName))
