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

package uk.gov.hmrc.agentregistration.shared.risking

import play.api.libs.json.*
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.===

enum CheckId(val value: String):

  case CheckId3
  extends CheckId("3")
  case CheckId4
  extends CheckId("4")
  case CheckId5
  extends CheckId("5")
  case CheckId6
  extends CheckId("6")
  case CheckId7
  extends CheckId("7")
  case CheckId8
  extends CheckId("8")
  case CheckId9
  extends CheckId("9")
  case CheckId10
  extends CheckId("10")

object CheckId:

  given Format[CheckId] = Format(
    Reads[CheckId] {
      case JsString(raw) =>
        fromValue(raw)
          .map(JsSuccess(_))
          .getOrElse(JsError(s"Unknown value for enum CheckId: '$raw'"))
      case other => JsError("Unexpected type received for enum CheckId")
    },
    Writes[CheckId](checkId => JsString(checkId.value))
  )

  private def fromValue(s: String): Option[CheckId] = CheckId.values.find(_.value === s)
