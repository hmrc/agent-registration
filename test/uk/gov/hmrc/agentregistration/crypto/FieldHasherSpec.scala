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

import com.typesafe.config.ConfigFactory
import play.api.Configuration
import uk.gov.hmrc.agentregistration.config.AppConfig
import uk.gov.hmrc.agentregistration.testsupport.UnitSpec
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class FieldHasherSpec
extends UnitSpec:

  "hash" - {

    "prefix the result with hash:" in:
      fh.hash("internal-user-id-12345") should startWith("hash:")

    "be the HMAC-SHA256 of the input under the configured key, base64 encoded" in:
      fh.hash("internal-user-id-12345") shouldBe s"hash:${expectedDigest(hasherKey, "internal-user-id-12345")}"

    "be deterministic" in:
      fh.hash("internal-user-id-12345") shouldBe fh.hash("internal-user-id-12345")

    "be deterministic across instances built from the same key" in:
      fieldHasher(hasherKey).hash("internal-user-id-12345") shouldBe fh.hash("internal-user-id-12345")

    "give different results for different inputs" in:
      fh.hash("internal-user-id-1") should not be fh.hash("internal-user-id-2")

    "be case sensitive" in:
      fh.hash("abc") should not be fh.hash("ABC")

    "give different results for different keys" in:
      fieldHasher(otherHasherKey).hash("internal-user-id-12345") should not be fh.hash("internal-user-id-12345")

    "not contain the plaintext" in:
      fh.hash("internal-user-id-12345") should not include "internal-user-id-12345"

    "hash an empty string without failing" in:
      fh.hash("") shouldBe s"hash:${expectedDigest(hasherKey, "")}"

    "be idempotent" in:
      val once = fh.hash("internal-user-id-12345")
      fh.hash(once) shouldBe once
  }

  "isHashed" - {

    "be true for a hashed value" in:
      fh.isHashed(fh.hash("internal-user-id-12345")) shouldBe true

    "be false for plaintext" in:
      fh.isHashed("internal-user-id-12345") shouldBe false

    "be false for an empty string" in:
      fh.isHashed("") shouldBe false

    "be false when the prefix is not at the start" in:
      fh.isHashed("x-hash:abc") shouldBe false

    "be true for anything starting with the prefix, even if not produced by hash" in:
      fh.isHashed("hash:anything") shouldBe true
  }

  private val hasherKey = "U/g3pVPBa/hIJ109AIwEeuRefPzeqXB312MTw/JTF00="
  private val otherHasherKey = "HIvqb3uQRW8oryUZ3jEQPgMQsvgBSgl71ygWJk6VIdc="

  private def fieldHasher(key: String): FieldHasher =
    val configuration: Configuration = Configuration(ConfigFactory.parseString(
      s"""
         |appName = "agent-registration"
         |microservice.services.des.host = "localhost"
         |microservice.services.des.port = 1234
         |microservice.services.des.protocol = "http"
         |microservice.services.des.environment = "test"
         |microservice.services.des.authorization-token = "test-token"
         |microservice.services.hip.host = "localhost"
         |microservice.services.hip.port = 1234
         |microservice.services.hip.protocol = "http"
         |microservice.services.hip.authorization-token = "test-token"
         |field-level-encryption.enabled = true
         |field-level-encryption.key = "HIvqb3uQRW8oryUZ3jEQPgMQsvgBSgl71ygWJk6VIdc="
         |field-level-encryption.previousKeys = []
         |field-hasher.hasherKey = "$key"
         |post-expiry-grace-period = 45 days
         |""".stripMargin
    ))
    new FieldHasher(new AppConfig(new ServicesConfig(configuration), configuration))

  private val fh: FieldHasher = fieldHasher(hasherKey)

  private def expectedDigest(
    key: String,
    plain: String
  ): String =
    val mac = Mac.getInstance("HmacSHA256")
    mac.init(new SecretKeySpec(Base64.getDecoder.decode(key), "HmacSHA256"))
    Base64.getEncoder.encodeToString(mac.doFinal(plain.getBytes("UTF-8")))
