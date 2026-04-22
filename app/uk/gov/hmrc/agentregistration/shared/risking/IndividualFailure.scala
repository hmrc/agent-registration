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

import scala.annotation.nowarn

sealed trait IndividualFailure

object IndividualFailure:

  export IndividualFailureFormats.format

  sealed trait Fixable
  extends IndividualFailure

  sealed trait NonFixable
  extends IndividualFailure

  /** Check 4: Overdue returns */
  object _4:

    /** 4.1: One or more overdue SA returns */
    object _1
    extends IndividualFailure.Fixable:
      override def toString: String = "IndividualFailure.4.1"

    /** 4.3: One or more overdue VAT returns */
    object _3
    extends IndividualFailure.Fixable:
      override def toString: String = "IndividualFailure.4.3"

    /** 4.4: One or more overdue PAYE returns */
    object _4
    extends IndividualFailure.Fixable:
      override def toString: String = "IndividualFailure.4.4"

  /** Check 5: Overdue liabilities */
  object _5:

    /** 5.1: One or more overdue SA liabilities */
    final case class _1(value: Double)
    extends IndividualFailure.Fixable:
      override def toString: String = "IndividualFailure.5.1"

    /** 5.3: One or more overdue VAT liabilities */
    final case class _3(value: Double)
    extends IndividualFailure.Fixable:
      override def toString: String = "IndividualFailure.5.3"

    /** 5.4: One or more overdue PAYE liabilities */
    final case class _4(value: Double)
    extends IndividualFailure.Fixable:
      override def toString: String = "IndividualFailure.5.4"

    /** 5.5: One or more overdue civil penalties */
    final case class _5(value: Double)
    extends IndividualFailure.Fixable:
      override def toString: String = "IndividualFailure.5.5"

    /** 5.6: One or more overdue Stamp Duty liabilities */
    final case class _6(value: Double)
    extends IndividualFailure.Fixable:
      override def toString: String = "IndividualFailure.5.6"

    /** 5.7: One or more overdue Capital Gains Tax liabilities */
    final case class _7(value: Double)
    extends IndividualFailure.Fixable:
      override def toString: String = "IndividualFailure.5.7"

  /** 6: Disqualified as a director on Companies House */
  object _6
  extends IndividualFailure.NonFixable:
    override def toString: String = "IndividualFailure.6"

  /** 7: Insolvent */
  object _7
  extends IndividualFailure.NonFixable:
    override def toString: String = "IndividualFailure.7"

  /** Check 8: Anti-avoidance measures or penalties */
  object _8:

    /** 8.1: Measure - Published Tax Avoidance promoters, enablers and suppliers */
    object _1
    extends IndividualFailure.NonFixable:
      override def toString: String = "IndividualFailure.8.1"

    /** 8.6: Enablers Penalty - within 12 months */
    object _6
    extends IndividualFailure.NonFixable:
      override def toString: String = "IndividualFailure.8.6"

    /** 8.7: Enablers Penalty - more than 12 months, not paid */
    object _7
    extends IndividualFailure.Fixable:
      override def toString: String = "IndividualFailure.8.7"

  /** 9: Relevant criminal convictions */
  object _9
  extends IndividualFailure.NonFixable:
    override def toString: String = "IndividualFailure.9"

  /** Check 10: Cannot verify the individual's information */
  object _10:

    /** 10.1: Unable to match Name and DOB against references */
    object _1
    extends IndividualFailure.Fixable:
      override def toString: String = "IndividualFailure.10.1"

    /** 10.2: Unable to match Name and DOB against references and Missing SA-UTR */
    object _2
    extends IndividualFailure.Fixable:
      override def toString: String = "IndividualFailure.10.2"

object IndividualFailureFormats:

  import play.api.libs.json.*

  private given JsonConfiguration = JsonConfiguration(
    discriminator = "type",
    typeNaming = JsonNaming { fullName =>
      fullName.split('.').filter(_.startsWith("_")).mkString(".")
    }
  )

  @nowarn()
  given format: OFormat[IndividualFailure] =
    // Note: using implicit val instead of given due to Scala compiler bug with given and Play JSON macros
    implicit val _4_1: OFormat[IndividualFailure._4._1.type] = Json.format[IndividualFailure._4._1.type]
    implicit val _4_3: OFormat[IndividualFailure._4._3.type] = Json.format[IndividualFailure._4._3.type]
    implicit val _4_4: OFormat[IndividualFailure._4._4.type] = Json.format[IndividualFailure._4._4.type]
    implicit val _5_1: OFormat[IndividualFailure._5._1] = Json.format[IndividualFailure._5._1]
    implicit val _5_3: OFormat[IndividualFailure._5._3] = Json.format[IndividualFailure._5._3]
    implicit val _5_4: OFormat[IndividualFailure._5._4] = Json.format[IndividualFailure._5._4]
    implicit val _5_5: OFormat[IndividualFailure._5._5] = Json.format[IndividualFailure._5._5]
    implicit val _5_6: OFormat[IndividualFailure._5._6] = Json.format[IndividualFailure._5._6]
    implicit val _5_7: OFormat[IndividualFailure._5._7] = Json.format[IndividualFailure._5._7]
    implicit val _6: OFormat[IndividualFailure._6.type] = Json.format[IndividualFailure._6.type]
    implicit val _7: OFormat[IndividualFailure._7.type] = Json.format[IndividualFailure._7.type]
    implicit val _8_1: OFormat[IndividualFailure._8._1.type] = Json.format[IndividualFailure._8._1.type]
    implicit val _8_6: OFormat[IndividualFailure._8._6.type] = Json.format[IndividualFailure._8._6.type]
    implicit val _8_7: OFormat[IndividualFailure._8._7.type] = Json.format[IndividualFailure._8._7.type]
    implicit val _9: OFormat[IndividualFailure._9.type] = Json.format[IndividualFailure._9.type]
    implicit val _10_1: OFormat[IndividualFailure._10._1.type] = Json.format[IndividualFailure._10._1.type]
    implicit val _10_2: OFormat[IndividualFailure._10._2.type] = Json.format[IndividualFailure._10._2.type]

    val dontDeleteMe = """
                         |Don't delete me.
                         |I will emit a warning so `@nowarn` can be applied to address below
                         |`Unreachable case except for null` problem emited by Play Json macro"""

    Json.format[IndividualFailure]
