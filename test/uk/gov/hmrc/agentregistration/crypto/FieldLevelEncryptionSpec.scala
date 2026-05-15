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

package uk.gov.hmrc.agentregistration.crypto

import com.typesafe.config.ConfigFactory
import play.api.Configuration
import uk.gov.hmrc.agentregistration.config.AppConfig
import uk.gov.hmrc.agentregistration.testsupport.UnitSpec
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

class FieldLevelEncryptionSpec
extends UnitSpec:

  private val baseConfigString: String =
    """
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
      |microservice.services.hip.system-id = "agent-registration"
      |field-level-encryption.key = "HIvqb3uQRW8oryUZ3jEQPgMQsvgBSgl71ygWJk6VIdc="
      |field-level-encryption.previousKeys = []
      |""".stripMargin

  private def fleWith(enabled: Boolean): FieldLevelEncryption = {
    val configuration: Configuration = Configuration(ConfigFactory.parseString(
      s"$baseConfigString\nfield-level-encryption.enabled = $enabled"
    ))
    val appConfig: AppConfig = new AppConfig(new ServicesConfig(configuration), configuration)
    new FieldLevelEncryption(appConfig)
  }

  "FieldLevelEncryption with enabled = true" - {
    val fle = fleWith(enabled = true)

    "encrypts deterministically — same plaintext always produces the same ciphertext (required for indexed/searchable fields)" in:
      fle.encrypt("alice@example.com") shouldBe fle.encrypt("alice@example.com")

    "produces ciphertext distinct from plaintext" in:
      fle.encrypt("alice@example.com") should not be "alice@example.com"

    "round-trips: decrypt(encrypt(x)) == x" in:
      fle.decrypt(fle.encrypt("alice@example.com")) shouldBe "alice@example.com"
  }

  "FieldLevelEncryption with enabled = false (local-dev path)" - {
    val fle = fleWith(enabled = false)

    "encrypt is identity" in:
      fle.encrypt("alice@example.com") shouldBe "alice@example.com"

    "decrypt is identity" in:
      fle.decrypt("alice@example.com") shouldBe "alice@example.com"
  }
