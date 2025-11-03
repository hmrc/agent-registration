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

package uk.gov.hmrc.agentregistration.controllers.providedetails

import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.agentregistration.action.providedetails.ProvideDetailsRequest
import uk.gov.hmrc.agentregistration.action.Actions
import uk.gov.hmrc.agentregistration.repository.providedetails.{ProvideDetailsId, ProvidedDetailsRepo}
import uk.gov.hmrc.agentregistration.shared.{LinkId, ProvidedDetails}
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.=!=
import uk.gov.hmrc.auth.core.AuthorisationException
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton()
class ProvideDetailsController @Inject()(
  cc: ControllerComponents,
  actions: Actions,
  providedDetailsRepo: ProvidedDetailsRepo
)
extends BackendController(cc):

  given ExecutionContext = controllerComponents.executionContext

  
  def findProvidedDetails(linkId: LinkId): Action[AnyContent] = actions.individualAuthorised.async: (request: ProvideDetailsRequest[AnyContent]) =>
    providedDetailsRepo
      .findById(ProvideDetailsId(internalUserId = request.internalUserId, linkId = linkId))
      .map:
        case Some(providedDetails) => Ok(Json.toJson(providedDetails))
        case None => NoContent

  //TODO - should we check if there is coresponding application etc 
  val upsertApplication: Action[ProvidedDetails] =
    actions
      .individualAuthorised
      .async(parse.json[ProvidedDetails]):
        implicit request =>
          val providedDetails: ProvidedDetails = request.body
          ensureInternalUserId(providedDetails)
          providedDetailsRepo
            .upsert(request.body)
            .map(_ => Ok(""))

//  def findApplicationByLinkId(linkId: LinkId): Action[AnyContent] = Action.async: request =>
//    providedDetailsRepo
//      .findByLinkId(linkId)
//      .map(providedDetailsList => Ok(Json.toJson(providedDetailsList)))


  private def ensureInternalUserId(providedDetails: ProvidedDetails)(using request: ProvideDetailsRequest[?]): Unit =
    if providedDetails.internalUserId =!= request.internalUserId then
      throw AuthorisationException.fromString(
        s"InternalUserId in request body (${providedDetails.internalUserId.value}) does not match InternalUserId from enrolments (${request.internalUserId.value})"
      )
    else ()
