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
import uk.gov.hmrc.agentregistration.model.IndividualAddDetailsResponse
import uk.gov.hmrc.agentregistration.repository.AgentApplicationRepo
import uk.gov.hmrc.agentregistration.repository.providedetails.llp.IndividualProvidedDetailsRepo
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetails
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetailsId
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.=!=
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.AgentApplicationId
import uk.gov.hmrc.auth.core.AuthorisationException

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.Future

@Singleton()
class IndividualProvidedDetailsController @Inject() (
  cc: ControllerComponents,
  actions: Actions,
  individualProvidedDetailsRepo: IndividualProvidedDetailsRepo,
  agentApplicationRepo: AgentApplicationRepo
)
extends BackendController(cc):

  def upsert: Action[IndividualProvidedDetails] =
    actions.individualAuthorised.async(parse.json[IndividualProvidedDetails]):
      implicit request =>
        val individualProvidedDetails: IndividualProvidedDetails = request.body
        individualProvidedDetailsRepo
          .upsert(request.body)
          .map(_ => Ok(""))

  // removing the internalUserId guard to enable management by an applicant (agent affinity)
  // before the individual signs in and is matched
  def upsertForApplication: Action[IndividualProvidedDetails] =
    actions.authorised.async(parse.json[IndividualProvidedDetails]):
      implicit request =>
        val individualProvidedDetails: IndividualProvidedDetails = request.body
        individualProvidedDetailsRepo
          .upsert(request.body)
          .map(_ => Ok(""))

  def findById(individualProvidedDetailsId: IndividualProvidedDetailsId): Action[AnyContent] = actions.authorised.async: request =>
    individualProvidedDetailsRepo
      .findById(individualProvidedDetailsId)
      .map {
        case Some(individualProvidedDetails) => Ok(Json.toJson(individualProvidedDetails))
        case None => NoContent
      }

  // TODO: Find out the correct stride profile and add auth to this endpoint
  def findByPersonReference(individualProvidedDetailsId: IndividualProvidedDetailsId): Action[AnyContent] = actions.default.async: request =>
    individualProvidedDetailsRepo
      .findById(individualProvidedDetailsId)
      .flatMap {
        case Some(individualProvidedDetails) =>
          agentApplicationRepo.findById(individualProvidedDetails.agentApplicationId).map {
            case Some(agentApp) =>
              val resp = IndividualAddDetailsResponse.from(individualProvidedDetails, agentApp)
              Ok(Json.toJson(resp))
            case None => NoContent
          }
        case None => Future.successful(NoContent)
      }

  def findForApplication(agentApplicationId: AgentApplicationId): Action[AnyContent] = actions.authorised.async: request =>
    individualProvidedDetailsRepo
      .findForApplication(agentApplicationId)
      .map: list =>
        Ok(Json.toJson(list))

  def findForMatchingWithApplication(agentApplicationId: AgentApplicationId): Action[AnyContent] = actions.individualAuthorised.async: request =>
    individualProvidedDetailsRepo
      .findForApplication(agentApplicationId)
      .map: list =>
        Ok(Json.toJson(list))

  private def ensureInternalUserId(individualProvidedDetails: IndividualProvidedDetails)(using request: IndividualAuthorisedRequest[?]): Unit =
    if individualProvidedDetails.getInternalUserId =!= request.internalUserId then
      throw AuthorisationException.fromString(
        s"InternalUserId in request body (${individualProvidedDetails.getInternalUserId.value}) does not match InternalUserId from enrolments (${request.internalUserId.value})"
      )
    else ()

  def deleteById(individualProvidedDetailsId: IndividualProvidedDetailsId): Action[AnyContent] = actions.authorised.async: request =>
    individualProvidedDetailsRepo
      .removeById(individualProvidedDetailsId)
      .map(_ => Ok(""))
