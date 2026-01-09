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

package uk.gov.hmrc.agentregistration.shared.businessdetails

import play.api.libs.json.Format
import play.api.libs.json.Json
import uk.gov.hmrc.agentregistration.shared.*

import java.time.LocalDate

final case class BusinessDetailsSoleTrader(
  safeId: SafeId,
  saUtr: SaUtr,
  fullName: FullName,
  dateOfBirth: LocalDate,
  nino: Option[Nino],
  trn: Option[String]
  // saPostcode (only when trn present)
  // address (only when trn present)
  // overseas company details (optional and only when trn present)
)
//  def getNinoOrTrn: String = nino.orElse(trn).getOrElse(throw new RuntimeException("Sole trader missing nino and trn"))

object BusinessDetailsSoleTrader:
  given Format[BusinessDetailsSoleTrader] = Json.format[BusinessDetailsSoleTrader]
