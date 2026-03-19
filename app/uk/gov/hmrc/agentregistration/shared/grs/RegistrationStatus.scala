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

package uk.gov.hmrc.agentregistration.shared.grs

import play.api.libs.json.*
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.===

enum RegistrationStatus(val key: String):

  case GrsRegistered
  extends RegistrationStatus("REGISTERED")
  case GrsFailed
  extends RegistrationStatus("REGISTRATION_FAILED")
  case GrsNotCalled
  extends RegistrationStatus("REGISTRATION_NOT_CALLED")

object RegistrationStatus:
  given Format[RegistrationStatus] = Format(
    _.validate[String].flatMap { string =>
      RegistrationStatus.values.find(_.key === string).map(JsSuccess(_))
        .getOrElse(JsError(s"Unknown value for GrsRegistrationStatus: '$string'"))
    },
    status => JsString(status.key)
  )
