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

import play.api.libs.json.Json
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.ControllerComponents
import uk.gov.hmrc.agentregistration.action.Actions
import uk.gov.hmrc.agentregistration.action.AuthorisedRequest
import uk.gov.hmrc.agentregistration.repository.AgentApplicationRepo
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.AgentApplicationId
import uk.gov.hmrc.agentregistration.shared.LinkId
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.=!=
import uk.gov.hmrc.auth.core.AuthorisationException
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext

@Singleton()
class ApplicationController @Inject() (
  cc: ControllerComponents,
  actions: Actions,
  agentApplicationRepo: AgentApplicationRepo
)
extends BackendController(cc):

  given ExecutionContext = controllerComponents.executionContext

  def findApplication: Action[AnyContent] = actions.authorised.async: (request: AuthorisedRequest[AnyContent]) =>
    agentApplicationRepo
      .findByInternalUserId(request.internalUserId)
      .map:
        case Some(agentApplication) => Ok(Json.toJson(agentApplication))
        case None => NoContent

  def upsertApplication: Action[AgentApplication] =
    actions
      .authorised
      .async(parse.json[AgentApplication]):
        implicit request =>
          val agentApplication: AgentApplication = request.body
          ensureInternalUserId(agentApplication)
          agentApplicationRepo
            .upsert(request.body)
            .map(_ => Ok(""))

  def findApplicationByLinkId(linkId: LinkId): Action[AnyContent] = Action.async: request =>
    agentApplicationRepo
      .findByLinkId(linkId)
      .map:
        case Some(agentApplication) => Ok(Json.toJson(agentApplication))
        case None => NoContent

  def findApplicationById(agentApplicationId: AgentApplicationId): Action[AnyContent] = actions.individualAuthorised.async: request =>
    agentApplicationRepo
      .findById(agentApplicationId)
      .map:
        case Some(agentApplication) => Ok(Json.toJson(agentApplication))
        case None => NoContent

  private def ensureInternalUserId(agentApplication: AgentApplication)(using request: AuthorisedRequest[?]): Unit =
    if agentApplication.internalUserId =!= request.internalUserId then
      throw AuthorisationException.fromString(
        s"InternalUserId in request body (${agentApplication.internalUserId.value}) does not match InternalUserId from enrolments (${request.internalUserId.value})"
      )
    else ()
