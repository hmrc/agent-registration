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
import play.api.libs.json.Json
import uk.gov.hmrc.agentregistration.config.AppConfig
import uk.gov.hmrc.agentregistration.shared.individual.IndividualNino
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetails
import uk.gov.hmrc.agentregistration.shared.individual.IndividualSaUtr
import uk.gov.hmrc.agentregistration.testsupport.UnitSpec
import uk.gov.hmrc.agentregistration.testsupport.testdata.TdAll
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

class IndividualProvidedDetailsEncryptionSpec
extends UnitSpec:

  private val tdAll: TdAll = TdAll()

  private val configuration: Configuration = Configuration(ConfigFactory.parseString(
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
      |field-level-encryption.enabled = true
      |field-level-encryption.key = "HIvqb3uQRW8oryUZ3jEQPgMQsvgBSgl71ygWJk6VIdc="
      |field-level-encryption.previousKeys = []
      |""".stripMargin
  ))
  private val appConfig: AppConfig = new AppConfig(new ServicesConfig(configuration), configuration)
  private val fle: FieldLevelEncryption = new FieldLevelEncryption(appConfig)
  private val service: IndividualProvidedDetailsEncryption = new IndividualProvidedDetailsEncryption(fle)

  private def enc(plain: String): String = fle.encrypt(plain)

  private val model: IndividualProvidedDetails = tdAll.providedDetails.afterFinished
  private val encrypted: IndividualProvidedDetails = service.encrypt(model)

  "IndividualProvidedDetailsEncryption.encrypt sets ciphertext on every PII field (afterFinished fixture)" - {

    "individualName is encrypted" in:
      encrypted.individualName.value shouldBe enc(model.individualName.value)

    "internalUserId is encrypted" in:
      encrypted.getInternalUserId.value shouldBe enc(model.getInternalUserId.value)

    "telephoneNumber is encrypted" in:
      encrypted.getTelephoneNumber.value shouldBe enc(model.getTelephoneNumber.value)

    "emailAddress.emailAddress is encrypted" in:
      encrypted.getEmailAddress.emailAddress.value shouldBe enc(model.getEmailAddress.emailAddress.value)

    "individualNino.Provided.nino is encrypted" in:
      val originalNino =
        model.getNino match
          case IndividualNino.Provided(n) => n
          case other => fail(s"expected IndividualNino.Provided, got $other")
      encrypted.getNino match
        case IndividualNino.Provided(n) => n.value shouldBe enc(originalNino.value)
        case other => fail(s"expected IndividualNino.Provided after encrypt, got $other")

    "individualSaUtr.Provided.saUtr is encrypted" in:
      val originalSaUtr =
        model.getIndividualSaUtr match
          case IndividualSaUtr.Provided(s) => s
          case other => fail(s"expected IndividualSaUtr.Provided, got $other")
      encrypted.getIndividualSaUtr match
        case IndividualSaUtr.Provided(s) => s.value shouldBe enc(originalSaUtr.value)
        case other => fail(s"expected IndividualSaUtr.Provided after encrypt, got $other")

    "vrns are encrypted element-wise" in:
      encrypted.vrns.value.map(_.value) shouldBe model.vrns.value.map(v => enc(v.value))

    "payeRefs are encrypted element-wise" in:
      encrypted.payeRefs.value.map(_.value) shouldBe model.payeRefs.value.map(p => enc(p.value))

    "personReference stays plaintext (search key)" in:
      encrypted.personReference.value shouldBe model.personReference.value

    "agentApplicationId stays plaintext (search key)" in:
      encrypted.agentApplicationId.value shouldBe model.agentApplicationId.value
  }

  "IndividualProvidedDetailsEncryption single-value helpers" - {

    "encrypt(InternalUserId) produces ciphertext" in:
      service.encrypt(tdAll.internalUserId).value shouldBe enc(tdAll.internalUserId.value)

    "encrypt then decrypt round-trips" in:
      service.decrypt(service.encrypt(tdAll.internalUserId)) shouldBe tdAll.internalUserId
  }

  "IndividualProvidedDetailsEncryption handles sealed-trait branches" - {

    "IndividualNino.FromAuth is encrypted" in:
      val withFromAuth: IndividualProvidedDetails = model.copy(individualNino = Some(IndividualNino.FromAuth(tdAll.nino)))
      val originalNino = tdAll.nino.value
      service.encrypt(withFromAuth).getNino match
        case IndividualNino.FromAuth(n) => n.value shouldBe enc(originalNino)
        case other => fail(s"expected IndividualNino.FromAuth after encrypt, got $other")

    "IndividualNino.NotProvided is unchanged" in:
      val withNotProvided: IndividualProvidedDetails = model.copy(individualNino = Some(IndividualNino.NotProvided))
      service.encrypt(withNotProvided).getNino shouldBe IndividualNino.NotProvided

    "IndividualSaUtr.FromAuth is encrypted" in:
      val withFromAuth: IndividualProvidedDetails = model.copy(individualSaUtr = Some(IndividualSaUtr.FromAuth(tdAll.saUtr)))
      val originalSaUtr = tdAll.saUtr.value
      service.encrypt(withFromAuth).getIndividualSaUtr match
        case IndividualSaUtr.FromAuth(s) => s.value shouldBe enc(originalSaUtr)
        case other => fail(s"expected IndividualSaUtr.FromAuth after encrypt, got $other")

    "IndividualSaUtr.FromCitizenDetails is encrypted" in:
      val withFromCitizen: IndividualProvidedDetails = model.copy(individualSaUtr = Some(IndividualSaUtr.FromCitizenDetails(tdAll.saUtr)))
      val originalSaUtr = tdAll.saUtr.value
      service.encrypt(withFromCitizen).getIndividualSaUtr match
        case IndividualSaUtr.FromCitizenDetails(s) => s.value shouldBe enc(originalSaUtr)
        case other => fail(s"expected IndividualSaUtr.FromCitizenDetails after encrypt, got $other")

    "IndividualSaUtr.NotProvided is unchanged" in:
      val withNotProvided: IndividualProvidedDetails = model.copy(individualSaUtr = Some(IndividualSaUtr.NotProvided))
      service.encrypt(withNotProvided).getIndividualSaUtr shouldBe IndividualSaUtr.NotProvided
  }

  "IndividualProvidedDetailsEncryption round-trips and does not leak plaintext PII" - {

    "encrypt then decrypt is identity" in:
      service.decrypt(service.encrypt(model)) shouldBe model

    "rendered JSON of the encrypted model contains no plaintext PII" in:
      val rendered = Json.toJson(service.encrypt(model))(IndividualProvidedDetails.format).toString
      val plaintextPii: List[String] =
        List(
          model.individualName.value,
          model.getInternalUserId.value,
          model.getTelephoneNumber.value,
          model.getEmailAddress.emailAddress.value
        ) ++
          (model.getNino match { case IndividualNino.Provided(n) => List(n.value); case _ => Nil }) ++
          (model.getIndividualSaUtr match { case IndividualSaUtr.Provided(s) => List(s.value); case _ => Nil }) ++
          model.vrns.value.map(_.value) ++
          model.payeRefs.value.map(_.value)

      plaintextPii.foreach { plaintext =>
        withClue(s"plaintext '$plaintext' must not appear as a JSON value in encrypted JSON: ") {
          rendered should not include s"\"$plaintext\""
        }
      }
  }
