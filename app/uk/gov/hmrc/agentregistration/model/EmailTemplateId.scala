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

package uk.gov.hmrc.agentregistration.model

import play.api.libs.json.JsString
import play.api.libs.json.Writes

enum EmailTemplateId(val id: String):

  case ApplicationReadyToSubmit
  extends EmailTemplateId("agent_registration_application_ready_to_submit")
  
  case ApplicationReadyToSubmitSoleTraderNotBusinessOwner
  extends EmailTemplateId("agent_registration_application_ready_to_submit_sole_trader_not_business_owner")

object EmailTemplateId:

  given Writes[EmailTemplateId] = Writes(o => JsString(o.id))
