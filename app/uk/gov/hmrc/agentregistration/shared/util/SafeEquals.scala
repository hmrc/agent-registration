/*
 * Copyright 2023 HM Revenue & Customs
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

import scala.annotation.implicitNotFound

// scalafix:off DisableSyntax
object SafeEquals:

  /** Simple safe equals so we don't have to import cats
    *
    * Why use SafeEquals instead of Scala 3's CanEqual?
    *
    *   1. Zero Boilerplate: SafeEquals works immediately for all types in the codebase because it relies on subtyping evidence (<:<) which the compiler
    *      generates automatically. CanEqual requires adding `derives CanEqual` to case classes or defining manual given instances, which is a significant
    *      refactoring effort for existing codebases.
    *   2. Opt-in Usage: SafeEquals allows us to use type-safe equality locally (via ===) without enabling the `strictEquality` language feature globally, which
    *      would break standard `==` usage across the project.
    *   3. Pragmatism: It provides a lightweight, low-friction way to catch equality bugs at compile time without modifying domain models.
    */
  extension [A](v: A)

    @SuppressWarnings(Array("org.wartremover.warts.Equals"))
    inline def ===[B](other: B)(using CanCompare[A, B]): Boolean = v == other
    @SuppressWarnings(Array("org.wartremover.warts.Equals"))
    inline def =!=[B](other: B)(using CanCompare[A, B]): Boolean = v != other

  @implicitNotFound("Comparing unrelated types: ${A} and ${B}.")
  sealed trait CanCompare[A, B]

  object CanCompare
  extends LowPriorityCanCompare:

    @SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf"))
    given subtype[A, B](using A <:< B): CanCompare[A, B] = instance.asInstanceOf[CanCompare[A, B]]

  trait LowPriorityCanCompare:

    protected val instance = new CanCompare[Any, Any] {}
    @SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf"))
    given supertype[A, B](using B <:< A): CanCompare[A, B] = instance.asInstanceOf[CanCompare[A, B]]
