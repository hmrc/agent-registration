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

import play.api.mvc.QueryStringBindable
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.===

import scala.reflect.ClassTag

object QueryStringBindableFactory:

  inline def makeEnumQueryStringBindable[E <: reflect.Enum](using
    classTag: ClassTag[E]
  ): QueryStringBindable[E] = queryStringBindable(EnumValues.all[E]) // macro enforces scala3 enum at call site

  inline def makeSealedObjectQueryStringBindable[E](using
    classTag: ClassTag[E]
  ): QueryStringBindable[E] = queryStringBindable(SealedObjects.all[E]) // macro enforces E to be SealedObject type at call site

  /** Creates a [[play.api.mvc.QueryStringBindable]] for enums and sealed case objects.
    *
    * This utility method creates a query string binder that:
    *   - Converts query parameters to/from enum values or case objects
    *   - Uses the toString method of type E for string conversion
    *   - Converts camelCase to hyphenated strings (e.g. "SoleTrader" -> "sole-trader")
    *   - Provides type-safe query parameter binding
    *
    * @tparam E
    *   The type to create a QueryStringBindable for
    * @param values
    *   The sequence of valid values that can be bound
    * @return
    *   A QueryStringBindable instance for type E
    */
  private[util] def queryStringBindable[E](values: Seq[E])(using
    classTag: ClassTag[E]
  ): QueryStringBindable[E] =
    new QueryStringBindable[E]:
      val runtimeClass = classTag.runtimeClass

      override def bind(
        key: String,
        params: Map[String, Seq[String]]
      ): Option[Either[String, E]] = params.get(key).flatMap(_.headOption).map { value =>
        values.find(e => HyphenTool.camelCaseToHyphenated(e.toString) === value.toLowerCase) match
          case Some(e) => Right(e)
          case None => Left(s"Could not parse $value as ${runtimeClass.getSimpleName}")
      }

      override def unbind(
        key: String,
        e: E
      ): String = s"$key=${HyphenTool.camelCaseToHyphenated(e.toString)}"
