/*
 * Copyright 2026 HM Revenue & Customs
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
import uk.gov.hmrc.agentregistration.connectors.hip.HipConnector
import uk.gov.hmrc.agentregistration.shared.Nino
import uk.gov.hmrc.agentregistration.shared.SaUtr
import uk.gov.hmrc.agentregistration.shared.Utr

import javax.inject.Inject
import javax.inject.Singleton

@Singleton()
class UcrController @Inject() (
  cc: ControllerComponents,
  actions: Actions,
  hipConnector: HipConnector
)
extends BackendController(cc):

  def searchByNino(nino: Nino): Action[AnyContent] = actions
    .authorised
    .async:
      implicit request =>
        hipConnector
          .searchByIdentifier(nino)
          .map(ucrIdentifiers => Ok(Json.toJson(ucrIdentifiers)))

  def searchBySaUtr(saUtr: SaUtr): Action[AnyContent] = actions
    .authorised
    .async:
      implicit request =>
        hipConnector
          .searchByIdentifier(saUtr)
          .map(ucrIdentifiers => Ok(Json.toJson(ucrIdentifiers)))

  def searchOrganisationByUtr(utr: Utr): Action[AnyContent] = actions
    .authorised
    .async:
      implicit request =>
        hipConnector
          .searchOrganisationByIdentifier(utr)
          .map(ucrIdentifiers => Ok(Json.toJson(ucrIdentifiers)))
