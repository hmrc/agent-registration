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

package uk.gov.hmrc.agentregistration.shared.upscan

import play.api.libs.json.Json
import play.api.libs.json.OFormat
import uk.gov.hmrc.agentregistration.shared.InternalUserId
import uk.gov.hmrc.agentregistration.shared.upload.UploadId

import java.time.Instant

final case class Upload(
  private val _id: UploadId,
  internalUserId: InternalUserId,
  createdAt: Instant,
  fileUploadReference: FileUploadReference,
  uploadStatus: UploadStatus
):
  val uploadId: UploadId = _id

object Upload:
  given format: OFormat[Upload] = Json.format[Upload]
