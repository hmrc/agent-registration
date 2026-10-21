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

import org.scalatest.Assertion
import play.api.libs.json.JsObject
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import uk.gov.hmrc.agentregistration.repository.FieldNames
import uk.gov.hmrc.agentregistration.shared.individual.IndividualNino
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetails
import uk.gov.hmrc.agentregistration.shared.individual.IndividualSaUtr
import uk.gov.hmrc.agentregistration.testsupport.UnitSpec
import uk.gov.hmrc.agentregistration.testsupport.testdata.TdAll

class IndividualProvidedDetailsMongoFormatsSpec
extends UnitSpec:

  "plaintextFormat" - {

    "serialize and deserialize IndividualProvidedDetails" in:
      val individualProvidedDetails: IndividualProvidedDetails = tdAll.providedDetails.afterRiskedFixable
      IndividualProvidedDetailsMongoFormats.plaintextFormat.writes(individualProvidedDetails) shouldBe afterRiskedFixableJson
      afterRiskedFixableJson.as[IndividualProvidedDetails](using
        IndividualProvidedDetailsMongoFormats.plaintextFormat
      ) shouldBe individualProvidedDetails

  }

  "encryptingFormat" - {

    "serialize and deserialize IndividualProvidedDetails" in:
      mongoFormats.encryptingFormat.reads(mongoFormats.encryptingFormat.writes(model)).get shouldBe model

    "store no plaintext PII" in:
      val written: String = mongoFormats.encryptingFormat.writes(model).toString
      plaintextPii.foreach: plaintext =>
        withClue(s"plaintext '$plaintext' must not appear as a JSON value in Mongo JSON: "):
          written should not include s"\"$plaintext\""

    "store the hash of the plaintext internal user id and the encrypted internal user id when present" in:
      val written: JsObject = mongoFormats.encryptingFormat.writes(model)
      val plainId: String = model.internalUserId.value.value
      (written \ FieldNames.internalUserIdHash).as[String] shouldBe hasher.hash(plainId)
      (written \ FieldNames.internalUserId).as[String] shouldBeEncryptionOf (plainId)

    s"omit both internalUserId and ${FieldNames.internalUserIdHash} when there is no internal user id" in:
      val written: JsObject = mongoFormats.encryptingFormat.writes(modelWithoutInternalUserId)
      written.keys should not contain FieldNames.internalUserId
      written.keys should not contain FieldNames.internalUserIdHash

    s"read back an individual who has no internal user id, ignoring ${FieldNames.internalUserIdHash}" in:
      mongoFormats.encryptingFormat.reads(mongoFormats.encryptingFormat.writes(model)).get shouldBe model
  }

  "encrypt sets ciphertext on every PII field" - {

    "individualName is encrypted" in:
      encrypted.individualName.value shouldBeEncryptionOf (model.individualName.value)

    "internalUserId is encrypted" in:
      encrypted.getInternalUserId.value shouldBeEncryptionOf (model.getInternalUserId.value)

    "telephoneNumber is encrypted" in:
      encrypted.getTelephoneNumber.value shouldBeEncryptionOf (model.getTelephoneNumber.value)

    "emailAddress.emailAddress is encrypted" in:
      encrypted.getEmailAddress.emailAddress.value shouldBeEncryptionOf (model.getEmailAddress.emailAddress.value)

    "individualNino.Provided.nino is encrypted" in:
      val originalNino =
        model.getNino match
          case IndividualNino.Provided(n) => n
          case other => fail(s"expected IndividualNino.Provided, got $other")
      encrypted.getNino match
        case IndividualNino.Provided(n) => n.value shouldBeEncryptionOf (originalNino.value)
        case other => fail(s"expected IndividualNino.Provided after encrypt, got $other")

    "individualSaUtr.Provided.saUtr is encrypted" in:
      val originalSaUtr =
        model.getIndividualSaUtr match
          case IndividualSaUtr.Provided(s) => s
          case other => fail(s"expected IndividualSaUtr.Provided, got $other")
      encrypted.getIndividualSaUtr match
        case IndividualSaUtr.Provided(s) => s.value shouldBeEncryptionOf (originalSaUtr.value)
        case other => fail(s"expected IndividualSaUtr.Provided after encrypt, got $other")

    "vrns are encrypted element-wise" in:
      encrypted.vrns.value.map(_.value)
        .zip(model.vrns.value.map(_.value))
        .foreach(
          (
            ciphertext,
            plain
          ) => ciphertext shouldBeEncryptionOf plain
        )
      encrypted.vrns.value.size shouldBe model.vrns.value.size

    "payeRefs are encrypted element-wise" in:
      encrypted.payeRefs.value.map(_.value)
        .zip(model.payeRefs.value.map(_.value))
        .foreach(
          (
            ciphertext,
            plain
          ) => ciphertext shouldBeEncryptionOf plain
        )
      encrypted.payeRefs.value.size shouldBe model.payeRefs.value.size

    "personReference stays plaintext (search key)" in:
      encrypted.personReference.value shouldBe model.personReference.value

    "agentApplicationId stays plaintext (search key)" in:
      encrypted.agentApplicationId.value shouldBe model.agentApplicationId.value
  }

  "encrypt handles every IndividualNino and IndividualSaUtr branch" - {

    "IndividualNino.FromAuth is encrypted" in:
      val withFromAuth: IndividualProvidedDetails = model.copy(individualNino = Some(IndividualNino.FromAuth(tdAll.nino)))
      val originalNino = tdAll.nino.value
      mongoFormats.encrypt(withFromAuth).getNino match
        case IndividualNino.FromAuth(n) => n.value shouldBeEncryptionOf (originalNino)
        case other => fail(s"expected IndividualNino.FromAuth after encrypt, got $other")

    "IndividualNino.NotProvided is unchanged" in:
      val withNotProvided: IndividualProvidedDetails = model.copy(individualNino = Some(IndividualNino.NotProvided))
      mongoFormats.encrypt(withNotProvided).getNino shouldBe IndividualNino.NotProvided

    "IndividualSaUtr.FromAuth is encrypted" in:
      val withFromAuth: IndividualProvidedDetails = model.copy(individualSaUtr = Some(IndividualSaUtr.FromAuth(tdAll.saUtr)))
      val originalSaUtr = tdAll.saUtr.value
      mongoFormats.encrypt(withFromAuth).getIndividualSaUtr match
        case IndividualSaUtr.FromAuth(s) => s.value shouldBeEncryptionOf (originalSaUtr)
        case other => fail(s"expected IndividualSaUtr.FromAuth after encrypt, got $other")

    "IndividualSaUtr.FromCitizenDetails is encrypted" in:
      val withFromCitizen: IndividualProvidedDetails = model.copy(individualSaUtr = Some(IndividualSaUtr.FromCitizenDetails(tdAll.saUtr)))
      val originalSaUtr = tdAll.saUtr.value
      mongoFormats.encrypt(withFromCitizen).getIndividualSaUtr match
        case IndividualSaUtr.FromCitizenDetails(s) => s.value shouldBeEncryptionOf (originalSaUtr)
        case other => fail(s"expected IndividualSaUtr.FromCitizenDetails after encrypt, got $other")

    "IndividualSaUtr.NotProvided is unchanged" in:
      val withNotProvided: IndividualProvidedDetails = model.copy(individualSaUtr = Some(IndividualSaUtr.NotProvided))
      mongoFormats.encrypt(withNotProvided).getIndividualSaUtr shouldBe IndividualSaUtr.NotProvided
  }

  private val tdAll: TdAll = TdAll()

  private val hasher: Hasher = Hasher("U/g3pVPBa/hIJ109AIwEeuRefPzeqXB312MTw/JTF00=")

  private val fieldLevelEncryption: FieldLevelEncryption = FieldLevelEncryption(
    enabled = true,
    key = "HIvqb3uQRW8oryUZ3jEQPgMQsvgBSgl71ygWJk6VIdc=",
    previousKeys = Seq.empty
  )
  private val mongoFormats: IndividualProvidedDetailsMongoFormats = new IndividualProvidedDetailsMongoFormats(fieldLevelEncryption, hasher)

  /** AES-GCM gives a different ciphertext every time, so a field is encrypted if it is not the plaintext and decrypts back to it. */
  extension (ciphertext: String)
    private infix def shouldBeEncryptionOf(plain: String): Assertion =
      withClue(s"ciphertext of '$plain': "):
        ciphertext should not be plain
        fieldLevelEncryption.decrypt(ciphertext) shouldBe plain

  private val model: IndividualProvidedDetails = tdAll.providedDetails.afterFinished
  private val modelWithoutInternalUserId: IndividualProvidedDetails = tdAll.providedDetails.precreated
  private val encrypted: IndividualProvidedDetails = mongoFormats.encrypt(model)

  private val plaintextPii: List[String] =
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

  // dates as BSON dates
  private val afterRiskedFixableJson: JsValue = Json.parse(
    // language=JSON
    """{
      |  "_id": "individual-provided-details-id-12345",
      |  "personReference": "1234567890",
      |  "individualName": "Test Name",
      |  "isPersonOfControl": true,
      |  "internalUserId": "internal-user-id-12345",
      |  "createdAt": {
      |    "$date": {
      |      "$numberLong": "2837003631880"
      |    }
      |  },
      |  "providedDetailsState": "Finished",
      |  "agentApplicationId": "agent-application-id-12345",
      |  "individualDateOfBirth": {
      |    "dateOfBirth": "2000-01-01",
      |    "type": "Provided"
      |  },
      |  "telephoneNumber": "(+44) 10794554342",
      |  "emailAddress": {
      |    "emailAddress": "member@test.com",
      |    "isVerified": true
      |  },
      |  "individualNino": {
      |    "nino": "AB123456C",
      |    "type": "Provided"
      |  },
      |  "individualSaUtr": {
      |    "saUtr": "1234567895",
      |    "type": "Provided"
      |  },
      |  "hmrcStandardForAgentsAgreed": "Agreed",
      |  "hasApprovedApplication": true,
      |  "vrns": [
      |    "123456789"
      |  ],
      |  "payeRefs": [
      |    "123/AB12345"
      |  ],
      |  "passedIv": true,
      |  "providedByApplicant": false,
      |  "riskingOutcomeIndividual": {
      |    "fixes": [
      |      {
      |        "type": "IndividualFix._4._1"
      |      }
      |    ],
      |    "declarationAgreed": false,
      |    "type": "FailedFixable"
      |  }
      |}""".stripMargin
  )
