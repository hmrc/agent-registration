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
import uk.gov.hmrc.agentregistration.shared.amls.AmlsDetails
import uk.gov.hmrc.agentregistration.shared.risking.EntityFailure.IsAmls
import uk.gov.hmrc.agentregistration.shared.risking.EntityFailure._3
import uk.gov.hmrc.agentregistration.shared.util.JsonConfig

sealed trait EntityFix

object EntityFix:

  given OFormat[EntityFix] =
    given JsonConfiguration = JsonConfig.jsonConfiguration

    implicit val isAmlsFormat: OFormat[IsAmls] =
      import play.api.libs.json.*
      val reads: Reads[EntityFailure.IsAmls] = EntityFailure.format.flatMap:
        case f: EntityFailure.IsAmls =>
          f match
            case f: EntityFailure._3._1.type => Reads.pure(f)
            case f: EntityFailure._3._2.type => Reads.pure(f)
            case f: EntityFailure._3._3.type => Reads.pure(f)
            case f: EntityFailure._3._4.type => Reads.pure(f)
            case f: EntityFailure._3._5.type => Reads.pure(f)
        case f: EntityFailure.IsNotAmls => Reads(_ => JsError(s"Expected an IsAmls type: $f"))
        case f: EntityFailure.NonFixable => Reads(_ => JsError(s"Expected an IsAmls type: $f"))

      OFormat(
        r = reads,
        w = OWrites[IsAmls] { (f: EntityFailure.IsAmls) => EntityFailure.format.writes(f) }
      )

    implicit val `_3.AmlsFix`: OFormat[_3.AmlsFix] = Json.format[_3.AmlsFix]
    implicit val `_4._1`: OFormat[_4._1] = Json.format[_4._1]
    implicit val `_4._2`: OFormat[_4._2] = Json.format[_4._2]
    implicit val `_4._3`: OFormat[_4._3] = Json.format[_4._3]
    implicit val `_4._4`: OFormat[_4._4] = Json.format[_4._4]
    implicit val `_5._1`: OFormat[_5._1] = Json.format[_5._1]
    implicit val `_5._2`: OFormat[_5._2] = Json.format[_5._2]
    implicit val `_5._3`: OFormat[_5._3] = Json.format[_5._3]
    implicit val `_5._4`: OFormat[_5._4] = Json.format[_5._4]
    implicit val `_5._5`: OFormat[_5._5] = Json.format[_5._5]
    implicit val `_5._6`: OFormat[_5._6] = Json.format[_5._6]
    implicit val `_5._7`: OFormat[_5._7] = Json.format[_5._7]
    implicit val `_8._5`: OFormat[_8._5] = Json.format[_8._5]
    implicit val `_8._7`: OFormat[_8._7] = Json.format[_8._7]
    Json.format[EntityFix]

//  def forFix(f: EntityFailure.Fixable): EntityFix =
//    f match
//      case f: EntityFailure.IsAmls => EntityFix.AmlsFix(f, None, None)
//      case EntityFailure._4._1 => EntityFix._4._1(None)
//      case EntityFailure._4._2 => EntityFix._4._2(None)
//      case EntityFailure._4._3 => EntityFix._4._3(None)
//      case EntityFailure._4._4 => EntityFix._4._4(None)
//      case _: EntityFailure._5._1 => EntityFix._5._1(None)
//      case _: EntityFailure._5._2 => EntityFix._5._2(None)
//      case _: EntityFailure._5._3 => EntityFix._5._3(None)
//      case _: EntityFailure._5._4 => EntityFix._5._4(None)
//      case _: EntityFailure._5._5 => EntityFix._5._5(None)
//      case _: EntityFailure._5._6 => EntityFix._5._6(None)
//      case _: EntityFailure._5._7 => EntityFix._5._7(None)
//      case EntityFailure._8._5 => EntityFix._8._5(None)
//      case EntityFailure._8._7 => EntityFix._8._7(None)

//  def initialFixes(failures: Seq[EntityFailure.Fixable]): Seq[EntityFix] = failures.map(forFix).distinct

  /** @see [[EntityFailure._3]] */
  object _3:

    /** A fix corresponding to any AMLS entity failure [[EntityFailure.IsAmls]]
      *
      * All AMLS failures collapse into this single fix, which additionally requires [[AmlsDetails]] to be supplied by the user.
      */
    final case class AmlsFix(
      failure: EntityFailure.IsAmls,
      isConfirmed: Option[Boolean],
      amlsDetails: Option[AmlsDetails]
    )
    extends EntityFix:
      override def toString = "AmlsFix"

  /** @see [[EntityFailure._4]] */
  object _4:

    /** A fix corresponding to the [[EntityFailure._4._1]] entity failure. */
    final case class _1(isConfirmed: Option[Boolean])
    extends EntityFix

    /** A fix corresponding to the [[EntityFailure._4._2]] entity failure. */
    final case class _2(isConfirmed: Option[Boolean])
    extends EntityFix

    /** A fix corresponding to the [[EntityFailure._4._3]] entity failure. */
    final case class _3(isConfirmed: Option[Boolean])
    extends EntityFix

    /** A fix corresponding to the [[EntityFailure._4._4]] entity failure. */
    final case class _4(isConfirmed: Option[Boolean])
    extends EntityFix

  /** @see [[EntityFailure._5]] */
  object _5:

    /** A fix corresponding to the [[EntityFailure._5._1]] entity failure. */
    final case class _1(isConfirmed: Option[Boolean])
    extends EntityFix

    /** A fix corresponding to the [[EntityFailure._5._2]] entity failure. */
    final case class _2(isConfirmed: Option[Boolean])
    extends EntityFix

    /** A fix corresponding to the [[EntityFailure._5._3]] entity failure. */
    final case class _3(isConfirmed: Option[Boolean])
    extends EntityFix

    /** A fix corresponding to the [[EntityFailure._5._4]] entity failure. */
    final case class _4(isConfirmed: Option[Boolean])
    extends EntityFix

    /** A fix corresponding to the [[EntityFailure._5._5]] entity failure. */
    final case class _5(isConfirmed: Option[Boolean])
    extends EntityFix

    /** A fix corresponding to the [[EntityFailure._5._6]] entity failure. */
    final case class _6(isConfirmed: Option[Boolean])
    extends EntityFix

    /** A fix corresponding to the [[EntityFailure._5._7]] entity failure. */
    final case class _7(isConfirmed: Option[Boolean])
    extends EntityFix

  /** @see [[EntityFailure._8]] */
  object _8:

    /** A fix corresponding to the [[EntityFailure._8._5]] entity failure. */
    final case class _5(isConfirmed: Option[Boolean])
    extends EntityFix

    /** A fix corresponding to the [[EntityFailure._8._7]] entity failure. */
    final case class _7(isConfirmed: Option[Boolean])
    extends EntityFix
