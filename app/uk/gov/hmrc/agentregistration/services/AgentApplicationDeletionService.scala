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

package uk.gov.hmrc.agentregistration.services

import play.api.mvc.RequestHeader
import uk.gov.hmrc.agentregistration.repository.AgentApplicationRepo
import uk.gov.hmrc.agentregistration.repository.providedetails.llp.IndividualProvidedDetailsRepo
import uk.gov.hmrc.agentregistration.shared.AgentApplication

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@Singleton
class AgentApplicationDeletionService @Inject() (
  agentApplicationRepo: AgentApplicationRepo,
  individualProvidedDetailsRepo: IndividualProvidedDetailsRepo,
  objectStoreService: ObjectStoreService
)(using ec: ExecutionContext):

  def deleteWithIndividuals(agentApplication: AgentApplication): Future[Unit] =
    for
      _ <- individualProvidedDetailsRepo.deleteByApplicationId(agentApplication.agentApplicationId)
      _ <- agentApplicationRepo.removeById(agentApplication.agentApplicationId)
    yield ()

  def deleteWithIndividualsAndAmlsEvidence(agentApplication: AgentApplication)(using request: RequestHeader): Future[Unit] =
    for
      _ <- deleteAmlsEvidence(agentApplication)
      _ <- deleteWithIndividuals(agentApplication)
    yield ()

  private def deleteAmlsEvidence(agentApplication: AgentApplication)(using request: RequestHeader): Future[Unit] =
    agentApplication.amlsDetails.flatMap(_.amlsEvidence) match
      case Some(amlsEvidence) => objectStoreService.deleteAmlsEvidence(amlsEvidence)
      case None => Future.successful(())
