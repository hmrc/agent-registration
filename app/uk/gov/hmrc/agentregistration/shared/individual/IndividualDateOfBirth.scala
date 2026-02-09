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

package uk.gov.hmrc.agentregistration.shared.individual

import play.api.libs.json.*
import uk.gov.hmrc.agentregistration.shared.util.JsonConfig

import java.time.LocalDate
import scala.annotation.nowarn

sealed trait IndividualDateOfBirth

sealed trait UserProvidedDateOfBirth
extends IndividualDateOfBirth

object IndividualDateOfBirth:

  final case class Provided(dateOfBirth: LocalDate)
  extends UserProvidedDateOfBirth

  final case class FromCitizensDetails(dateOfBirth: LocalDate)
  extends IndividualDateOfBirth

  extension (individualDateOfBirth: IndividualDateOfBirth)
    def toUserProvidedDateOfBirth: UserProvidedDateOfBirth =
      individualDateOfBirth match
        case u: UserProvidedDateOfBirth => u
        case _: FromCitizensDetails => throw new IllegalArgumentException(s"Date of birth is already provided from citizens details") // no logging of PII

  @nowarn()
  given OFormat[IndividualDateOfBirth] =
    given JsonConfiguration = JsonConfig.jsonConfiguration
    given OFormat[Provided] = Json.format[Provided]
    given OFormat[FromCitizensDetails] = Json.format[FromCitizensDetails]

    val dontDeleteMe = """
                         |Don't delete me.
                         |I will emit a warning so `@nowarn` can be applied to address below
                         |`Unreachable case except for null` problem emited by Play Json macro"""

    Json.format[IndividualDateOfBirth]
