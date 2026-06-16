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

package uk.gov.hmrc.agentregistration.model

import play.api.libs.json.Json
import play.api.libs.json.OFormat
import uk.gov.hmrc.agentregistration.shared.ApplicationReference
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats.Implicits.given

import java.time.Clock
import java.time.Instant

/** Tracks scheduler-driven decisions about an application's emails. One record per application; each scheduler job owns its own [[EmailStatus]] field and
  * writes only that field (field-level upsert) — see the carve-out in `feedback_be_patterns` memory for the rationale.
  *
  * Stored separately from `AgentApplication` so scheduler writes don't race concurrent FE writes of the application document (APB-11490). Keyed by
  * `ApplicationReference` (matching Risking) — human-meaningful in logs and audit trails.
  */
final case class ApplicationScheduler(
  _id: ApplicationReference,
  applicationReadyToSubmitEmailStatus: EmailStatus,
  lastUpdated: Instant
)

object ApplicationScheduler:

  given OFormat[ApplicationScheduler] = Json.format[ApplicationScheduler]

  /** Create a fresh record for an application no scheduler has decided about yet. */
  def makeNew(
    applicationReference: ApplicationReference,
    clock: Clock = Clock.systemUTC()
  ): ApplicationScheduler = ApplicationScheduler(
    _id = applicationReference,
    applicationReadyToSubmitEmailStatus = EmailStatus.NotProcessed,
    lastUpdated = Instant.now(clock)
  )
