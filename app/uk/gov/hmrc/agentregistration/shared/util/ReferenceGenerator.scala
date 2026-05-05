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

import scala.util.Random

/** Generates a random reference of length 9, consisting of uppercase letters and digits. More detail here: https://confluence.tools.tax.service.gov.uk/x/6YlkSw
  */
object ReferenceGenerator:

  private val validCharacters: IndexedSeq[Char] =
    val allowedLetters = ('A' to 'Z').diff(IndexedSeq(
      'I',
      'O',
      'S',
      'U',
      'V',
      'W'
    ))
    val allowedDigits = ('0' to '9').diff(IndexedSeq('0', '1', '5'))
    allowedLetters ++ allowedDigits

  private val validLength: Int = 9

  private val random: Random = new Random()

  @SuppressWarnings(Array("org.wartremover.warts.SeqApply"))
  private def randomChar(chars: IndexedSeq[Char]): Char =
    val randomIndex = random.nextInt(chars.size)
    chars(randomIndex)

  def generateReference(): String = List.fill(validLength)(randomChar(validCharacters)).mkString("")
