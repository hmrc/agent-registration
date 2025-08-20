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
import play.api.mvc.QueryStringBindable
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.===

import scala.reflect.ClassTag

object EnumBinder:

  def pathBindable[E <: reflect.Enum](using classTag: ClassTag[E]): PathBindable[E] =
    val enumClass = classTag.runtimeClass
    // Call the values() method on the companion object to get all enum values
    val valuesMethod = enumClass.getDeclaredMethod("values")
    @SuppressWarnings(Array(
      "org.wartremover.warts.AsInstanceOf",
      "org.wartremover.warts.Null"
    ))
    val enumValues: Array[E] = valuesMethod.invoke(null).asInstanceOf[Array[E]]

    new PathBindable[E]:
      override def bind(
        key: String,
        value: String
      ): Either[String, E] = summon[PathBindable[String]]
        .bind(key, value)
        .flatMap { str =>
          enumValues.find((e: E) => HyphenTool.camelCaseToHyphenated(e.toString) === str.toLowerCase) match {
            case Some(enumValue) => Right(enumValue)
            case None => Left(s"Could not parse $str as ${enumClass.getSimpleName}")
          }
        }

      override def unbind(
        key: String,
        e: E
      ): String = summon[PathBindable[String]].unbind(key, HyphenTool.camelCaseToHyphenated(e.toString))

  def queryStringEnumBinder[E <: reflect.Enum](using
    classTag: ClassTag[E]
  ): QueryStringBindable[E] =
    val enumClass = classTag.runtimeClass
    // Call the values() method on the companion object to get all enum values
    val valuesMethod = enumClass.getDeclaredMethod("values")
    @SuppressWarnings(Array(
      "org.wartremover.warts.AsInstanceOf",
      "org.wartremover.warts.Null"
    ))
    val eenums: Array[E] = valuesMethod.invoke(null).asInstanceOf[Array[E]]

    new QueryStringBindable[E]:
      override def bind(
        key: String,
        params: Map[String, Seq[String]]
      ): Option[Either[String, E]] = params.get(key).flatMap(_.headOption).map { value =>
        eenums.find(e => HyphenTool.camelCaseToHyphenated(e.toString) === value.toLowerCase) match
          case Some(eenum) => Right(eenum)
          case None => Left(s"Could not parse $value as ${enumClass.getSimpleName}")
      }

      override def unbind(
        key: String,
        eenum: E
      ): String = s"$key=${HyphenTool.camelCaseToHyphenated(eenum.toString)}"
