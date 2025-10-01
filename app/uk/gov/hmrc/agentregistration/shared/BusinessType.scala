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
import uk.gov.hmrc.agentregistration.shared.util.JsonFormatsFactory
import uk.gov.hmrc.agentregistration.shared.util.PathBindableFactory
import uk.gov.hmrc.agentregistration.shared.util.SealedObjects

sealed trait BusinessType

object BusinessType:

  case object SoleTrader
  extends BusinessType

  case object LimitedCompany
  extends BusinessType

  val values: Seq[BusinessType] = SealedObjects.all[BusinessType]

  /** Business Types that are Partnerships.
    */
  sealed trait Partnership
  extends BusinessType

  object Partnership:

    case object GeneralPartnership
    extends BusinessType.Partnership

    case object LimitedLiabilityPartnership
    extends BusinessType.Partnership

    case object LimitedPartnership
    extends BusinessType.Partnership

    case object ScottishLimitedPartnership
    extends BusinessType.Partnership

    case object ScottishPartnership
    extends BusinessType.Partnership

    val values: Seq[Partnership] = SealedObjects.all[Partnership]

  given Format[BusinessType] = JsonFormatsFactory.makeSealedObjectFormat[BusinessType]
  given PathBindable[BusinessType] = PathBindableFactory.makeSealedObjectPathBindable[BusinessType]
  given PathBindable[BusinessType.Partnership] = PathBindableFactory.makeSealedObjectPathBindable[BusinessType.Partnership]
