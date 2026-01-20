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

package uk.gov.hmrc.agentregistration.controllers.providedetails.llp

import play.api.libs.json.Json
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.ControllerComponents
import uk.gov.hmrc.agentregistration.action.Actions
import uk.gov.hmrc.agentregistration.action.providedetails.IndividualAuthorisedRequest
import uk.gov.hmrc.agentregistration.controllers.BackendController
import uk.gov.hmrc.agentregistration.repository.providedetails.llp.IndividualProvidedDetailsRepo
import uk.gov.hmrc.agentregistration.shared.AgentApplicationId
import uk.gov.hmrc.agentregistration.shared.llp.IndividualProvidedDetails
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.=!=
import uk.gov.hmrc.auth.core.AuthorisationException

import javax.inject.Inject
import javax.inject.Singleton

@Singleton()
class IndividualProvidedDetailsController @Inject() (
  cc: ControllerComponents,
  actions: Actions,
  memeberProvidedDetailsRepo: IndividualProvidedDetailsRepo
)
extends BackendController(cc):

  def upsert: Action[IndividualProvidedDetails] =
    actions.individualAuthorised.async(parse.json[IndividualProvidedDetails]):
      implicit request =>
        val individualProvidedDetails: IndividualProvidedDetails = request.body
        ensureInternalUserId(individualProvidedDetails)
        memeberProvidedDetailsRepo
          .upsert(request.body)
          .map(_ => Ok(""))

  def findByAgentApplicationId(agentApplicationId: AgentApplicationId): Action[AnyContent] = actions.individualAuthorised.async: request =>
    memeberProvidedDetailsRepo
      .find(request.internalUserId, agentApplicationId)
      .map {
        case Some(individualProvidedDetails) => Ok(Json.toJson(individualProvidedDetails))
        case None => NoContent
      }

  def findByInternalUserId: Action[AnyContent] = actions.individualAuthorised.async: request =>
    memeberProvidedDetailsRepo
      .findByInternalUserId(request.internalUserId)
      .map:
        case Nil => NoContent
        case individualProvidedDetails => Ok(Json.toJson(individualProvidedDetails))

  private def ensureInternalUserId(individualProvidedDetails: IndividualProvidedDetails)(using request: IndividualAuthorisedRequest[?]): Unit =
    if individualProvidedDetails.internalUserId =!= request.internalUserId then
      throw AuthorisationException.fromString(
        s"InternalUserId in request body (${individualProvidedDetails.internalUserId.value}) does not match InternalUserId from enrolments (${request.internalUserId.value})"
      )
    else ()
