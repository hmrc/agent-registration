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

package uk.gov.hmrc.agentregistration.shared

import play.api.libs.json.Format
import play.api.mvc.PathBindable
import uk.gov.hmrc.agentregistration.shared.util.EnumBinder
import uk.gov.hmrc.agentregistration.shared.util.EnumFormat

import scala.annotation.nowarn

enum BusinessType:

  case SoleTrader
  case LimitedCompany

  case GeneralPartnership
  extends BusinessType
  with BusinessType.Partnership

  case LimitedLiabilityPartnership
  extends BusinessType
  with BusinessType.Partnership

  case LimitedPartnership
  extends BusinessType
  with BusinessType.Partnership

  case ScottishLimitedPartnership
  extends BusinessType
  with BusinessType.Partnership

  case ScottishPartnership
  extends BusinessType
  with BusinessType.Partnership

object BusinessType:

  /** Marking trait for business types that are partnerships.
    */
  sealed trait Partnership

  val partnershipTypes: Seq[BusinessType] =
    BusinessType.values.toIndexedSeq.collect {
      case p: BusinessType.Partnership => p
    }: @nowarn( /*scala3 bug?*/ "msg=Unreachable case")

  given Format[BusinessType] = EnumFormat.enumFormat[BusinessType]
  given PathBindable[BusinessType] = EnumBinder.pathBindable[BusinessType]
