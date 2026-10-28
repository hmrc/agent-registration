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

package uk.gov.hmrc.agentregistration.util

import play.api.mvc.RequestHeader

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.Failure

object FutureUtil:

  extension [T](f: Future[T])(using
    logger: RequestAwareLogger,
    requestHeader: RequestHeader,
    ec: ExecutionContext
  )
    def andLogOnFailure(message: => String): Future[T] = f.andThen:
      case Failure(exception) => logger.error(message, exception)
