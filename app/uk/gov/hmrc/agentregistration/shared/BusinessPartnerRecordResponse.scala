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
import uk.gov.hmrc.agentregistration.shared.util.Errors.getOrThrowExpectedDataMissing

final case class BusinessPartnerRecordResponse(
  organisationName: Option[String],
  individualName: Option[String],
  address: DesBusinessAddress,
  emailAddress: Option[String],
  primaryPhoneNumber: Option[String]
):

  def getEntityName: String = organisationName.orElse(individualName).getOrThrowExpectedDataMissing("No entity name found")

object BusinessPartnerRecordResponse:
  given format: Format[BusinessPartnerRecordResponse] = Json.format[BusinessPartnerRecordResponse]
