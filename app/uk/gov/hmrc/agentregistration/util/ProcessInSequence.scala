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

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

object ProcessInSequence:

  def processInSequence[Item, Result](items: Seq[Item])(f: Item => Future[Result])(using ExecutionContext): Future[List[Result]] = items
    .foldLeft[Future[List[Result]]](Future.successful(List.empty[Result])):
      (
        acc: Future[List[Result]],
        item
      ) =>
        acc.flatMap(list => f(item).map(_ :: list))
    .map(_.reverse)

  def processAllInSequence[Item, Result](
    items: Seq[Item]
  )(
    f: Item => Future[Result]
  )(
    onFailure: (
      Throwable,
      Item
    ) => Unit
  )(using
    ExecutionContext
  ): Future[Int] = processInSequence(items)(i =>
    f(i).map(_ => true).recover:
      case ex => onFailure(ex, i); false
  ).map(_.count(identity))
