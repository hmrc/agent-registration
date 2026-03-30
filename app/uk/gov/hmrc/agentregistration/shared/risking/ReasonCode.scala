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

import play.api.libs.json.Format
import play.api.libs.json.JsError
import play.api.libs.json.JsString
import play.api.libs.json.JsSuccess
import play.api.libs.json.Reads
import play.api.libs.json.Writes
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.===

enum ReasonCode(val value: String):

  case ReasonCode3_1
  extends ReasonCode("3.1")
  case ReasonCode3_2
  extends ReasonCode("3.2")
  case ReasonCode3_3
  extends ReasonCode("3.3")
  case ReasonCode3_4
  extends ReasonCode("3.4")
  case ReasonCode3_5
  extends ReasonCode("3.5")

  case ReasonCode4_1
  extends ReasonCode("4.1")
  case ReasonCode4_2
  extends ReasonCode("4.2")
  case ReasonCode4_3
  extends ReasonCode("4.3")
  case ReasonCode4_4
  extends ReasonCode("4.4")

  case ReasonCode5_1
  extends ReasonCode("5.1")
  case ReasonCode5_2
  extends ReasonCode("5.2")
  case ReasonCode5_3
  extends ReasonCode("5.3")
  case ReasonCode5_4
  extends ReasonCode("5.4")
  case ReasonCode5_5
  extends ReasonCode("5.5")
  case ReasonCode5_6
  extends ReasonCode("5.6")

  case ReasonCode6
  extends ReasonCode("6")

  case ReasonCode7
  extends ReasonCode("7")

  case ReasonCode8_1
  extends ReasonCode("8.1")
  case ReasonCode8_4
  extends ReasonCode("8.4")
  case ReasonCode8_5
  extends ReasonCode("8.5")
  case ReasonCode8_6
  extends ReasonCode("8.6")
  case ReasonCode8_7
  extends ReasonCode("8.7")

  case ReasonCode9
  extends ReasonCode("9")

  case ReasonCode10_1
  extends ReasonCode("10.1")
  case ReasonCode10_2
  extends ReasonCode("10.2")

object ReasonCode:

  given Format[ReasonCode] = Format(
    Reads[ReasonCode] {
      case JsString(raw) =>
        fromValue(raw)
          .map(JsSuccess(_))
          .getOrElse(JsError(s"Unknown value for enum ReasonCode: '$raw'"))
      case other => JsError("Unexpected type received for enum ReasonCode")
    },
    Writes[ReasonCode](reasonCode => JsString(reasonCode.value))
  )

  private def fromValue(s: String): Option[ReasonCode] = ReasonCode.values.find(_.value === s)

  private val NonFixableReasonCodes: Set[ReasonCode] = Set(
    ReasonCode6,
    ReasonCode7,
    ReasonCode8_1,
    ReasonCode8_4,
    ReasonCode8_6,
    ReasonCode9
  )

  extension (reasonCode: ReasonCode)

    def isNonFixable: Boolean = NonFixableReasonCodes.contains(reasonCode)

    def isFixable: Boolean = !reasonCode.isNonFixable
