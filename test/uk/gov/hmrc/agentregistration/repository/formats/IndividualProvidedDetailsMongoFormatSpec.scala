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

package uk.gov.hmrc.agentregistration.repository.formats

import play.api.libs.json.JsObject
import play.api.libs.json.Json
import uk.gov.hmrc.agentregistration.shared.individual.IndividualDateOfBirth
import uk.gov.hmrc.agentregistration.shared.individual.IndividualNino
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetails
import uk.gov.hmrc.agentregistration.shared.individual.IndividualSaUtr
import uk.gov.hmrc.agentregistration.testsupport.UnitSpec
import uk.gov.hmrc.agentregistration.testsupport.testdata.TdAll
import uk.gov.hmrc.crypto.Decrypter
import uk.gov.hmrc.crypto.Encrypter
import uk.gov.hmrc.crypto.PlainText
import uk.gov.hmrc.crypto.SymmetricCryptoFactory

class IndividualProvidedDetailsMongoFormatSpec
extends UnitSpec:

  private val tdAll: TdAll = TdAll()

  private given crypto: Encrypter & Decrypter = SymmetricCryptoFactory.aesCrypto("HIvqb3uQRW8oryUZ3jEQPgMQsvgBSgl71ygWJk6VIdc=")

  private def enc(plain: String): String = crypto.encrypt(PlainText(plain)).value

  private val model: IndividualProvidedDetails = tdAll.providedDetails.afterFinished

  private val expectedJson: JsObject = Json.obj(
    "_id" -> model._id.value,
    "personReference" -> model.personReference.value,
    "individualName" -> enc(model.individualName.value),
    "isPersonOfControl" -> model.isPersonOfControl,
    "internalUserId" -> enc(model.getInternalUserId.value),
    "createdAt" -> model.createdAt,
    "providedDetailsState" -> "Finished",
    "agentApplicationId" -> model.agentApplicationId.value,
    "individualDateOfBirth" -> Json.obj(
      "dateOfBirth" -> {
        model.getDateOfBirth match
          case IndividualDateOfBirth.Provided(d) => d.toString
          case _ => fail("expected IndividualDateOfBirth.Provided in test data")
      },
      "type" -> "Provided"
    ),
    "telephoneNumber" -> enc(model.getTelephoneNumber.value),
    "emailAddress" -> Json.obj(
      "emailAddress" -> enc(model.getEmailAddress.emailAddress.value),
      "isVerified" -> model.getEmailAddress.isVerified
    ),
    "individualNino" -> Json.obj(
      "nino" -> {
        model.getNino match
          case IndividualNino.Provided(n) => enc(n.value)
          case _ => fail("expected IndividualNino.Provided in test data")
      },
      "type" -> "Provided"
    ),
    "individualSaUtr" -> Json.obj(
      "saUtr" -> {
        model.getSaUtr match
          case IndividualSaUtr.Provided(u) => enc(u.value)
          case _ => fail("expected IndividualSaUtr.Provided in test data")
      },
      "type" -> "Provided"
    ),
    "hmrcStandardForAgentsAgreed" -> "Agreed",
    "hasApprovedApplication" -> model.hasApprovedApplication.value,
    "vrns" -> Json.arr(enc(model.vrns.value.headOption.value.value)),
    "payeRefs" -> Json.arr(enc(model.payeRefs.value.headOption.value.value)),
    "passedIv" -> model.passedIv.value
  )

  "mongoFormat writes the model to JSON with PII fields encrypted at the expected paths" in:
    Json.toJson(model)(IndividualProvidedDetailsMongoFormat.mongoFormat) shouldBe expectedJson

  "mongoFormat reads the JSON and decrypts PII fields back to the model" in:
    expectedJson.as[IndividualProvidedDetails](IndividualProvidedDetailsMongoFormat.mongoFormat) shouldBe model
