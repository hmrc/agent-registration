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
import play.api.libs.json.Reads

//TODO: move this to the risking project, close to the connector (Marjan)
final case class Failure(
  reasonCode: String,
  reasonDescription: String,
  checkId: String,
  checkDescription: String,
  additionalInfo: Option[AdditionalInfo]
)

object FailureParser:
  // format: off
  def parseIndividualFailure(failure: Failure): IndividualFailure =
    failure match
      case Failure("4.1", _, "4", _, None) => IndividualFailure._4._1
      case Failure("4.3", _, "4", _, None) => IndividualFailure._4._3
      case Failure("4.4", _, "4", _, None) => IndividualFailure._4._4

      case Failure("5.1", _, "5", _, Some(AdditionalInfo(value))) => IndividualFailure._5._1(value)
      case Failure("5.3", _, "5", _, Some(AdditionalInfo(value))) => IndividualFailure._5._3(value)
      case Failure("5.4", _, "5", _, Some(AdditionalInfo(value))) => IndividualFailure._5._4(value)
      case Failure("5.5", _, "5", _, Some(AdditionalInfo(value))) => IndividualFailure._5._5(value)
      case Failure("5.6", _, "5", _, Some(AdditionalInfo(value))) => IndividualFailure._5._6(value)
      case Failure("5.7", _, "5", _, Some(AdditionalInfo(value))) => IndividualFailure._5._7(value)

      case Failure("6", _, "6", _, None) => IndividualFailure._6

      case Failure("7", _, "7", _, None) => IndividualFailure._7

      case Failure("8.1", _, "8", _, None) => IndividualFailure._8._1
      case Failure("8.6", _, "8", _, None) => IndividualFailure._8._6
      case Failure("8.7", _, "8", _, None) => IndividualFailure._8._7

      case Failure("9", _, "9", _, None) => IndividualFailure._9

      case Failure("10.1", _, "10", _, None) => IndividualFailure._10._1
      case Failure("10.2", _, "10", _, None) => IndividualFailure._10._2

      case failure:Failure => throw new IllegalArgumentException(s"Unsupported IndividualFailure $failure")

  def parseEntityFailure(failure: Failure): EntityFailure =
    failure match
      case Failure("3.1", _, "3", _, None) => EntityFailure._3._1
      case Failure("3.2", _, "3", _, None) => EntityFailure._3._2
      case Failure("3.3", _, "3", _, None) => EntityFailure._3._3
      case Failure("3.4", _, "3", _, None) => EntityFailure._3._4
      case Failure("3.5", _, "3", _, None) => EntityFailure._3._5

      case Failure("4.1", _, "4", _, None) => EntityFailure._4._1
      case Failure("4.2", _, "4", _, None) => EntityFailure._4._2
      case Failure("4.3", _, "4", _, None) => EntityFailure._4._3
      case Failure("4.4", _, "4", _, None) => EntityFailure._4._4

      case Failure("5.1", _, "5", _, Some(AdditionalInfo(value))) => EntityFailure._5._1(value)
      case Failure("5.2", _, "5", _, Some(AdditionalInfo(value))) => EntityFailure._5._2(value)
      case Failure("5.3", _, "5", _, Some(AdditionalInfo(value))) => EntityFailure._5._3(value)
      case Failure("5.4", _, "5", _, Some(AdditionalInfo(value))) => EntityFailure._5._4(value)
      case Failure("5.5", _, "5", _, Some(AdditionalInfo(value))) => EntityFailure._5._5(value)
      case Failure("5.6", _, "5", _, Some(AdditionalInfo(value))) => EntityFailure._5._6(value)
      case Failure("5.7", _, "5", _, Some(AdditionalInfo(value))) => EntityFailure._5._7(value)

      case Failure("7", _, "7", _, None) => EntityFailure._7

      case Failure("8.1", _, "8", _, None) => EntityFailure._8._1
      case Failure("8.4", _, "8", _, None) => EntityFailure._8._4
      case Failure("8.5", _, "8", _, None) => EntityFailure._8._5
      case Failure("8.6", _, "8", _, None) => EntityFailure._8._6
      case Failure("8.7", _, "8", _, None) => EntityFailure._8._7

      case failure:Failure => throw new IllegalArgumentException(s"Unsupported EntityFailure $failure")
  // format: on

final case class AdditionalInfo(value: String)

object AdditionalInfo:
  given reads: Reads[AdditionalInfo] = Json.reads[AdditionalInfo]

object Failure:
  given reads: Reads[Failure] = Json.reads[Failure]
