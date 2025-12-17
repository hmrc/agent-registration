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

package uk.gov.hmrc.agentregistration.repository

import org.mongodb.scala.model.FindOneAndUpdateOptions
import org.mongodb.scala.model.IndexModel
import org.mongodb.scala.model.IndexOptions
import org.mongodb.scala.model.Indexes
import uk.gov.hmrc.agentregistration.config.AppConfig
import uk.gov.hmrc.agentregistration.repository.Repo.IdExtractor
import uk.gov.hmrc.agentregistration.repository.Repo.IdString
import uk.gov.hmrc.agentregistration.shared.InternalUserId
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.Codecs
import org.mongodb.scala.model.Filters.equal
import org.mongodb.scala.model.Updates.set
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration
import UploadRepoHelp.given
import uk.gov.hmrc.agentregistration.shared.upscan.FileUploadReference
import uk.gov.hmrc.agentregistration.shared.upscan.Upload
import uk.gov.hmrc.agentregistration.shared.upscan.UploadId
import uk.gov.hmrc.agentregistration.shared.upscan.UploadStatus

@Singleton
final class UploadRepo @Inject() (
  mongoComponent: MongoComponent,
  appConfig: AppConfig
)(using ec: ExecutionContext)
extends Repo[UploadId, Upload](
  collectionName = "upload",
  mongoComponent = mongoComponent,
  indexes = UploadRepoHelp.indexes(appConfig.UploadRepo.ttl),
  extraCodecs = Seq(Codecs.playFormatCodec(Upload.format)),
  replaceIndexes = true
)

// when named it UploadRepo, Scala 3 compiler complains
// about cyclic reference error during compilation ...
object UploadRepoHelp:

  given IdString[UploadId] =
    new IdString[UploadId]:
      override def idString(i: UploadId): String = i.value

  given IdExtractor[Upload, UploadId] =
    new IdExtractor[Upload, UploadId]:
      override def id(upload: Upload): UploadId = upload.uploadId

  def indexes(cacheTtl: FiniteDuration): Seq[IndexModel] = Seq(
    IndexModel(
      keys = Indexes.ascending("lastUpdated"),
      indexOptions = IndexOptions().expireAfter(cacheTtl.toSeconds, TimeUnit.SECONDS).name("lastUpdatedIdx")
    )
  )
