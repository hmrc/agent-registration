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

package uk.gov.hmrc.agentregistration.controllers

import play.api.libs.json.*
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.ControllerComponents
import play.api.mvc.Request
import play.api.mvc.RequestHeader
import uk.gov.hmrc.agentregistration.action.Actions
import uk.gov.hmrc.agentregistration.connectors.des.BusinessPartnerRecordConnector
import uk.gov.hmrc.agentregistration.repository.UploadDetailsRepo
import uk.gov.hmrc.agentregistration.shared.Utr
import uk.gov.hmrc.agentregistration.shared.upscan.FileUploadReference
import uk.gov.hmrc.agentregistration.shared.upscan.UploadId
import uk.gov.hmrc.agentregistration.shared.upscan.UploadStatus
import uk.gov.hmrc.agentregistration.shared.util.HttpUrlFormat
import uk.gov.hmrc.agentregistration.util.RequestAwareLogging
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import java.net.URL
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

sealed trait CallbackBody:
  def reference: FileUploadReference

final case class ReadyCallbackBody(
  reference: FileUploadReference,
  downloadUrl: URL,
  uploadDetails: UploadEventDetails
)
extends CallbackBody

final case class FailedCallbackBody(
  reference: FileUploadReference,
  failureDetails: ErrorDetails
)
extends CallbackBody

object CallbackBody:

  given Reads[UploadEventDetails] = Json.reads[UploadEventDetails]
  given Reads[ErrorDetails] = Json.reads[ErrorDetails]

  given Reads[ReadyCallbackBody] =
    given Format[URL] = HttpUrlFormat.format

    Json.reads[ReadyCallbackBody]

  given Reads[FailedCallbackBody] = Json.reads[FailedCallbackBody]

  given Reads[CallbackBody] =
    (json: JsValue) =>
      json \ "fileStatus" match
        case JsDefined(JsString("READY")) => json.validate[ReadyCallbackBody]
        case JsDefined(JsString("FAILED")) => json.validate[FailedCallbackBody]
        case JsDefined(value) => JsError(s"Invalid type discriminator: $value")
        case _ => JsError(s"Missing type discriminator")

final case class ErrorDetails(
  failureReason: String,
  message: String
)

final case class UploadEventDetails(
  uploadTimestamp: Instant,
  checksum: String,
  fileMimeType: String,
  fileName: String,
  size: Long
)

@Singleton()
final class UpscanCallbackController @Inject() (
  cc: ControllerComponents,
  actions: Actions,
  uploadDetailsRepo: UploadDetailsRepo
)
extends BackendController(cc)
with RequestAwareLogging:

  given ExecutionContext = controllerComponents.executionContext

  def callback: Action[JsValue] =
    Action.async(parse.json): request =>
      given Request[JsValue] = request

      withJsonBody[CallbackBody]: feedback =>
        handleCallback(feedback).map(_ => Ok)

  private def handleCallback(callback: CallbackBody)(using
    RequestHeader,
    HeaderCarrier
  ): Future[Unit] =
    val uploadStatus =
      callback match
        case s: ReadyCallbackBody =>
          UploadStatus.UploadedSuccessfully(
            name = s.uploadDetails.fileName,
            mimeType = s.uploadDetails.fileMimeType,
            downloadUrl = s.downloadUrl,
            size = Some(s.uploadDetails.size),
            checksum = s.uploadDetails.checksum
          )
        case _: FailedCallbackBody => UploadStatus.Failed

    logger.debug(s"Upscan callback received for reference ${callback.reference} with status $uploadStatus")
    uploadDetailsRepo
      .updateStatus(callback.reference, uploadStatus)
      .map(_ => ())
