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

package uk.gov.hmrc.agentregistration.shared

import play.api.libs.json.JsValue
import play.api.libs.json.Json
import uk.gov.hmrc.agentregistration.testsupport.UnitSpec
import uk.gov.hmrc.agentregistration.testsupport.testdata.TdAll

import java.time.LocalDate

class AgentApplicationFormatSpec
extends UnitSpec:

  "serialize and deserialize AgentApplication" in:
    val agentApplication: AgentApplication = tdAll.agentApplicationLlp.afterResubmitted
    Json.toJson[AgentApplication](agentApplication) shouldBe afterResubmittedRestJson
    afterResubmittedRestJson.as[AgentApplication] shouldBe agentApplication

  private val tdAll: TdAll =
    new TdAll:
      // TdBase derives these dates from LocalDate.now(); the documents below need fixed values, so they are taken from the frozen time
      override def dateOfIncorporation: LocalDate = nowAsLocalDateTime.toLocalDate.minusYears(10)
      override def riskingCompletedDate: LocalDate = nowAsLocalDateTime.toLocalDate.minusDays(1)
      override def correctiveActionExpiryDate: LocalDate = nowAsLocalDateTime.toLocalDate.plusDays(45)

  // dates as ISO strings
  private val afterResubmittedRestJson: JsValue = Json.parse(
    // language=JSON
    """{
      |  "_id": "agent-application-id-12345",
      |  "cachedSessionId": "session-id-123",
      |  "applicationReference": "APPREF123",
      |  "internalUserId": "internal-user-id-12345",
      |  "applicantCredentials": {
      |    "providerId": "cred-id-12345",
      |    "providerType": "GovernmentGateway"
      |  },
      |  "linkId": "link-id-12345",
      |  "groupId": "group-id-12345",
      |  "createdAt": "2059-11-25T16:33:51.880Z",
      |  "submittedAt": "2059-11-25T16:33:51.880Z",
      |  "applicationState": "SentForRisking",
      |  "userRole": "Authorised",
      |  "businessDetails": {
      |    "safeId": "XA0001234512345",
      |    "saUtr": "1234567895",
      |    "companyProfile": {
      |      "companyNumber": "1234567890",
      |      "companyName": "Test Partnership",
      |      "dateOfIncorporation": "2049-11-25",
      |      "unsanitisedCHROAddress": {
      |        "address_line_1": "23 Great Portland Street",
      |        "address_line_2": "London",
      |        "postal_code": "W1 8LT",
      |        "country": "GB"
      |      }
      |    }
      |  },
      |  "applicantContactDetails": {
      |    "applicantName": "Alice Smith",
      |    "telephoneNumber": "(+44) 10794554342",
      |    "applicantEmailAddress": {
      |      "emailAddress": "user@test.com",
      |      "isVerified": true
      |    }
      |  },
      |  "amlsDetails": {
      |    "supervisoryBody": "HMRC",
      |    "amlsRegistrationNumber": "XAML00000123456"
      |  },
      |  "agentDetails": {
      |    "businessName": {
      |      "agentBusinessName": "Test Company Name"
      |    },
      |    "telephoneNumber": {
      |      "agentTelephoneNumber": "(+44) 10794554342"
      |    },
      |    "agentEmailAddress": {
      |      "emailAddress": {
      |        "agentEmailAddress": "user@test.com"
      |      },
      |      "isVerified": true
      |    },
      |    "agentCorrespondenceAddress": {
      |      "addressLine1": "23 Great Portland Street",
      |      "addressLine2": "London",
      |      "postalCode": "W1 8LT",
      |      "countryCode": "GB"
      |    }
      |  },
      |  "refusalToDealWithCheckResult": "Pass",
      |  "globalAsaEnrolmentCheckResult": "Pass",
      |  "hmrcStandardForAgentsAgreed": "Agreed",
      |  "numberOfIndividuals": {
      |    "numberOfCompaniesHouseOfficers": 2,
      |    "isCompaniesHouseOfficersListCorrect": true,
      |    "type": "FiveOrLessOfficers"
      |  },
      |  "hasOtherRelevantIndividuals": false,
      |  "vrns": [
      |    "123456789"
      |  ],
      |  "payeRefs": [
      |    "123/AB12345"
      |  ],
      |  "riskingOutcomeApplication": {
      |    "actualDecisionDate": "2059-11-24",
      |    "correctiveActionExpiryDate": "2060-01-09",
      |    "reSubmittedAt": "2059-11-25T16:33:51.880Z",
      |    "outcome": "FailedFixable"
      |  },
      |  "riskingOutcomeEntity": {
      |    "fixes": [
      |      {
      |        "failure": {
      |          "type": "_3._5"
      |        },
      |        "isConfirmed": true,
      |        "amlsDetails": {
      |          "supervisoryBody": "HMRC",
      |          "amlsRegistrationNumber": "XAML00000123456"
      |        },
      |        "type": "EntityFix._3.AmlsFix"
      |      },
      |      {
      |        "isConfirmed": true,
      |        "type": "EntityFix._4._4"
      |      },
      |      {
      |        "isConfirmed": true,
      |        "type": "EntityFix._5._4"
      |      }
      |    ],
      |    "type": "FailedFixable"
      |  },
      |  "type": "AgentApplicationLlp"
      |}""".stripMargin
  )
