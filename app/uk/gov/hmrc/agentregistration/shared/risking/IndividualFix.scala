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

import play.api.libs.json.Json
import play.api.libs.json.JsonConfiguration
import play.api.libs.json.OFormat
import uk.gov.hmrc.agentregistration.shared.individual.IndividualDateOfBirth
import uk.gov.hmrc.agentregistration.shared.individual.IndividualNino
import uk.gov.hmrc.agentregistration.shared.individual.IndividualSaUtr
import uk.gov.hmrc.agentregistration.shared.util.JsonConfig

import java.time.LocalDate

sealed trait IndividualFix

object IndividualFix:

  given OFormat[IndividualFix] =
    given JsonConfiguration = JsonConfig.jsonConfiguration

    implicit val `_4._1`: OFormat[_4._1] = Json.format[_4._1]
    implicit val `_4._3`: OFormat[_4._3] = Json.format[_4._3]
    implicit val `_4._4`: OFormat[_4._4] = Json.format[_4._4]
    implicit val `_5._1`: OFormat[_5._1] = Json.format[_5._1]
    implicit val `_5._3`: OFormat[_5._3] = Json.format[_5._3]
    implicit val `_5._4`: OFormat[_5._4] = Json.format[_5._4]
    implicit val `_5._5`: OFormat[_5._5] = Json.format[_5._5]
    implicit val `_5._6`: OFormat[_5._6] = Json.format[_5._6]
    implicit val `_5._7`: OFormat[_5._7] = Json.format[_5._7]
    implicit val `_8._7`: OFormat[_8._7] = Json.format[_8._7]

    implicit val `_10.IndividualDetailsFix`: OFormat[_10.IndividualDetailsFix] =
      implicit val individualDateOfBirthFormat: OFormat[IndividualDateOfBirth.Provided] = Json.format[IndividualDateOfBirth.Provided]
      implicit val individualSaUtrFormat: OFormat[IndividualSaUtr.Provided] = Json.format[IndividualSaUtr.Provided]
      implicit val individualNinoFormat: OFormat[IndividualNino.Provided] = Json.format[IndividualNino.Provided]
      Json.format[_10.IndividualDetailsFix]

    Json.format[IndividualFix]

  /** @see [[IndividualFailure._4]] */
  object _4:

    /** A fix corresponding to the [[IndividualFailure._4._1]] individual failure. */
    final case class _1(isConfirmed: Option[Boolean])
    extends IndividualFix

    /** A fix corresponding to the [[IndividualFailure._4._3]] individual failure. */
    final case class _3(isConfirmed: Option[Boolean])
    extends IndividualFix

    /** A fix corresponding to the [[IndividualFailure._4._4]] individual failure. */
    final case class _4(isConfirmed: Option[Boolean])
    extends IndividualFix

  /** @see [[IndividualFailure._5]] */
  object _5:

    /** A fix corresponding to the [[IndividualFailure._5._1]] individual failure. */
    final case class _1(isConfirmed: Option[Boolean])
    extends IndividualFix

    /** A fix corresponding to the [[IndividualFailure._5._3]] individual failure. */
    final case class _3(isConfirmed: Option[Boolean])
    extends IndividualFix

    /** A fix corresponding to the [[IndividualFailure._5._4]] individual failure. */
    final case class _4(isConfirmed: Option[Boolean])
    extends IndividualFix

    /** A fix corresponding to the [[IndividualFailure._5._5]] individual failure. */
    final case class _5(isConfirmed: Option[Boolean])
    extends IndividualFix

    /** A fix corresponding to the [[IndividualFailure._5._6]] individual failure. */
    final case class _6(isConfirmed: Option[Boolean])
    extends IndividualFix

    /** A fix corresponding to the [[IndividualFailure._5._7]] individual failure. */
    final case class _7(isConfirmed: Option[Boolean])
    extends IndividualFix

  /** @see [[IndividualFailure._8]] */
  object _8:

    /** A fix corresponding to the [[IndividualFailure._8._7]] individual failure. */
    final case class _7(isConfirmed: Option[Boolean])
    extends IndividualFix

  /** @see [[IndividualFailure._10]] */
  object _10:
    /** A fix corresponding to the  [[IndividualFailure._10]] individual failures */
    final case class IndividualDetailsFix(
      dateOfBirth: Option[IndividualDateOfBirth.Provided],
      saUtr: Option[IndividualSaUtr.Provided],
      nino: Option[IndividualNino.Provided],
      isConfirmed: Option[Boolean]
    )
    extends IndividualFix
