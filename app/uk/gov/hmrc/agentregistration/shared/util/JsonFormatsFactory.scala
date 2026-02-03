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

import play.api.libs.json.*
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.===
import play.api.libs.functional.syntax.*
import play.api.libs.json.Format

import scala.compiletime.erasedValue
import scala.compiletime.error
import scala.deriving.Mirror
import scala.reflect.ClassTag

object JsonFormatsFactory:

  /** Creates a Format for Scala 3 enums by automatically retrieving all enum values.
    */
  inline def makeEnumFormat[E <: reflect.Enum](using ct: ClassTag[E]): Format[E] = makeFormat(EnumValues.all[E])

  /** Creates a Format for Scala 3 enums by automatically retrieving all enum values. Enum values are represented as hyphenated strings, eg. "sole-trader"
    */
  inline def makeEnumFormatHyphenated[E <: reflect.Enum](using ct: ClassTag[E]): Format[E] =
    import uk.gov.hmrc.agentregistration.shared.util.EnumExtensions.toStringHyphenated
    makeFormat(
      EnumValues.all[E],
      stringRepresentation = _.toStringHyphenated
    )

  /** Creates a Format for sealed objects
    */
  inline def makeSealedObjectFormat[E](using ct: ClassTag[E]): Format[E] = makeFormat(SealedObjects.all[E])

  /** Internal utility to create a Format for the type `E`. The formatter uses toString() for serialization and expects all objects in the provided sequence to
    * have unique string representations.
    */
  private def makeFormat[E](
    values: Iterable[E],
    stringRepresentation: E => String = (_: E).toString
  )(using ct: ClassTag[E]): Format[E] =
    val enumName = ct.runtimeClass.getSimpleName
    Format(
      Reads { json =>
        json.validate[String].flatMap { str =>
          values
            .find(stringRepresentation(_) === str)
            .fold[JsResult[E]](JsError(s"Unknown value for enum $enumName: '$str'"))(JsSuccess(_))
        }
      },
      Writes(e => JsString(stringRepresentation(e)))
    )

  /** Compile-time checked derivation of play json Format for final case classes of the form: final case class X(value: String)
    */
  @SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf"))
  inline def makeValueClassFormat[A](using
    m: Mirror.ProductOf[A],
    strFmt: Format[String]
  ): Format[A] =
    // Check the single element type is String
    inline erasedValue[m.MirroredElemTypes] match
      case _: Tuple1[String] =>
        // Check the single element label is "value"
        inline erasedValue[m.MirroredElemLabels] match
          case _: Tuple1["value"] =>
            val base = summon[Format[String]]
            base.inmap[A](
              s => m.fromProduct(Tuple1(s)),
              a => a.asInstanceOf[Product].productElement(0).asInstanceOf[String]
            )
          case _ =>
            error(
              "'JsonFormatsFactory.makeValueClassFormat' can only be used for final case classes with exactly one field of type String,\nie. final case class X(value: String)"
            )
      case _ =>
        error(
          "'JsonFormatsFactory.makeValueClassFormat' can only be used for final case classes with exactly one field of type String,\nie. final case class X(value: String)"
        )

  @SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf"))
  inline def makeIntValueClassFormat[A](using
    m: Mirror.ProductOf[A],
    intFmt: Format[Int]
  ): Format[A] =
    // Check the single element type is String
    inline erasedValue[m.MirroredElemTypes] match
      case _: Tuple1[Int] =>
        // Check the single element label is "value"
        inline erasedValue[m.MirroredElemLabels] match
          case _: Tuple1["value"] =>
            val base = summon[Format[Int]]
            base.inmap[A](
              s => m.fromProduct(Tuple1(s)),
              a => a.asInstanceOf[Product].productElement(0).asInstanceOf[Int]
            )
          case _ =>
            error(
              "'JsonFormatsFactory.makeIntValueClassFormat' can only be used for final case classes with exactly one field of type Int,\nie. final case class X(value: Int)"
            )
      case _ =>
        error(
          "'JsonFormatsFactory.makeIntValueClassFormat' can only be used for final case classes with exactly one field of type Int,\nie. final case class X(value: Int)"
        )
