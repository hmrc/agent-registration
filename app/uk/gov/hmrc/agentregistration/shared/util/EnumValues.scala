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

object EnumValues:

  /** Lists all values of a Scala 3 enum type T. Compiles only if `T` is an enum; otherwise aborts with an error.
    */
  inline def all[T]: Seq[T] = ${ allEnumImpl[T] }

  @SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf"))
  private def allEnumImpl[T: Type](using Quotes): Expr[Seq[T]] =
    import quotes.reflect.*

    val tpe = TypeRepr.of[T].dealias.simplified
    val sym = tpe.typeSymbol

    if !sym.exists || !sym.flags.is(Flags.Enum) || sym.flags.is(Flags.Module) then
      report.errorAndAbort(s"Type ${tpe.show} must be a Scala 3 enum")

    val companion = sym.companionModule
    if !companion.exists then
      report.errorAndAbort(s"Enum ${tpe.show} has no companion module")

    // Call T.values and coerce to Seq[T]
    val valuesSel = Select.unique(Ref(companion), "values")
    '{
      // Scala 3 enums expose a values method returning an array
      val arr = ${ valuesSel.asExpr }.asInstanceOf[Array[Any]]
      arr.iterator.map(_.asInstanceOf[T]).toSeq
    }
