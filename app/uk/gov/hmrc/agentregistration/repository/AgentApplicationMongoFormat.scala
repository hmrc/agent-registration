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

import play.api.libs.functional.syntax.*
import play.api.libs.json.Format
import play.api.libs.json.Json
import play.api.libs.json.JsonConfiguration
import play.api.libs.json.OFormat
import play.api.libs.json.Reads
import play.api.libs.json.__
import uk.gov.hmrc.agentregistration.shared.*
import uk.gov.hmrc.agentregistration.shared.risking.RiskingOutcomeApplication
import uk.gov.hmrc.agentregistration.shared.util.JsonConfig
import uk.gov.hmrc.auth.core.retrieve.Credentials
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats

import java.time.Instant
import java.time.LocalDate
import scala.annotation.nowarn

/** Mongo format of [[AgentApplication]]. Same as the REST format except that `createdAt`, `applicationExpiresAt`, `submittedAt`,
  * `riskingOutcomeApplication.reSubmittedAt` and `riskingOutcomeApplication.correctiveActionExpiryDate` are BSON `Date`.
  */
object AgentApplicationMongoFormat:

  private given Format[Instant] = MongoInstantFormat.instantFormat

  // for data that exists prior to the migration to BSON Date
  private val legacyLocalDateReads: Reads[LocalDate] = Reads.DefaultLocalDateReads

  private val correctiveActionExpiryDateFormat: Format[LocalDate] = Format(
    MongoJavatimeFormats.localDateReads.orElse(legacyLocalDateReads),
    MongoJavatimeFormats.localDateWrites
  )

  @nowarn()
  private val riskingOutcomeApplicationFormat: OFormat[RiskingOutcomeApplication] =
    given JsonConfiguration = JsonConfig.jsonConfiguration(discriminator = "outcome")
    given OFormat[RiskingOutcomeApplication.Approved] = Json.format[RiskingOutcomeApplication.Approved]
    given OFormat[RiskingOutcomeApplication.FailedFixable] =
      (
        (__ \ "actualDecisionDate").format[LocalDate] and
          (__ \ "correctiveActionExpiryDate").format[LocalDate](correctiveActionExpiryDateFormat) and
          (__ \ "reSubmittedAt").formatNullable[Instant]
      )(
        RiskingOutcomeApplication.FailedFixable.apply,
        failedFixable => (failedFixable.actualDecisionDate, failedFixable.correctiveActionExpiryDate, failedFixable.reSubmittedAt)
      )
    given OFormat[RiskingOutcomeApplication.FailedNonFixable] =
      (
        (__ \ "actualDecisionDate").format[LocalDate] and
          (__ \ "correctiveActionExpiryDate").format[LocalDate](correctiveActionExpiryDateFormat)
      )(
        RiskingOutcomeApplication.FailedNonFixable.apply,
        failedNonFixable => (failedNonFixable.actualDecisionDate, failedNonFixable.correctiveActionExpiryDate)
      )

    val dontDeleteMe = """
        |Don't delete me.
        |I will emit a warning so `@nowarn` can be applied to address below
        |`Unreachable case except for null` problem emited by Play Json macro"""

    Json.format[RiskingOutcomeApplication]

  @nowarn()
  val format: OFormat[AgentApplication] =
    given OFormat[RiskingOutcomeApplication] = riskingOutcomeApplicationFormat
    given OFormat[AgentApplicationSoleTrader] = Json.format[AgentApplicationSoleTrader]
    given OFormat[AgentApplicationLlp] = Json.format[AgentApplicationLlp]
    given OFormat[AgentApplicationLimitedCompany] = Json.format[AgentApplicationLimitedCompany]
    given OFormat[AgentApplicationGeneralPartnership] = Json.format[AgentApplicationGeneralPartnership]
    given OFormat[AgentApplicationLimitedPartnership] = Json.format[AgentApplicationLimitedPartnership]
    given OFormat[AgentApplicationScottishLimitedPartnership] = Json.format[AgentApplicationScottishLimitedPartnership]
    given OFormat[AgentApplicationScottishPartnership] = Json.format[AgentApplicationScottishPartnership]
    given OFormat[Credentials] = Json.format[Credentials]

    given JsonConfiguration = JsonConfig.jsonConfiguration

    val dontDeleteMe = """
        |Don't delete me.
        |I will emit a warning so `@nowarn` can be applied to address below
        |`Unreachable case except for null` problem emited by Play Json macro"""

    Json.format[AgentApplication]
