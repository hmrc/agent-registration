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
import uk.gov.hmrc.agentregistration.model.SmuViewerIndividualResponse
import uk.gov.hmrc.agentregistration.repository.AgentApplicationRepo
import uk.gov.hmrc.agentregistration.repository.providedetails.llp.IndividualProvidedDetailsRepo
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetailsId
import uk.gov.hmrc.agentregistration.shared.util.Errors.getOrThrowExpectedDataMissing

import javax.inject.Inject
import javax.inject.Singleton

@Singleton()
class SmuViewerController @Inject() (
  cc: ControllerComponents,
  actions: Actions,
  individualProvidedDetailsRepo: IndividualProvidedDetailsRepo,
  agentApplicationRepo: AgentApplicationRepo
)
extends BackendController(cc):

  // TODO: Find out the correct stride profile and add auth to this endpoint
  def findIndividualByPersonReference(individualProvidedDetailsId: IndividualProvidedDetailsId): Action[AnyContent] = actions.default.async: request =>
    for
      ipd <- individualProvidedDetailsRepo.findById(individualProvidedDetailsId)
      aa <- agentApplicationRepo.findById(ipd.map(_.agentApplicationId).getOrThrowExpectedDataMissing("agentApplicationId"))
    yield (ipd, aa) match
      case (Some(ipd), Some(aa)) if aa.applicationState.sentForRisking =>
        val resp = SmuViewerIndividualResponse.make(ipd, aa)
        Ok(Json.toJson(resp))
      case _ => NoContent
