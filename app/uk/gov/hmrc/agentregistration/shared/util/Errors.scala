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

import play.api.mvc.RequestHeader
import uk.gov.hmrc.http.InternalServerException

import scala.concurrent.Future

object Errors:

  extension [T](t: Option[T])

    /** Extracts the value from an Option when we know it should be populated at this point in the user journey.
      *
      * This method should be used on optional fields when we have confidence that they are populated, for example on Check Your Answers pages, where we know
      * that the user has provided answers and thus populated options with values.
      */
    inline def getOrThrowExpectedDataMissing(
      message: => String
    ): T = t.getOrElse(throw new IllegalStateException(s"Expected data was missing: $message"))

  /** Creates a requirement which has to pass to continue computation.
    */
  inline def require(
    requirement: Boolean,
    message: => String
  )(using request: RequestHeader): Unit =
    if !requirement then
      throw InternalServerException(message)
    else ()

  def requireF(
    requirement: Boolean,
    message: => String
  )(using request: RequestHeader): Future[Unit] =
    if !requirement then
      Future.failed(InternalServerException(message))
    else Future.successful(())
