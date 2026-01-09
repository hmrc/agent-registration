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

import play.api.mvc.PathBindable
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.===

import scala.reflect.ClassTag

object PathBindableFactory:

  inline def pathBindable[E <: reflect.Enum](using classTag: ClassTag[E]): PathBindable[E] = pathBindable(EnumValues.all[E]) // macro enforces scala3 enum at call site

  inline def makeSealedObjectPathBindable[E](using classTag: ClassTag[E]): PathBindable[E] = pathBindable(SealedObjects.all[E])

  /** Creates a [[play.api.mvc.PathBindable]] for type E based on a sequence of values.
    *
    * This utility method creates a path binder that:
    *   - Converts path parameters to/from instances of type E
    *   - Uses the toString method of type E for string conversion
    *   - Converts camelCase to hyphenated strings (e.g. "SoleTrader" -> "sole-trader")
    *   - Provides type-safe path parameter binding for Scala 3 enums and sealed objects
    *
    * @tparam E
    *   The type to create a PathBindable for
    * @param values
    *   The sequence of valid values that can be bound
    * @return
    *   A PathBindable instance for type E
    */
  private[util] def pathBindable[E](values: Seq[E])(using classTag: ClassTag[E]): PathBindable[E] =
    val runtimeClass = classTag.runtimeClass
    new PathBindable[E]:
      override def bind(
        key: String,
        value: String
      ): Either[String, E] = summon[PathBindable[String]]
        .bind(key, value)
        .flatMap { str =>
          values.find((e: E) => HyphenTool.camelCaseToHyphenated(e.toString) === str.toLowerCase) match {
            case Some(enumValue) => Right(enumValue)
            case None => Left(s"Could not parse $str as ${runtimeClass.getSimpleName}")
          }
        }

      override def unbind(
        key: String,
        e: E
      ): String = summon[PathBindable[String]].unbind(key, HyphenTool.camelCaseToHyphenated(e.toString))
