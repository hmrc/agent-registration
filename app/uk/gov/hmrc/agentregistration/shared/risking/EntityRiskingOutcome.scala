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

enum EntityRiskingOutcome:

  case FailedNonFixable
  case FailedFixable
  case Approved

object EntityRiskingOutcome:

  extension (failures: List[EntityFailure])

    def outcome(): EntityRiskingOutcome =
      def hasNonFixableFailures: Boolean = failures.exists:
        case _: EntityFailure.NonFixable => true
        case _: EntityFailure.Fixable => false

      failures match
        case Nil => EntityRiskingOutcome.Approved
        case _ if hasNonFixableFailures => EntityRiskingOutcome.FailedNonFixable
        case _ => EntityRiskingOutcome.FailedFixable

    def exampleFrontendFunctionality_renderHtml(failure: Seq[EntityFailure]): String =
      val fixableBullets = failure.collect:
        case f: EntityFailure.Fixable => f
        // now we have Seq[EntityFailure.Fixable] !
      .map:
        case EntityFailure._3._1 => "HMRC's can't find AMLS number, please resubmit..."
        case EntityFailure._3._2 => "Professional body can't find your AMLS registration number, please check and resubmit"
        case EntityFailure._3._3 => "No proof or evidence of AMLS coverage uploaded, please upload the required file"
        case EntityFailure._3._4 => "Professional body is not on the approved list, please select an approved body"
        case EntityFailure._3._5 => "Student membership is not acceptable, please provide full membership details"
        case EntityFailure._4._1 => "You have overdue SA returns, please submit them"
        case EntityFailure._4._2 => "You have overdue CoTax returns, please submit them"
        case EntityFailure._4._3 => "You have overdue VAT returns, please submit them"
        case EntityFailure._4._4 => "You have overdue PAYE returns, please submit them"
        case EntityFailure._5._1(value) => s"You have overdue SA liabilities of $value, please pay them"
        case EntityFailure._5._2(value) => s"You have overdue CoTax liabilities of $value, please pay them"
        case EntityFailure._5._3(value) => s"You have overdue VAT liabilities of $value, please pay them"
        case EntityFailure._5._4(value) => s"You have overdue PAYE liabilities of $value, please pay them"
        case EntityFailure._5._5(value) => s"You have overdue civil penalties of $value, please pay them"
        case EntityFailure._5._6(value) => s"You have overdue Stamp Duty liabilities of $value, please pay them"
        case EntityFailure._5._7(value) => s"You have overdue Capital Gains Tax liabilities of $value, please pay them"
        case EntityFailure._8._5 => "You have a POTAS penalty from more than 12 months ago that is not paid, please pay it"
        case EntityFailure._8._7 => "You have an Enablers Penalty from more than 12 months ago that is not paid, please pay it"

      val nonFixableBullets = failure.collect:
        case f: EntityFailure.NonFixable => f
        // now we have Seq[EntityFailure.NonFixable] !
      .map:
        case EntityFailure._7 => "Sorry, insolvents can't be registered"
        case EntityFailure._8._1 => "Sorry, Published Tax Avoidance promoters can't be registered"
        case EntityFailure._8._4 => "Sorry, you have a POTAS penalty within the last 12 months and can't be registered"
        case EntityFailure._8._6 => "Sorry, you have an Enablers Penalty within the last 12 months and can't be registered"

      s""" Sorry, there are some issues with your application:
         |$fixableBullets
         |$nonFixableBullets
         |""".stripMargin
