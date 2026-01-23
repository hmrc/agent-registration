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

package uk.gov.hmrc.agentregistration.shared.lists

import play.api.libs.json.Format
import play.api.libs.json.Json
import play.api.libs.json.JsonConfiguration
import uk.gov.hmrc.agentregistration.shared.util.JsonConfig

sealed trait RequiredKeyIndividuals:

  def isValidNumberToProvideDetails: Boolean

final case class FromFiveOrFewer(
  numberToProvideDetails: Int, // in this context this is the total number of key individuals (e.g. partners) that exist
  source: KeyIndividualListSource
)
extends RequiredKeyIndividuals:

  override def isValidNumberToProvideDetails: Boolean = numberToProvideDetails <= 5 && numberToProvideDetails >= 1

final case class FromSixOrMore(
  numberToProvideDetails: Int, // in this context this is a subset of the 6 or more, those who are responsible for tax advice
  source: KeyIndividualListSource
)
extends RequiredKeyIndividuals:

  override def isValidNumberToProvideDetails: Boolean = numberToProvideDetails >= 1 && numberToProvideDetails <= 30

object RequiredKeyIndividuals:

  given Format[RequiredKeyIndividuals] =
    given Format[FromFiveOrFewer] = Json.format[FromFiveOrFewer]
    given Format[FromSixOrMore] = Json.format[FromSixOrMore]

    given JsonConfiguration = JsonConfig.jsonConfiguration

    Json.format[RequiredKeyIndividuals]
