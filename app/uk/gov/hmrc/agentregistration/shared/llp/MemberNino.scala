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

package uk.gov.hmrc.agentregistration.shared.llp

import uk.gov.hmrc.agentregistration.shared.Nino
import play.api.libs.json.*
import uk.gov.hmrc.agentregistration.shared.util.JsonConfig

import scala.annotation.nowarn

sealed trait MemberNino

sealed trait UserProvidedNino
extends MemberNino

object MemberNino:

  final case class Provided(nino: Nino)
  extends MemberNino,
    UserProvidedNino

  case object NotProvided
  extends MemberNino,
    UserProvidedNino

  final case class FromAuth(nino: Nino)
  extends MemberNino

  extension (memberNino: MemberNino)
    def toUserProvidedNino: UserProvidedNino =
      memberNino match
        case u: UserProvidedNino => u
        case h: FromAuth => throw new IllegalArgumentException(s"Nino is already provided from auth enrolments (${h.nino})")

  @nowarn()
  given OFormat[MemberNino] =
    given JsonConfiguration = JsonConfig.jsonConfiguration
    given OFormat[NotProvided.type] = Json.format[NotProvided.type]
    given OFormat[Provided] = Json.format[Provided]
    given OFormat[FromAuth] = Json.format[FromAuth]

    val dontDeleteMe = """
                         |Don't delete me.
                         |I will emit a warning so `@nowarn` can be applied to address below
                         |`Unreachable case except for null` problem emited by Play Json macro"""

    Json.format[MemberNino]
