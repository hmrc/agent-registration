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

package uk.gov.hmrc.agentregistration.crypto

import uk.gov.hmrc.agentregistration.testsupport.UnitSpec

class FieldLevelEncryptionSpec
extends UnitSpec:

  "encrypt is deterministic, so an encrypted field can still be matched in a filter" in:
    fieldLevelEncryption.encrypt("alice@example.com") shouldBe fieldLevelEncryption.encrypt("alice@example.com")

  "encrypt gives something other than the plaintext" in:
    fieldLevelEncryption.encrypt("alice@example.com") should not be "alice@example.com"

  "decrypt undoes encrypt" in:
    fieldLevelEncryption.decrypt(fieldLevelEncryption.encrypt("alice@example.com")) shouldBe "alice@example.com"

  "decrypt reads a value encrypted under a previous key" in:
    val beforeRotation: FieldLevelEncryption = FieldLevelEncryption(
      enabled = true,
      key = previousKey,
      previousKeys = Seq.empty
    )
    val afterRotation: FieldLevelEncryption = FieldLevelEncryption(
      enabled = true,
      key = key,
      previousKeys = Seq(previousKey)
    )

    afterRotation.decrypt(beforeRotation.encrypt("alice@example.com")) shouldBe "alice@example.com"

  "encrypt is identity when encryption is off" in:
    fieldLevelEncryptionDisabled.encrypt("alice@example.com") shouldBe "alice@example.com"

  "decrypt is identity when encryption is off" in:
    fieldLevelEncryptionDisabled.decrypt("alice@example.com") shouldBe "alice@example.com"

  private val key: String = "HIvqb3uQRW8oryUZ3jEQPgMQsvgBSgl71ygWJk6VIdc="
  private val previousKey: String = "U/g3pVPBa/hIJ109AIwEeuRefPzeqXB312MTw/JTF00="

  private val fieldLevelEncryption: FieldLevelEncryption = FieldLevelEncryption(
    enabled = true,
    key = key,
    previousKeys = Seq.empty
  )

  private val fieldLevelEncryptionDisabled: FieldLevelEncryption = FieldLevelEncryption(
    enabled = false,
    key = key,
    previousKeys = Seq.empty
  )
