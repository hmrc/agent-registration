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

import scala.quoted.*

/** */
object SealedObjects:

  /** Collects all singleton objects (case objects or objects) that conform to the queried type `T`. `T` is a SealedObject type, that is a sealed type that all
    * instances are objects.
    *
    * Supported T:
    *   - A sealed trait
    *   - A finite union (|) or intersection (&) of sealed traits.
    *   - A singleton object type (e.g. Foo.type), or unions/intersections that include singletons.
    *
    * Behavior:
    *   - Recursively traverses the sealed hierarchy (via known children) to find all descendant modules.
    *   - Includes modules that extend any sealed subtrait/class in the hierarchy when querying a supertype.
    *   - For unions, returns the union of matches; for intersections, returns only modules satisfying all parts.
    *   - The result is filtered by <:< T to ensure type conformance.
    *
    * Requirements:
    *   - Every non-singleton component of T must be sealed; otherwise, compilation aborts with an error.
    *
    * Ordering:
    *   - Follows declaration order within each sealed branch; global order across unions is not guaranteed.
    *
    * Examples: all[BusinessType] all[Partnership] all[Partnership | SoleTrader.type] all[Foo & Bar]
    */
  inline def all[T]: Seq[T] = ${ allImpl[T] }

  private def allImpl[T: Type](using Quotes): Expr[Seq[T]] =
    import quotes.reflect.*

    val target = TypeRepr.of[T]

    // Gather candidate module symbols for a (sub)type. Aborts if the (sub)type isn't sealed or a singleton.

    @SuppressWarnings(Array("org.wartremover.warts.Recursion"))
    def collectCandidates(tpe: TypeRepr): Set[Symbol] =
      tpe.dealias.simplified match
        case OrType(lhs, rhs) => collectCandidates(lhs) ++ collectCandidates(rhs)

        case AndType(lhs, rhs) =>
          // Intersection: candidates must satisfy both sides.
          val left = collectCandidates(lhs)
          val right = collectCandidates(rhs)
          left intersect right

        case tr if tr.isSingleton =>
          // Singleton type (e.g., X.type). Ensure it’s a module.
          val sym = tr.termSymbol
          if !sym.exists || !sym.flags.is(Flags.Module) then
            report.errorAndAbort(s"Type ${tr.show} must be a singleton object")
          Set(sym)

        case tr =>
          val sym = tr.typeSymbol
          if !sym.exists then
            report.errorAndAbort(s"Type ${tr.show} has no symbol; cannot enumerate children")
          if !sym.flags.is(Flags.Sealed) || !sym.flags.is(Flags.Trait) then
            report.errorAndAbort(s"Type ${tr.show} must be sealed trait; cannot enumerate children")

          // Depth-first through sealed descendants
          def descendants(s: Symbol): List[Symbol] =
            val cs = s.children
            cs.flatMap { c =>
              val rec = if c.flags.is(Flags.Sealed) then descendants(c) else Nil
              c :: rec
            }

          val allSyms: List[Symbol] = (sym :: descendants(sym)).distinct
          // Only modules
          allSyms.filter(_.flags.is(Flags.Module)).toSet

    // Final filter: keep modules whose widened type conforms to the whole target T
    val candidates: Set[Symbol] = collectCandidates(target)

    val typedModules: List[Symbol] = candidates.toList.filter { m =>
      val mt = m.termRef.widen
      mt <:< target
    }

    val exprs: List[Expr[T]] = typedModules.map(m => Ref(m).asExprOf[T])

    Expr.ofList(exprs)
