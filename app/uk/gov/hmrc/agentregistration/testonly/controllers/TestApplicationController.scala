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

package uk.gov.hmrc.agentregistration.testonly.controllers

import org.mongodb.scala.ObservableFuture
import org.mongodb.scala.model.Sorts
import play.api.libs.json.Json
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.ControllerComponents
import uk.gov.hmrc.agentregistration.action.Actions
import uk.gov.hmrc.agentregistration.repository.AgentApplicationRepo
import uk.gov.hmrc.agentregistration.repository.providedetails.llp.IndividualProvidedDetailsRepo
import uk.gov.hmrc.agentregistration.runner.ExpireUnsubmittedApplicationsRunner
import uk.gov.hmrc.agentregistration.shared.*
import uk.gov.hmrc.agentregistration.shared.individual.*
import uk.gov.hmrc.agentregistration.testonly.util.TestMongoCleanup
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext

@Singleton()
class TestApplicationController @Inject() (
  cc: ControllerComponents,
  actions: Actions,
  agentApplicationRepo: AgentApplicationRepo,
  individualProvidedDetailsRepo: IndividualProvidedDetailsRepo,
  expireUnsubmittedApplicationsRunner: ExpireUnsubmittedApplicationsRunner,
  testMongoCleanup: TestMongoCleanup
)
extends BackendController(cc):

  given ExecutionContext = controllerComponents.executionContext

  def upsertIndividualProvidedDetails: Action[IndividualProvidedDetails] =
    actions
      .default
      .async(parse.json[IndividualProvidedDetails]):
        implicit request =>
          individualProvidedDetailsRepo
            .upsert(request.body)
            .map(_ => Ok(""))

  def upsertApplication: Action[AgentApplication] =
    actions
      .default
      .async(parse.json[AgentApplication]):
        implicit request =>
          agentApplicationRepo
            .upsert(request.body)
            .map(_ => Ok(""))

  def runExpiryScheduler: Action[AnyContent] = actions
    .default
    .async: _ =>
      expireUnsubmittedApplicationsRunner.run().map(_ => Ok(""))

  def recentApplications(
    page: Int,
    pageSize: Int
  ): Action[AnyContent] = actions
    .default
    .async:
      implicit request =>
        val safePage = math.max(page, 1)
        val safePageSize = math.min(math.max(pageSize, 1), 100)
        agentApplicationRepo
          .collection
          .find()
          .sort(Sorts.descending("createdAt"))
          .skip((safePage - 1) * safePageSize)
          .limit(safePageSize)
          .toFuture()
          .map((recentApplications: Seq[AgentApplication]) => Ok(Json.toJson(recentApplications)))

  def findApplication(agentApplicationId: AgentApplicationId): Action[AnyContent] = actions
    .default
    .async:
      implicit request =>
        agentApplicationRepo
          .findById(agentApplicationId)
          .map:
            case Some(agentApplication) => Ok(Json.toJson(agentApplication))
            case None => NoContent

  def findIndividuals(agentApplicationId: AgentApplicationId): Action[AnyContent] = actions
    .default
    .async:
      implicit request =>
        individualProvidedDetailsRepo
          .findForApplication(agentApplicationId)
          .map: individuals =>
            Ok(Json.toJson(individuals))

  def findIndividual(individualProvidedDetailsId: IndividualProvidedDetailsId): Action[AnyContent] = actions
    .default
    .async:
      implicit request =>
        individualProvidedDetailsRepo
          .findById(individualProvidedDetailsId)
          .map:
            case Some(individualProvidedDetails) => Ok(Json.toJson(individualProvidedDetails))
            case None => NoContent

  def findIndividualByPersonReference(personReference: PersonReference): Action[AnyContent] = actions
    .default
    .async:
      implicit request =>
        individualProvidedDetailsRepo
          .findByPersonReference(personReference)
          .map:
            case Some(individualProvidedDetails) => Ok(Json.toJson(individualProvidedDetails))
            case None => NoContent

  def deleteAllApplications: Action[AnyContent] = actions
    .default
    .async:
      implicit request =>
        for
          _ <- testMongoCleanup.deleteAllApplications
          _ <- testMongoCleanup.deleteAllIndividuals
        yield NoContent
