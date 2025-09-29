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

package uk.gov.hmrc.agentregistration.shared.util

import uk.gov.hmrc.agentregistration.testsupport.UnitSpec

class SealedObjectsSpec
extends UnitSpec:

  "SealedObjects.all should return all Sealed Objects" in:

    SealedObjects.all[Bt].toSet shouldBe Set(
      St,
      Lc,
      Gp,
      Lp,
      Slp,
      Sp
    ) withClue "it should select all Bt objects"

    SealedObjects.all[PShip].toSet shouldBe Set(
      Gp,
      Lp,
      Slp,
      Sp
    ) withClue "it should select all Ps objects, which are subtype of Bt"

    SealedObjects.all[PShip | St.type].toSet shouldBe Set(
      Gp,
      Lp,
      Slp,
      Sp,
      St
    ) withClue "union types supported"

    SealedObjects.all[St.type & OneP].toSet shouldBe Set(St) withClue "intersection types supported"

    SealedObjects.all[PShip & OneP].toSet shouldBe Set() withClue "intersection selects no objects"

    sealed trait T

    case object T1
    extends T
    case object T2
    extends T

    SealedObjects.all[T] shouldBe Seq(T1, T2) withClue "it should support traits defined inside a class"

sealed trait Bt

sealed trait OneP
extends Bt

sealed trait PShip { self: Bt => }

case object St
extends Bt,
  OneP

case object Lc
extends Bt

case object Gp
extends Bt,
  PShip

object Lp
extends Bt,
  PShip

case object Slp
extends Bt,
  PShip

case object Sp
extends Bt,
  PShip
