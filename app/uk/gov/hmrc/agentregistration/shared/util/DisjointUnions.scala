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

package uk.gov.hmrc.agentregistration.shared.util

import scala.annotation.unused
import scala.quoted.*

object DisjointUnions:

  /** Proves at compile-time that two union types `L` and `R` form a disjoint and exhaustive partition of a sealed type `Parent`.
    *
    * This macro verifies three properties:
    *   1. '''Exhaustiveness''': Every direct subtype of `Parent` is covered by either `L` or `R`
    *      2. '''Disjointness''': No subtype appears in both `L` and `R`
    *      3. '''Validity''': All types in `L` and `R` are actually subtypes of `Parent` (no alien types)
    *
    * If any of these properties are violated, compilation fails with a descriptive error message indicating exactly which types are problematic and how to fix
    * them.
    *
    * ==Usage==
    * {{{
    * sealed trait Animal
    * final case class Dog(name: String) extends Animal
    * final case class Cat(name: String) extends Animal
    * final case class Fish(name: String) extends Animal
    * final case class Bird(name: String) extends Animal
    *
    * // Define two disjoint union types
    * type Mammals = Dog | Cat
    * type NonMammals = Fish | Bird
    *
    * // Prove they partition Animal - compiles successfully!
    * DisjointUnions.prove[Animal, Mammals, NonMammals]
    * }}}
    *
    * ==Error Examples==
    *
    * '''Overlap''' - a type appears in both unions:
    * {{{
    * type Mammals = Dog | Cat
    * type NonMammals = Fish | Bird | Cat // Cat appears in both which is a BUG!
    *
    * DisjointUnions.prove[Animal, Mammals, NonMammals]
    * // Compile error: OVERLAP: 'Cat' appears in both 'Mammals' and 'NonMammals'. Remove it from one of them.
    * }}}
    *
    * '''Missing''' - a subtype is not covered:
    * {{{
    * type Mammals = Dog | Cat
    * type NonMammals = Fish  // Bird is missing which is a BUG!
    *
    * DisjointUnions.prove[Animal, Mammals, NonMammals]
    * // Compile error: MISSING: 'Bird' is not covered by either 'Mammals' or 'NonMammals'. Add it to exactly one.
    * }}}
    *
    * '''Alien''' - a non-subtype sneaks into a union:
    * {{{
    * final case class Robot(id: Int)  // Not an Animal, which is a BUG!
    *
    * type Mammals = Dog | Cat
    * type NonMammals = Fish | Bird | Robot
    *
    * DisjointUnions.prove[Animal, Mammals, NonMammals]
    * // Compile error: ALIEN: 'Robot' in 'NonMammals' is not a subtype of 'Animal'. Remove it from the union.
    * }}}
    *
    * ==Why Use This?==
    *
    * When working with sealed hierarchies, you often want to group subtypes for different handling (e.g., "incorporated" vs "non-incorporated" business types).
    * This macro ensures:
    *   - You don't accidentally put a type in both groups
    *   - You don't forget to classify a new subtype when it's added
    *   - You don't accidentally include unrelated types
    *
    * Place the `prove` call near your type definitions to catch errors early during compilation.
    *
    * @tparam Parent
    *   the sealed parent type whose subtypes should be partitioned
    * @tparam L
    *   the "left" union type (first partition)
    * @tparam R
    *   the "right" union type (second partition)
    */
  inline def prove[Parent, L, R]: Unit = ${ proveImpl[Parent, L, R] }

  @SuppressWarnings(Array("org.wartremover.warts.MutableDataStructures", "org.wartremover.warts.Recursion"))
  private def proveImpl[
    Parent: Type,
    L: Type,
    R: Type
  ](using Quotes): Expr[Unit] =
    import quotes.reflect.*

    val parentRepr = TypeRepr.of[Parent]
    val leftRepr = TypeRepr.of[L]
    val rightRepr = TypeRepr.of[R]

    // Get the symbol and check it's a sealed trait/class
    val parentSymbol = parentRepr.typeSymbol
    if !parentSymbol.flags.is(Flags.Sealed) then
      report.errorAndAbort(s"${Type.show[Parent]} must be a sealed trait or class")

    // Get all direct children
    val children = parentSymbol.children

    if children.isEmpty then
      report.errorAndAbort(s"${Type.show[Parent]} has no subtypes")

    // Helper to extract all types from a union type (A | B | C) => List(A, B, C)
    def extractUnionMembers(repr: TypeRepr): List[TypeRepr] =
      repr.dealias match
        case OrType(left, right) => extractUnionMembers(left) ++ extractUnionMembers(right)
        case AndType(left, right) => extractUnionMembers(left) // For (A | B) & Parent, extract from left
        case other => List(other)

    val leftMembers = extractUnionMembers(leftRepr)
    val rightMembers = extractUnionMembers(rightRepr)

    val errors = scala.collection.mutable.ListBuffer[String]()

    // Check that all union members are subtypes of Parent
    def checkMembersAreSubtypes(
      members: List[TypeRepr],
      unionName: String
    ): Unit =
      for member <- members do
        if !(member <:< parentRepr) then
          val memberName = member.typeSymbol.name
          val parentName = parentSymbol.name
          errors += s"ALIEN: '$memberName' in '$unionName' is not a subtype of '$parentName'. Remove it from the union or make it subtype of '$parentName'."

    val leftName = leftRepr.typeSymbol.name
    val rightName = rightRepr.typeSymbol.name

    checkMembersAreSubtypes(leftMembers, leftName)
    checkMembersAreSubtypes(rightMembers, rightName)

    // Check each child of Parent
    for child <- children do
      val childRepr: TypeRef = child.typeRef
      val childSymbolName: String = child.name

      val isInLeft = childRepr <:< leftRepr
      val isInRight = childRepr <:< rightRepr

      (isInLeft, isInRight) match
        case (true, true) => errors += s"OVERLAP: '$childSymbolName' appears in both '$leftName' and '$rightName'. Remove it from one of them.\n"
        case (false, false) => errors += s"MISSING: '$childSymbolName' is not covered by either '$leftName' or '$rightName'. Add it to exactly one.\n"
        case _ => // OK - in exactly one
    if errors.nonEmpty then
      val parentName = Type.show[Parent]
      val leftTypeName = Type.show[L]
      val rightTypeName = Type.show[R]

      val header =
        s"""Disjoint union proof failed!
           |
           |Parent type: $parentName
           |Union '$leftName': $leftTypeName
           |Union '$rightName': $rightTypeName
           |
           |Each subtype of '${parentSymbol.name}' must be in exactly one union, and all union members must be subtypes of '${parentSymbol.name}'.
           |
           |Problems found:""".stripMargin

      report.errorAndAbort(errors.mkString(
        s"$header\n  • ",
        "\n  • ",
        "\n"
      ))

    '{ () }

  @SuppressWarnings(Array("org.wartremover.warts.MutableDataStructures"))
  @unused
  private def proveImplOld[
    Parent: Type,
    L: Type,
    R: Type
  ](using Quotes): Expr[Unit] =
    import quotes.reflect.*

    val parentRepr = TypeRepr.of[Parent]
    val leftRepr = TypeRepr.of[L]
    val rightRepr = TypeRepr.of[R]

    // Get the symbol and check it's a sealed trait/class
    val parentSymbol = parentRepr.typeSymbol
    if !parentSymbol.flags.is(Flags.Sealed) then
      report.errorAndAbort(s"${parentSymbol.name} must be a sealed trait or class")

    // Get all direct children
    val children = parentSymbol.children

    if children.isEmpty then
      report.errorAndAbort(s"${parentSymbol.name} has no subtypes")

    val errors = scala.collection.mutable.ListBuffer[String]()

    for child <- children do
      val childRepr: TypeRef = child.typeRef
      val childSymbolName: String = child.name
      val leftSymbolName: String = leftRepr.typeSymbol.name
      val rightSymbolName: String = rightRepr.typeSymbol.name

      val isInLeft = childRepr <:< leftRepr
      val isInRight = childRepr <:< rightRepr

      (isInLeft, isInRight) match
        case (true, true) => errors += s"OVERLAP: '$childSymbolName' is in BOTH '$leftSymbolName' and '$rightSymbolName' unions - remove it from one"
        case (false, false) => errors += s"MISSING: '$childSymbolName' is in NEITHER '$leftSymbolName' nor '$rightSymbolName' union - add it to one"
        case _ => // OK - in exactly one
    if errors.nonEmpty then
      val parentName = Type.show[Parent]
      val leftTypeName = Type.show[L]
      val rightTypeName = Type.show[R]
      val leftName = leftRepr.typeSymbol.name
      val rightName = rightRepr.typeSymbol.name

      val header =
        s"""Disjoint union proof failed!
           |
           |Parent type: $parentName
           |Union '$leftName': $leftTypeName
           |Union '$rightName': $rightTypeName
           |
           |Each subtype of '$parentName' must be in exactly one union (either '$leftName' or '$rightName', but not both).
           |
           |Problems found:""".stripMargin

      report.errorAndAbort(errors.mkString(
        s"$header\n  ",
        "\n  ",
        ""
      ))

    '{ () }
