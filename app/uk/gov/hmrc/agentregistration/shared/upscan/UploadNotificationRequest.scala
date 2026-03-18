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

import play.api.libs.json.*
import sttp.model.Uri
import uk.gov.hmrc.agentregistration.shared.util.UriFormat

/** Represents a callback notification request body sent from upscan-notify microsvice containing the outcome of uploaded file processing.
  *
  * The callback can indicate either a successful or failed upload
  *
  * For more details see upscan's documentation:
  *   - https://github.com/hmrc/upscan-initiate#file-processing-outcome-
  *   - https://github.com/hmrc/upscan-initiate#success-
  *   - https://github.com/hmrc/upscan-initiate#failure-
  *
  * Example bodies (snipped from above readmes):
  * {{{
  *   {
  *     "reference": "11370e18-6e24-453e-b45a-76d3e32ea33d",
  *     "downloadUrl": "https://bucketName.s3.eu-west-2.amazonaws.com?1235676",
  *     "fileStatus": "READY",
  *     "uploadDetails": {
  *         "fileName": "test.pdf",
  *         "fileMimeType": "application/pdf",
  *         "uploadTimestamp": "2018-04-24T09:30:00Z",
  *         "checksum": "396f101dd52e8b2ace0dcf5ed09b1d1f030e608938510ce46e7a5c7a4e775100",
  *         "size": 987
  *     }
  * }
  * }}}
  *
  * {{{
  *   {
  *     "reference" : "11370e18-6e24-453e-b45a-76d3e32ea33d",
  *     "fileStatus" : "FAILED",
  *     "failureDetails": {
  *         "failureReason": "QUARANTINE",  //or "REJECTED" or "UNKNOWN"
  *         "message": "e.g. This file has a virus"
  *     }
  * }
  * }}}
  */
sealed trait UploadNotificationRequest:
  def reference: FileUploadReference

object UploadNotificationRequest:

  final case class Succeeded(
    reference: FileUploadReference,
    downloadUrl: Uri,
    uploadDetails: UploadEventDetails
  )
  extends UploadNotificationRequest

  final case class Failed(
    reference: FileUploadReference,
    failureDetails: ErrorDetails
  )
  extends UploadNotificationRequest

  given Reads[UploadNotificationRequest] =
    given Reads[UploadEventDetails] = Json.reads[UploadEventDetails]
    given Reads[ErrorDetails] = Json.reads[ErrorDetails]
    given Format[Uri] = UriFormat.uriFormat
    given Reads[Succeeded] = Json.reads[Succeeded]
    given Reads[Failed] = Json.reads[Failed]

    (json: JsValue) =>
      json \ "fileStatus" match
        case JsDefined(JsString("READY")) => json.validate[Succeeded]
        case JsDefined(JsString("FAILED")) => json.validate[Failed]
        case JsDefined(value) => JsError(s"Invalid type discriminator: $value")
        case _ => JsError(s"Missing type discriminator")
