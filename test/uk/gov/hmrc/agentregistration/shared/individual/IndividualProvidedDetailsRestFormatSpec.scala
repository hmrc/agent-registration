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

package uk.gov.hmrc.agentregistration.shared.individual

import play.api.libs.json.JsValue
import play.api.libs.json.Json
import uk.gov.hmrc.agentregistration.testsupport.UnitSpec
import uk.gov.hmrc.agentregistration.testsupport.testdata.TdAll

class IndividualProvidedDetailsRestFormatSpec
extends UnitSpec:

  "serialize and deserialize IndividualProvidedDetails" in:
    val individualProvidedDetails: IndividualProvidedDetails = tdAll.providedDetails.afterRiskedFixable
    IndividualProvidedDetailsFormat.restFormat.writes(individualProvidedDetails) shouldBe afterRiskedFixableRestJson
    afterRiskedFixableRestJson.as[IndividualProvidedDetails](using IndividualProvidedDetailsFormat.restFormat) shouldBe individualProvidedDetails

  private val tdAll: TdAll = TdAll()

  // dates as ISO strings
  private val afterRiskedFixableRestJson: JsValue = Json.parse(
    // language=JSON
    """{
      |  "_id": "individual-provided-details-id-12345",
      |  "personReference": "1234567890",
      |  "individualName": "Test Name",
      |  "isPersonOfControl": true,
      |  "internalUserId": "internal-user-id-12345",
      |  "createdAt": "2059-11-25T16:33:51.880Z",
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
