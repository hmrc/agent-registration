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

import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.Promise

import uk.gov.hmrc.agentregistration.testsupport.UnitSpec
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.===

@SuppressWarnings(Array("org.wartremover.warts.MutableDataStructures"))
class ProcessInSequenceSpec
extends UnitSpec:

  "processInSequence emptySeq  returns an empty list without calling f" in:
    val callCount = mutable.ListBuffer.empty[Int]
    val result =
      ProcessInSequence.processInSequence(Seq.empty[Int]): item =>
        callCount += item
        Future.successful(item)
    result.futureValue shouldBe List.empty
    callCount shouldBe empty

  "processInSequence singleItem  returns a one-element list with the result of f" in:
    val callCount = mutable.ListBuffer.empty[String]
    val result =
      ProcessInSequence.processInSequence(Seq("a")): item =>
        callCount += item
        Future.successful(s"result-$item")
    result.futureValue shouldBe List("result-a")
    callCount.toList shouldBe List("a")

  "processInSequence multipleItems  does not start the next item until the previous future completes" in:
    val started = mutable.ListBuffer.empty[Int]
    val p0, p1, p2 = Promise[Int]()

    val result =
      ProcessInSequence.processInSequence(Seq(0, 1, 2)): item =>
        started += item
        item match
          case 0 => p0.future
          case 1 => p1.future
          case _ => p2.future

    eventually:
      started.toList shouldBe List(0)

    val _ = p0.success(10)
    eventually:
      started.toList shouldBe List(0, 1)

    val _ = p1.success(20)
    eventually:
      started.toList shouldBe List(0, 1, 2)

    val _ = p2.success(30)

    result.futureValue shouldBe List(10, 20, 30)

  "processInSequence failureHandling  propagates a failure from the first item and does not call f for remaining items" in:
    val callLog = mutable.ListBuffer.empty[Int]
    val boom: RuntimeException = new RuntimeException("boom")

    val result: Future[List[Int]] =
      ProcessInSequence.processInSequence(Seq(0, 1, 2)): item =>
        callLog += item
        if item === 0 then Future.failed(boom) else Future.successful(item)

    result.failed.futureValue shouldBe boom
    callLog.toList shouldBe List(0)

  "processInSequence failureHandling propagates a failure from a middle item and does not call f for subsequent items" in:
    val callLog = mutable.ListBuffer.empty[Int]
    val boom = new RuntimeException("middle boom")

    val result =
      ProcessInSequence.processInSequence(Seq(0, 1, 2)): item =>
        callLog += item
        if item === 1 then Future.failed(boom) else Future.successful(item)

    result.failed.futureValue shouldBe boom
    eventually:
      callLog.toList shouldBe List(0, 1)
    callLog should not contain 2

  "processInSequence failureHandling  propagates a failure from the last item" in:
    val callLog = mutable.ListBuffer.empty[Int]
    val boom = new RuntimeException("last boom")
    val result =
      ProcessInSequence.processInSequence(Seq(0, 1, 2)): item =>
        callLog += item
        if item === 2 then Future.failed(boom) else Future.successful(item)

    result.failed.futureValue shouldBe boom
    callLog.toList shouldBe List(0, 1, 2)

  "processInSequence syncThrow  propagates a synchronous throw from f and does not call f for subsequent items" in:
    val callLog = mutable.ListBuffer.empty[Int]
    val boom = new RuntimeException("sync boom")

    val result =
      ProcessInSequence.processInSequence(Seq(0, 1, 2)): item =>
        callLog += item
        if item === 1 then throw boom else Future.successful(item)

    result.failed.futureValue shouldBe boom
    callLog.toList shouldBe List(0, 1)

  "processAllInSequence emptySeq  returns 0 and does not call f or onFailure" in:
    val callLog = mutable.ListBuffer.empty[Int]
    val failureLog = mutable.ListBuffer.empty[(Throwable, Int)]
    val result =
      ProcessInSequence.processAllInSequence(Seq.empty[Int])(item => { callLog += item; Future.successful(item) })((ex, item) => failureLog += ((ex, item)))

    result.futureValue shouldBe 0
    callLog shouldBe empty
    failureLog shouldBe empty

  "processAllInSequence allSucceed  returns a count equal to the number of items" in:
    val result = ProcessInSequence.processAllInSequence(Seq(1, 2, 3))(item => Future.successful(item))((_, _) => ())

    result.futureValue shouldBe 3

  "processAllInSequence singleFailure  calls onFailure with the correct exception and item, returns 0" in:
    val failureLog = mutable.ListBuffer.empty[(Throwable, Int)]
    val boom = new RuntimeException("boom")
    val result = ProcessInSequence.processAllInSequence(Seq(42))(_ => Future.failed(boom))((ex, item) => failureLog += ((ex, item)))

    result.futureValue shouldBe 0
    failureLog.toList shouldBe List((boom, 42))

  "processAllInSequence someFailures  continues processing after failures and returns count of successes" in:
    val callLog = mutable.ListBuffer.empty[Int]
    val failureLog = mutable.ListBuffer.empty[Int]
    val boom = new RuntimeException("boom")
    val result =
      ProcessInSequence.processAllInSequence(Seq(0, 1, 2, 3, 4))(item =>
        callLog += item
        if item % 2 === 1 then Future.failed(boom) else Future.successful(item)
      )((_, item) => failureLog += item)

    result.futureValue shouldBe 3
    callLog.toList shouldBe List(0, 1, 2, 3, 4)
    failureLog.toList shouldBe List(1, 3)

  "processAllInSequence allFail  calls onFailure for every item and returns 0" in:
    val failureLog = mutable.ListBuffer.empty[Int]
    val boom = new RuntimeException("boom")
    val result = ProcessInSequence.processAllInSequence(Seq(0, 1, 2))(_ => Future.failed(boom))((_, item) => failureLog += item)

    result.futureValue shouldBe 0
    failureLog.toList shouldBe List(0, 1, 2)

  "processAllInSequence syncThrow  treats a synchronous throw in f as a failure and continues with remaining items" in:
    val callLog = mutable.ListBuffer.empty[Int]
    val failureLog = mutable.ListBuffer.empty[(String, Int)]
    val boom = new RuntimeException("sync boom")

    val result =
      ProcessInSequence.processAllInSequence(Seq(0, 1, 2))(item =>
        callLog += item
        if item === 1 then throw boom else Future.successful(item)
      )((ex, item) => failureLog += ((ex.getMessage, item)))

    result.futureValue shouldBe 2
    callLog.toList shouldBe List(0, 1, 2)
    failureLog.toList shouldBe List(("sync boom", 1))

  "processAllInSequence onFailureThrows  stops and fails the batch if onFailure itself throws" in:
    val callLog = mutable.ListBuffer.empty[Int]
    val original = new RuntimeException("original")
    val onFailureBoom = new RuntimeException("onFailure boom")

    val result =
      ProcessInSequence.processAllInSequence(Seq(0, 1, 2))(item =>
        callLog += item
        if item === 1 then Future.failed(original) else Future.successful(item)
      )((_, _) => throw onFailureBoom)

    result.failed.futureValue shouldBe onFailureBoom
    callLog.toList shouldBe List(0, 1)

  "processAllInSequence sequencing  starts items in order even when some fail" in:
    val started = mutable.ListBuffer.empty[Int]
    val p0, p1, p2 = Promise[Int]()
    val boom = new RuntimeException("boom")

    val result =
      ProcessInSequence.processAllInSequence(Seq(0, 1, 2))(item =>
        started += item
        item match
          case 0 => p0.future
          case 1 => p1.future
          case _ => p2.future
      )((_, _) => ())

    eventually:
      started.toList shouldBe List(0)

    val _ = p0.success(10)
    eventually:
      started.toList shouldBe List(0, 1)

    val _ = p1.failure(boom)
    eventually:
      started.toList shouldBe List(0, 1, 2)

    val _ = p2.success(30)

    result.futureValue shouldBe 2
