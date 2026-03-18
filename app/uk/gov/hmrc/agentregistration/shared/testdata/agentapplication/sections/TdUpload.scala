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

package uk.gov.hmrc.agentregistration.shared.testdata.agentapplication.sections

import play.api.libs.json.JsObject
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import sttp.model.Uri
import sttp.model.Uri.UriContext
import uk.gov.hmrc.agentregistration.shared.upload.UploadId
import uk.gov.hmrc.agentregistration.shared.upscan.ErrorDetails
import uk.gov.hmrc.agentregistration.shared.upscan.FileUploadReference
import uk.gov.hmrc.agentregistration.shared.upscan.Upload
import uk.gov.hmrc.agentregistration.shared.upscan.UploadEventDetails
import uk.gov.hmrc.agentregistration.shared.upscan.UploadNotificationRequest
import uk.gov.hmrc.agentregistration.shared.upscan.UploadStatus
import uk.gov.hmrc.agentregistration.shared.testdata.TdBase

trait TdUpload { dependencies: TdBase =>

  def uploadId: UploadId = UploadId("upload-id-12345")

  def fileUploadReference: FileUploadReference = FileUploadReference("test-file-reference")
  def downloadUrl: Uri = uri"https://bucketName.s3.eu-west-2.amazonaws.com?1235676"
  def checksum: String = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
  def mimeType: String = "application/pdf"
  def fileName: String = "evidence.pdf"
  def sizeInBytes: Long = 12345L
  def upscanFailureReason: String = "QUARANTINE"
  def upscanUnknownFailureReason: String = "UNKNOWN"
  def upscanFailureMessage: String = "Suspicious file uploaded."
  def objectStoreLocation: String = s"agent-registration-frontend/9d5ddeed-d26e-4005-97ca-e40f2466e0a3/$fileName"

  def uploadNotificationRequestSucceeded: UploadNotificationRequest.Succeeded = UploadNotificationRequest.Succeeded(
    reference = fileUploadReference,
    downloadUrl = downloadUrl,
    uploadDetails = UploadEventDetails(
      uploadTimestamp = dependencies.nowAsInstant,
      checksum = checksum,
      fileMimeType = mimeType,
      fileName = fileName,
      size = sizeInBytes
    )
  )

  def uploadNotificationRequestSucceededJson: JsValue = Json.parse(
    // language=JSON
    s"""{
       |  "reference": "${fileUploadReference.value}",
       |  "downloadUrl": "${downloadUrl.toString}",
       |  "fileStatus": "READY",
       |  "uploadDetails": {
       |      "fileName": "$fileName",
       |      "fileMimeType": "$mimeType",
       |      "uploadTimestamp": "${dependencies.nowAsInstant}",
       |      "checksum": "$checksum",
       |      "size": $sizeInBytes
       |  }
       |}
       |""".stripMargin
  )

  def uploadNotificationRequestFailed = UploadNotificationRequest.Failed(
    reference = fileUploadReference,
    failureDetails = ErrorDetails(
      failureReason = upscanFailureReason,
      message = upscanFailureMessage
    )
  )

  def uploadInProgress: Upload = Upload(
    _id = uploadId,
    internalUserId = dependencies.internalUserId,
    createdAt = dependencies.nowAsInstant,
    fileUploadReference = fileUploadReference,
    uploadStatus = UploadStatus.InProgress
  )

  def uploadUploadedSuccessfully: Upload = uploadInProgress.copy(
    uploadStatus = UploadStatus.UploadedSuccessfully(
      downloadUrl = downloadUrl,
      fileName = fileName,
      mimeType = mimeType,
      sizeInBytes = sizeInBytes,
      checksum = checksum
    )
  )

  def uploadFailed: Upload = uploadInProgress.copy(
    uploadStatus = UploadStatus.Failed(
      failureReason = upscanUnknownFailureReason,
      messageFromUpscan = upscanFailureMessage
    )
  )

  def uploadFailedWithVirus: Upload = uploadInProgress.copy(
    uploadStatus = UploadStatus.Failed(
      failureReason = upscanFailureReason,
      messageFromUpscan = upscanFailureMessage
    )
  )

  def objectStoreUploadResponse: JsObject = Json.obj(
    "location" -> objectStoreLocation,
    "contentLength" -> sizeInBytes,
    "contentMD5" -> "a3c2f1e38701bd2c7b54ebd7b1cd0dbc",
    "lastModified" -> dependencies.nowAsInstant
  )

}
