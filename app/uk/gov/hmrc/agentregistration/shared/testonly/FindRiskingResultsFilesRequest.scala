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

package uk.gov.hmrc.agentregistration.shared.testonly

import play.api.libs.json.Json
import play.api.libs.json.OFormat
import uk.gov.hmrc.agentregistration.shared.ApplicationReference
import uk.gov.hmrc.agentregistration.shared.PersonReference

/** Asks the risking service's test-only stub for the risking results files holding records about any of these applications or individuals. */
final case class FindRiskingResultsFilesRequest(
  applicationReferences: List[ApplicationReference],
  personReferences: List[PersonReference]
)

object FindRiskingResultsFilesRequest:
  given format: OFormat[FindRiskingResultsFilesRequest] = Json.format[FindRiskingResultsFilesRequest]
