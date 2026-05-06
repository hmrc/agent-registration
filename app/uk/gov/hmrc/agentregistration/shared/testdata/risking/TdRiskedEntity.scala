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

package uk.gov.hmrc.agentregistration.shared.testdata.risking

import uk.gov.hmrc.agentregistration.shared.risking.RiskedEntity
import uk.gov.hmrc.agentregistration.shared.risking.EntityFailure
import uk.gov.hmrc.agentregistration.shared.testdata.TdBase

trait TdRiskedEntity:
  dependencies: (TdBase) =>

  val riskedEntityApproved: RiskedEntity = RiskedEntity(
    applicationReference = dependencies.applicationReference,
    failures = Nil
  )
  val riskedEntityFailedFixable: RiskedEntity = RiskedEntity(
    applicationReference = dependencies.applicationReference,
    failures = List(
      EntityFailure._4._1, // fixable
      EntityFailure._4._3, // fixable
      EntityFailure._4._4 // fixable
    )
  )

  val riskedEntityFailedNonFixable: RiskedEntity = RiskedEntity(
    applicationReference = dependencies.applicationReference,
    failures = List(
      EntityFailure._4._1, // fixable
      EntityFailure._4._3, // fixable
      EntityFailure._7 // non fixable
    )
  )
