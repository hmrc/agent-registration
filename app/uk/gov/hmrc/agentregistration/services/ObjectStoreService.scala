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
import uk.gov.hmrc.agentregistration.shared.amls.AmlsEvidence
import uk.gov.hmrc.agentregistration.util.ProcessInSequence
import uk.gov.hmrc.agentregistration.util.RequestAwareLogging
import uk.gov.hmrc.agentregistration.util.RequestSupport.hc
import uk.gov.hmrc.objectstore.client.ObjectListing
import uk.gov.hmrc.objectstore.client.Path
import uk.gov.hmrc.objectstore.client.play.PlayObjectStoreClient

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@Singleton
class ObjectStoreService @Inject() (
  playObjectStoreClient: PlayObjectStoreClient
)(using ec: ExecutionContext)
extends RequestAwareLogging:

  def deleteAmlsEvidence(amlsEvidence: AmlsEvidence)(using request: RequestHeader): Future[Unit] =
    val directory: Path.Directory = AmlsEvidence.objectStoreDirectory(amlsEvidence.fileUploadReference)
    for
      objectListing: ObjectListing <- playObjectStoreClient.listObjects(directory, owner = AmlsEvidence.objectStoreOwner)
      _ <-
        ProcessInSequence.processInSequence(objectListing.objectSummaries): objectSummary =>
          val file: Path.File = directory.file(objectSummary.location.fileName)
          playObjectStoreClient
            .deleteObject(file, owner = AmlsEvidence.objectStoreOwner)
            .map(_ => logger.info(s"Deleted AMLS evidence ${AmlsEvidence.objectStoreOwner}/${file.asUri}"))
    yield ()
