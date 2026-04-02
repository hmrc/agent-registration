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

sealed trait EntityFailure

object EntityFailure:

  export EntityFailureFormats.format

  sealed trait Fixable
  extends EntityFailure

  sealed trait NonFixable
  extends EntityFailure

  /** Check 3: AMLS */
  object _3:

    /** 3.1: Entity claims AMLS with HMRC but their registration number cannot be found in HMRC's AMLS register */
    object _1
    extends EntityFailure.Fixable

    /** 3.2: Entity claims AMLS with a professional body but their registration number cannot be found in that professional body's AMLS register */
    object _2
    extends EntityFailure.Fixable

    /** 3.3: No proof or evidence of AMLS coverage (file upload) */
    object _3
    extends EntityFailure.Fixable

    /** 3.4: Professional body not on approved list */
    object _4
    extends EntityFailure.Fixable

    /** 3.5: Student membership */
    object _5
    extends EntityFailure.Fixable

  /** Check 4: Overdue returns */
  object _4:

    /** 4.1: One or more overdue SA returns */
    object _1
    extends EntityFailure.Fixable

    /** 4.2: One or more overdue CoTax returns */
    object _2
    extends EntityFailure.Fixable

    /** 4.3: One or more overdue VAT returns */
    object _3
    extends EntityFailure.Fixable

    /** 4.4: One or more overdue PAYE returns */
    object _4
    extends EntityFailure.Fixable

  /** Check 5: Overdue liabilities */
  object _5:

    /** 5.1: One or more overdue SA liabilities */
    final case class _1(value: Double)
    extends EntityFailure.Fixable

    /** 5.2: One or more overdue CoTax liabilities */
    final case class _2(value: Double)
    extends EntityFailure.Fixable

    /** 5.3: One or more overdue VAT liabilities */
    final case class _3(value: Double)
    extends EntityFailure.Fixable

    /** 5.4: One or more overdue PAYE liabilities */
    final case class _4(value: Double)
    extends EntityFailure.Fixable

    /** 5.5: One or more overdue civil penalties */
    final case class _5(value: Double)
    extends EntityFailure.Fixable

    /** 5.6: One or more overdue Stamp Duty liabilities */
    final case class _6(value: Double)
    extends EntityFailure.Fixable

    /** 5.7: One or more overdue Capital Gains Tax liabilities */
    final case class _7(value: Double)
    extends EntityFailure.Fixable

  /** 7: Insolvent */
  case object _7
  extends EntityFailure.NonFixable

  /** Check 8: Anti-avoidance measures or penalties */
  object _8:

    /** 8.1: Measure - Published Tax Avoidance promoters, enablers and suppliers */
    case object _1
    extends EntityFailure.NonFixable

    /** 8.4: POTAS penalty - within 12 months */
    case object _4
    extends EntityFailure.NonFixable

    /** 8.5: POTAS penalty - more than 12 months, not paid */
    case object _5
    extends EntityFailure.Fixable

    /** 8.6: Enablers Penalty - within 12 months */
    case object _6
    extends EntityFailure.NonFixable

    /** 8.7: Enablers Penalty - more than 12 months, not paid */
    case object _7
    extends EntityFailure.Fixable

object EntityFailureFormats:

  import play.api.libs.json.*
  import uk.gov.hmrc.agentregistration.shared.util.JsonConfig

  private given JsonConfiguration = JsonConfig.jsonConfiguration

  given format: OFormat[EntityFailure] =
    // Note: using implicit val instead of given due to Scala compiler bug with given and Play JSON macros
    implicit val _3_1: OFormat[EntityFailure._3._1.type] = Json.format[EntityFailure._3._1.type]
    implicit val _3_2: OFormat[EntityFailure._3._2.type] = Json.format[EntityFailure._3._2.type]
    implicit val _3_3: OFormat[EntityFailure._3._3.type] = Json.format[EntityFailure._3._3.type]
    implicit val _3_4: OFormat[EntityFailure._3._4.type] = Json.format[EntityFailure._3._4.type]
    implicit val _3_5: OFormat[EntityFailure._3._5.type] = Json.format[EntityFailure._3._5.type]
    implicit val _4_1: OFormat[EntityFailure._4._1.type] = Json.format[EntityFailure._4._1.type]
    implicit val _4_2: OFormat[EntityFailure._4._2.type] = Json.format[EntityFailure._4._2.type]
    implicit val _4_3: OFormat[EntityFailure._4._3.type] = Json.format[EntityFailure._4._3.type]
    implicit val _4_4: OFormat[EntityFailure._4._4.type] = Json.format[EntityFailure._4._4.type]
    implicit val _5_1: OFormat[EntityFailure._5._1] = Json.format[EntityFailure._5._1]
    implicit val _5_2: OFormat[EntityFailure._5._2] = Json.format[EntityFailure._5._2]
    implicit val _5_3: OFormat[EntityFailure._5._3] = Json.format[EntityFailure._5._3]
    implicit val _5_4: OFormat[EntityFailure._5._4] = Json.format[EntityFailure._5._4]
    implicit val _5_5: OFormat[EntityFailure._5._5] = Json.format[EntityFailure._5._5]
    implicit val _5_6: OFormat[EntityFailure._5._6] = Json.format[EntityFailure._5._6]
    implicit val _5_7: OFormat[EntityFailure._5._7] = Json.format[EntityFailure._5._7]
    implicit val _7: OFormat[EntityFailure._7.type] = Json.format[EntityFailure._7.type]
    implicit val _8_1: OFormat[EntityFailure._8._1.type] = Json.format[EntityFailure._8._1.type]
    implicit val _8_4: OFormat[EntityFailure._8._4.type] = Json.format[EntityFailure._8._4.type]
    implicit val _8_5: OFormat[EntityFailure._8._5.type] = Json.format[EntityFailure._8._5.type]
    implicit val _8_6: OFormat[EntityFailure._8._6.type] = Json.format[EntityFailure._8._6.type]
    implicit val _8_7: OFormat[EntityFailure._8._7.type] = Json.format[EntityFailure._8._7.type]

    Json.format[EntityFailure]
