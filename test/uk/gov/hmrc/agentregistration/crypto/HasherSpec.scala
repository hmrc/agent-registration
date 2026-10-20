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

class HasherSpec
extends UnitSpec:

  "hash is the HMAC-SHA256 of the input under the configured key, base64 encoded, behind the hash: prefix" in:
    hasher.hash("internal-user-id-12345") shouldBe "hash:iRGKF9SU4ftOBe8VJ8i+famfrXWuHB/tS4z4O4K4hUI="

  "hash gives a different result under a different key" in:
    Hasher(otherHasherKey).hash("internal-user-id-12345") shouldBe "hash:Wn2u4Sm9bpY3uKNReSnQ+xrESNEqRNxzsFn8hA8S9DY="

  "hash of an empty string does not fail" in:
    hasher.hash("") shouldBe "hash:8rDAAfMWMnxDZps3mvZjRkCASQHkn+Ebxo1myZQdmZg="

  "hash is deterministic" in:
    hasher.hash("internal-user-id-12345") shouldBe hasher.hash("internal-user-id-12345")

  "hash is deterministic across instances built from the same key" in:
    Hasher(hasherKey).hash("internal-user-id-12345") shouldBe hasher.hash("internal-user-id-12345")

  "hash gives different results for different inputs" in:
    hasher.hash("internal-user-id-1") should not be hasher.hash("internal-user-id-2")

  "hash is case sensitive" in:
    hasher.hash("abc") should not be hasher.hash("ABC")

  "hash does not contain the plaintext" in:
    hasher.hash("internal-user-id-12345") should not include "internal-user-id-12345"

  "hash hashes a value that already looks hashed, rather than passing it through" in:
    val once: String = hasher.hash("internal-user-id-12345")
    hasher.hash(once) should not be once

  private val hasherKey: String = "U/g3pVPBa/hIJ109AIwEeuRefPzeqXB312MTw/JTF00="
  private val otherHasherKey: String = "HIvqb3uQRW8oryUZ3jEQPgMQsvgBSgl71ygWJk6VIdc="

  private val hasher: Hasher = Hasher(hasherKey)
