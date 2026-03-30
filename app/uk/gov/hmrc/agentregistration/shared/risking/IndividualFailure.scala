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
    extends IndividualFailure.Fixable

    /** 4.3: One or more overdue VAT returns */
    object _3
    extends IndividualFailure.Fixable

    /** 4.4: One or more overdue PAYE returns */
    object _4
    extends IndividualFailure.Fixable

  /** Check 5: Overdue liabilities */
  object _5:

    /** 5.1: One or more overdue SA liabilities */
    final case class _1(value: String)
    extends IndividualFailure.Fixable

    /** 5.3: One or more overdue VAT liabilities */
    final case class _3(value: String)
    extends IndividualFailure.Fixable

    /** 5.4: One or more overdue PAYE liabilities */
    final case class _4(value: String)
    extends IndividualFailure.Fixable

    /** 5.5: One or more overdue civil penalties */
    final case class _5(value: String)
    extends IndividualFailure.Fixable

    /** 5.6: One or more overdue Stamp Duty liabilities */
    final case class _6(value: String)
    extends IndividualFailure.Fixable

    /** 5.7: One or more overdue Capital Gains Tax liabilities */
    final case class _7(value: String)
    extends IndividualFailure.Fixable

  /** 6: Disqualified as a director on Companies House */
  object _6
  extends IndividualFailure.NonFixable

  /** 7: Insolvent */
  object _7
  extends IndividualFailure.NonFixable

  /** Check 8: Anti-avoidance measures or penalties */
  object _8:

    /** 8.1: Measure - Published Tax Avoidance promoters, enablers and suppliers */
    case object _1
    extends IndividualFailure.NonFixable

    /** 8.6: Enablers Penalty - within 12 months */
    case object _6
    extends IndividualFailure.NonFixable

    /** 8.7: Enablers Penalty - more than 12 months, not paid */
    case object _7
    extends IndividualFailure.Fixable

  /** 9: Relevant criminal convictions */
  object _9
  extends IndividualFailure.NonFixable

  /** Check 10: Cannot verify the individual's information */
  object _10:

    /** 10.1: Unable to match Name and DOB against references */
    object _1
    extends IndividualFailure.Fixable

    /** 10.2: Unable to match Name and DOB against references and Missing SA-UTR */
    object _2
    extends IndividualFailure.Fixable

object IndividualFailureFormats:

  import play.api.libs.json.*
  import play.api.libs.functional.syntax.*
  import uk.gov.hmrc.agentregistration.shared.util.JsonConfig

  private given JsonConfiguration = JsonConfig.jsonConfiguration

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

    Json.format[IndividualFailure]
