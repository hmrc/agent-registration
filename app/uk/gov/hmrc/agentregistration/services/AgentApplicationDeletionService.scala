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

import uk.gov.hmrc.agentregistration.repository.AgentApplicationRepo
import uk.gov.hmrc.agentregistration.repository.providedetails.llp.IndividualProvidedDetailsRepo
import uk.gov.hmrc.agentregistration.shared.AgentApplicationId

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@Singleton
class AgentApplicationDeletionService @Inject() (
  agentApplicationRepo: AgentApplicationRepo,
  individualProvidedDetailsRepo: IndividualProvidedDetailsRepo
)(using ec: ExecutionContext):

  def deleteWithIndividuals(agentApplicationId: AgentApplicationId): Future[Unit] =
    // clean up orphaned individual provided details records before we lose the application id as well
    for
      _ <- individualProvidedDetailsRepo.deleteByApplicationId(agentApplicationId)
      _ <- agentApplicationRepo.removeById(agentApplicationId)
    yield ()
