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
import uk.gov.hmrc.agentregistration.action.Actions
import uk.gov.hmrc.agentregistration.repository.UploadRepo
import uk.gov.hmrc.agentregistration.shared.upscan.Upload
import uk.gov.hmrc.agentregistration.shared.upscan.UploadId
import uk.gov.hmrc.agentregistration.shared.upscan.UploadStatus
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext

@Singleton()
final class UpscanProgressController @Inject() (
  cc: ControllerComponents,
  actions: Actions,
  uploadDetailsRepo: UploadRepo
)
extends BackendController(cc):

  given ExecutionContext = controllerComponents.executionContext

  def initiate: Action[Upload] =
    actions.authorised.async(parse.json[Upload]):
      implicit request =>
        val uploadDetails: Upload = request.body
        uploadDetailsRepo
          .upsert(uploadDetails)
          .map(_ => Created)

  def getUpscanStatus(uploadId: UploadId): Action[AnyContent] = actions.authorised.async:
    implicit request =>
      uploadDetailsRepo
        .find(uploadId)
        .map:
          case Some(details) => Ok(Json.toJson(details.status))
          case None => NotFound("Upload details not found")
