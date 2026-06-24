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

package uk.gov.hmrc.agentregistration.shared.util

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

object ProcessInSequence:

  /** Processes a sequence of items sequentially, applying an asynchronous function to each item in order.
    *
    * This method ensures that the async function `f` is applied to each item one at a time, waiting for each Future to complete before processing the next
    * item. This is useful when order matters or when you need to limit concurrency to avoid overwhelming downstream services.
    */
  def processInSequence[Item, Result](items: Seq[Item])(f: Item => Future[Result])(using ExecutionContext): Future[List[Result]] = items
    .foldLeft[Future[List[Result]]](Future.successful(List.empty[Result])):
      (
        acc: Future[List[Result]],
        item
      ) =>
        acc.flatMap(list => f(item).map(_ :: list))
    .map(_.reverse)

  /** Processes a sequence of items sequentially, applying an asynchronous function to each item in order, without stopping on failures.
    *
    * Unlike `processInSequence`, this method does not stop processing when an item fails. Instead, it continues processing all items in the sequence. When an
    * item's processing fails, the `onFailure` hook is called with the exception and the failed item, allowing for custom error handling (e.g., logging).
    *
    * This is particularly useful when you need to process a batch of items and want to ensure all items are attempted, even if some fail, while still being
    * notified of failures.
    *
    * @param items
    *   The sequence of items to process.
    * @param f
    *   The asynchronous function to apply to each item. Failures are caught and handled via the onFailure hook.
    * @param onFailure
    *   A callback function invoked when processing of an item fails. It receives the exception and the item that failed.
    * @return
    *   A Future containing the count of successfully processed items.
    */
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
