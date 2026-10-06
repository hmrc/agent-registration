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

package uk.gov.hmrc.agentregistration.repository

import play.api.libs.json.JsObject
import play.api.libs.json.Json
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.risking.RiskingOutcomeApplication
import uk.gov.hmrc.agentregistration.testsupport.UnitSpec
import uk.gov.hmrc.agentregistration.testsupport.testdata.TdAll

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class AgentApplicationMongoFormatSpec
extends UnitSpec:

  private val tdAll: TdAll = TdAll()

  // the dates under test, set explicitly so the expected JSON below does not depend on the shared test data
  private val createdAt: Instant = Instant.parse("2026-03-04T05:06:07.890Z")
  private val applicationExpiresAt: Instant = Instant.parse("2026-05-16T05:06:07.890Z")
  private val submittedAt: Instant = Instant.parse("2026-04-10T11:12:13.140Z")
  private val reSubmittedAt: Instant = Instant.parse("2026-06-20T21:22:23.240Z")
  private val actualDecisionDate: LocalDate = LocalDate.parse("2026-05-01")
  private val correctiveActionExpiryDate: LocalDate = LocalDate.parse("2026-06-15")

  private def bsonDate(instant: Instant): JsObject = Json.obj("$date" -> Json.obj("$numberLong" -> instant.toEpochMilli.toString))
  private def bsonDate(localDate: LocalDate): JsObject = bsonDate(localDate.atStartOfDay(ZoneOffset.UTC).toInstant)

  // expected JSON of the date fields: REST keeps ISO strings, Mongo holds BSON Dates
  private val expectedRestDatesOfUnsubmittedApplication: JsObject = Json.obj(
    "createdAt" -> "2026-03-04T05:06:07.890Z",
    "applicationExpiresAt" -> "2026-05-16T05:06:07.890Z"
  )
  private val expectedMongoDatesOfUnsubmittedApplication: JsObject = Json.obj(
    "createdAt" -> bsonDate(createdAt),
    "applicationExpiresAt" -> bsonDate(applicationExpiresAt)
  )

  private val expectedRestDatesOfSubmittedApplication: JsObject = Json.obj(
    "createdAt" -> "2026-03-04T05:06:07.890Z",
    "submittedAt" -> "2026-04-10T11:12:13.140Z"
  )
  private val expectedMongoDatesOfSubmittedApplication: JsObject = Json.obj(
    "createdAt" -> bsonDate(createdAt),
    "submittedAt" -> bsonDate(submittedAt)
  )

  private val expectedRestDatesOfResubmittedApplication: JsObject =
    expectedRestDatesOfSubmittedApplication ++ Json.obj(
      "riskingOutcomeApplication" -> Json.obj(
        "actualDecisionDate" -> "2026-05-01",
        "correctiveActionExpiryDate" -> "2026-06-15",
        "reSubmittedAt" -> "2026-06-20T21:22:23.240Z"
      )
    )
  private val expectedMongoDatesOfResubmittedApplication: JsObject =
    expectedMongoDatesOfSubmittedApplication ++ Json.obj(
      "riskingOutcomeApplication" -> Json.obj(
        "actualDecisionDate" -> bsonDate(actualDecisionDate),
        "correctiveActionExpiryDate" -> bsonDate(correctiveActionExpiryDate),
        "reSubmittedAt" -> bsonDate(reSubmittedAt)
      )
    )

  private val expectedRestDatesOfApplicationWithNonFixableOutcome: JsObject =
    expectedRestDatesOfSubmittedApplication ++ Json.obj(
      "riskingOutcomeApplication" -> Json.obj(
        "actualDecisionDate" -> "2026-05-01",
        "correctiveActionExpiryDate" -> "2026-06-15"
      )
    )
  private val expectedMongoDatesOfApplicationWithNonFixableOutcome: JsObject =
    expectedMongoDatesOfSubmittedApplication ++ Json.obj(
      "riskingOutcomeApplication" -> Json.obj(
        "actualDecisionDate" -> bsonDate(actualDecisionDate),
        "correctiveActionExpiryDate" -> bsonDate(correctiveActionExpiryDate)
      )
    )

  private final case class Case(
    name: String,
    application: AgentApplication,
    expectedRestDates: JsObject,
    expectedMongoDates: JsObject
  )

  private val cases: Seq[Case] = Seq(
    Case(
      "LLP before submission",
      tdAll.agentApplicationLlp.afterStarted.copy(createdAt = createdAt, applicationExpiresAt = Some(applicationExpiresAt)),
      expectedRestDatesOfUnsubmittedApplication,
      expectedMongoDatesOfUnsubmittedApplication
    ),
    Case(
      "LLP after submission",
      tdAll.agentApplicationLlp.afterSentForRisking.copy(createdAt = createdAt, submittedAt = Some(submittedAt)),
      expectedRestDatesOfSubmittedApplication,
      expectedMongoDatesOfSubmittedApplication
    ),
    Case(
      "LLP after resubmission",
      tdAll.agentApplicationLlp.afterResubmitted.copy(
        createdAt = createdAt,
        submittedAt = Some(submittedAt),
        riskingOutcomeApplication = Some(RiskingOutcomeApplication.FailedFixable(
          actualDecisionDate,
          correctiveActionExpiryDate,
          Some(reSubmittedAt)
        ))
      ),
      expectedRestDatesOfResubmittedApplication,
      expectedMongoDatesOfResubmittedApplication
    ),
    Case(
      "LLP after a non-fixable outcome",
      tdAll.agentApplicationLlp.afterRiskingCompletedNonFixable.copy(
        createdAt = createdAt,
        submittedAt = Some(submittedAt),
        riskingOutcomeApplication = Some(RiskingOutcomeApplication.FailedNonFixable(actualDecisionDate, correctiveActionExpiryDate))
      ),
      expectedRestDatesOfApplicationWithNonFixableOutcome,
      expectedMongoDatesOfApplicationWithNonFixableOutcome
    ),
    Case(
      "sole trader",
      tdAll.agentApplicationSoleTrader.soleTraderWithTrn.copy(createdAt = createdAt, submittedAt = Some(submittedAt)),
      expectedRestDatesOfSubmittedApplication,
      expectedMongoDatesOfSubmittedApplication
    ),
    Case(
      "limited company",
      tdAll.agentApplicationLimitedCompany.afterDeclarationSubmitted.copy(createdAt = createdAt, submittedAt = Some(submittedAt)),
      expectedRestDatesOfSubmittedApplication,
      expectedMongoDatesOfSubmittedApplication
    ),
    Case(
      "general partnership",
      tdAll.agentApplicationGeneralPartnership.afterDeclarationSubmitted.copy(createdAt = createdAt, submittedAt = Some(submittedAt)),
      expectedRestDatesOfSubmittedApplication,
      expectedMongoDatesOfSubmittedApplication
    ),
    Case(
      "limited partnership",
      tdAll.agentApplicationLimitedPartnership.afterDeclarationSubmitted.copy(createdAt = createdAt, submittedAt = Some(submittedAt)),
      expectedRestDatesOfSubmittedApplication,
      expectedMongoDatesOfSubmittedApplication
    ),
    Case(
      "Scottish limited partnership",
      tdAll.agentApplicationScottishLimitedPartnership.afterDeclarationSubmitted.copy(createdAt = createdAt, submittedAt = Some(submittedAt)),
      expectedRestDatesOfSubmittedApplication,
      expectedMongoDatesOfSubmittedApplication
    ),
    Case(
      "Scottish partnership",
      tdAll.agentApplicationScottishPartnership.afterDeclarationSubmitted.copy(createdAt = createdAt, submittedAt = Some(submittedAt)),
      expectedRestDatesOfSubmittedApplication,
      expectedMongoDatesOfSubmittedApplication
    )
  )

  cases.foreach: testCase =>
    s"${testCase.name}" - {
      val restJson: JsObject = AgentApplication.format.writes(testCase.application)
      val mongoJson: JsObject = AgentApplicationMongoFormat.format.writes(testCase.application)

      "REST JSON holds the dates as ISO strings" in:
        restJson shouldBe restJson.deepMerge(testCase.expectedRestDates)

      "Mongo JSON is the REST JSON with exactly the migrated date fields replaced by BSON Dates" in:
        mongoJson shouldBe restJson.deepMerge(testCase.expectedMongoDates)

      "Mongo format reads back what it wrote" in:
        AgentApplicationMongoFormat.format.reads(mongoJson).get shouldBe testCase.application

      "Mongo format reads the dates stored as legacy ISO strings" in:
        AgentApplicationMongoFormat.format.reads(mongoJson.deepMerge(testCase.expectedRestDates)).get shouldBe testCase.application
    }
