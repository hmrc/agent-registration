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
import play.api.libs.json.JsonConfiguration
import play.api.libs.json.OFormat
import sttp.model.Uri
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.===
import uk.gov.hmrc.agentregistration.shared.util.JsonConfig
import uk.gov.hmrc.agentregistration.shared.util.UriFormat

import scala.annotation.nowarn

sealed trait UploadStatus

object UploadStatus:

  case object InProgress
  extends UploadStatus

  final case class Failed(
    failureReason: String,
    messageFromUpscan: String
  )
  extends UploadStatus:
    def isInQuarantine: Boolean = failureReason === "QUARANTINE"

  final case class UploadedSuccessfully(
    fileName: String,
    mimeType: String,
    downloadUrl: Uri,
    sizeInBytes: Long,
    checksum: String
  )
  extends UploadStatus

  @nowarn()
  given OFormat[UploadStatus] =
    import UriFormat.uriFormat
    given OFormat[InProgress.type] = Json.format[InProgress.type]
    given OFormat[Failed] = Json.format[Failed]
    given uploadedFormat: OFormat[UploadedSuccessfully] = Json.format[UploadedSuccessfully]
    given JsonConfiguration = JsonConfig.jsonConfiguration
    val dontDeleteMe = """
        |Don't delete me.
        |I will emit a warning so `@nowarn` can be applied to address below
        |`Unreachable case except for null` problem emited by Play Json macro"""

    Json.format[UploadStatus]
