/*
 * Copyright 2024 HM Revenue & Customs
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

import scala.reflect.ClassTag

/** Utility for creating JSON Format instances for Scala 3 enums */
object EnumFormat:

  /** Creates a Format for Scala 3 enums by automatically retrieving all enum values.
    *
    * @tparam E
    *   The enum type
    * @return
    *   A Format for the enum type
    */
  def enumFormat[E <: reflect.Enum](using ct: ClassTag[E]): Format[E] =
    // Get the enum's companion object
    val enumClass = ct.runtimeClass
    // Call the values() method on the companion object to get all enum values
    val valuesMethod = enumClass.getDeclaredMethod("values")
    @SuppressWarnings(Array(
      "org.wartremover.warts.AsInstanceOf",
      "org.wartremover.warts.Null"
    ))
    val enumValues: Array[E] = valuesMethod.invoke(null).asInstanceOf[Array[E]]

    // Create the Format using the retrieved enum values
    enumFormatWithValues(enumValues)

  /** Creates a Format for Scala 3 enums with explicitly provided enum values.
    *
    * @param enumValues
    *   The enum values to use for serialization/deserialization
    * @tparam E
    *   The enum type
    * @return
    *   A Format for the enum type
    */
  private def enumFormatWithValues[E <: reflect.Enum](enumValues: Iterable[E])(using ct: ClassTag[E]): Format[E] =
    val enumName = ct.runtimeClass.getSimpleName

    Format(
      Reads { json =>
        json.validate[String].flatMap { str =>
          enumValues
            .find(_.toString === str)
            .fold[JsResult[E]](JsError(s"Unknown value for enum $enumName: '$str'"))(JsSuccess(_))
        }
      },
      Writes(e => JsString(e.toString))
    )

  /** Extension method to create a Format for all values of an enum.
    *
    * @tparam E
    *   The enum type
    */
  extension [E <: reflect.Enum](values: Array[E])
    def jsonFormat(using ClassTag[E]): Format[E] = enumFormatWithValues(values)
