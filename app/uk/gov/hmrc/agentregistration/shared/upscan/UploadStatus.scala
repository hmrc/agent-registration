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
import uk.gov.hmrc.http.StringContextOps

import java.net.URL

sealed trait UploadStatus
object UploadStatus:

  case object InProgress
  extends UploadStatus
  final case class Failed(failureReason: String)
  extends UploadStatus
  final case class UploadedSuccessfully(
    name: String,
    mimeType: String,
    downloadUrl: URL,
    size: Option[Long],
    checksum: String
  )
  extends UploadStatus

  implicit val urlReads: Reads[URL] = Reads {
    case JsString(s) =>
      try JsSuccess(url"$s")
      catch {
        case e: Exception => JsError(s"Invalid URL: $s")
      }
    case _ => JsError("URL must be a string")
  }

  implicit val uploadedSuccessfullyReads: Reads[UploadedSuccessfully] = Json.reads[UploadedSuccessfully]
  implicit val failedReads: Reads[Failed] = Json.reads[Failed]

  implicit val reads: Reads[UploadStatus] = Reads { json =>
    (json \ "status").validate[String].flatMap {
      case "InProgress" => JsSuccess(InProgress)
      case "Failed" => failedReads.reads(json)
      case "UploadedSuccessfully" => uploadedSuccessfullyReads.reads(json)
      case other => JsError(s"Unknown type: $other")
    }
  }

  implicit val urlWrites: Writes[URL] = Writes(url => JsString(url.toString))
  implicit val uploadedSuccessfullyWrites: Writes[UploadedSuccessfully] = Json.writes[UploadedSuccessfully]
  implicit val failedWrites: Writes[Failed] = Json.writes[Failed]

  implicit val writes: Writes[UploadStatus] = Writes {
    case InProgress => Json.obj("status" -> "InProgress")
    case u: Failed => failedWrites.writes(u).as[JsObject] + ("status" -> JsString("Failed"))
    case u: UploadedSuccessfully => uploadedSuccessfullyWrites.writes(u).as[JsObject] + ("status" -> JsString("UploadedSuccessfully"))
  }
