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

import play.api.libs.json.*
import uk.gov.hmrc.agentregistration.shared.SaUtr
import uk.gov.hmrc.agentregistration.shared.util.JsonConfig

import scala.annotation.nowarn

sealed trait MemberSaUtr

sealed trait UserProvidedSaUtr
extends MemberSaUtr

object MemberSaUtr:

  final case class Provided(saUtr: SaUtr)
  extends MemberSaUtr,
    UserProvidedSaUtr

  case object NotProvided
  extends MemberSaUtr,
    UserProvidedSaUtr

  final case class FromAuth(saUtr: SaUtr)
  extends MemberSaUtr

  final case class FromCitizenDetails(saUtr: SaUtr)
  extends MemberSaUtr

  extension (memberSaUtr: MemberSaUtr)
    def toUserProvidedSaUtr: UserProvidedSaUtr =
      memberSaUtr match
        case u: UserProvidedSaUtr => u
        case h: FromAuth => throw new IllegalArgumentException(s"Utr is already provided from auth enrolments (${h.saUtr})")
        case h: FromCitizenDetails => throw new IllegalArgumentException(s"Utr is already provided from citizen details (${h.saUtr})")

  @nowarn()
  given OFormat[MemberSaUtr] =
    given JsonConfiguration = JsonConfig.jsonConfiguration
    given OFormat[NotProvided.type] = Json.format[NotProvided.type]
    given OFormat[Provided] = Json.format[Provided]
    given OFormat[FromAuth] = Json.format[FromAuth]
    given OFormat[FromCitizenDetails] = Json.format[FromCitizenDetails]

    val dontDeleteMe = """
                         |Don't delete me.
                         |I will emit a warning so `@nowarn` can be applied to address below
                         |`Unreachable case except for null` problem emited by Play Json macro"""

    Json.format[MemberSaUtr]
