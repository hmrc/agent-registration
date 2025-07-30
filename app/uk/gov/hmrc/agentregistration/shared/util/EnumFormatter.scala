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

import play.api.data.FormError
import play.api.data.format.Formatter
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.===

import scala.reflect.ClassTag

object EnumFormatter:

  def formatter[E <: reflect.Enum](
    errorMessageIfMissing: String = "error.required",
    errorMessageIfEnumError: String = "invalid input"
  )(using classTag: ClassTag[E]): Formatter[E] =
    val enumClass = classTag.runtimeClass
    // Call the values() method on the companion object to get all enum values
    val valuesMethod = enumClass.getDeclaredMethod("values")
    @SuppressWarnings(Array(
      "org.wartremover.warts.AsInstanceOf",
      "org.wartremover.warts.Null"
    ))
    val enumValues: Array[E] = valuesMethod.invoke(null).asInstanceOf[Array[E]]

    new Formatter[E] {
      override def bind(
        key: String,
        data: Map[String, String]
      ): Either[Seq[FormError], E] = data
        .get(key)
        .toRight(Seq(FormError(
          key,
          errorMessageIfMissing,
          Nil
        )))
        .flatMap { (str: String) =>
          enumValues
            .find(_.toString === str)
            .toRight(Seq(FormError(key, errorMessageIfEnumError)))
        }

      override def unbind(
        key: String,
        value: E
      ): Map[String, String] = Map(key -> value.toString)
    }
