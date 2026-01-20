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

package uk.gov.hmrc.agentregistration.shared.llp

import org.bson.types.ObjectId
import play.api.libs.json.Format
import uk.gov.hmrc.agentregistration.shared.util.JsonFormatsFactory

import javax.inject.Singleton

/** Individual provided details Identifier, which is unique for an member provided details
  */
final case class IndividualProvidedDetailsId(value: String)

object IndividualProvidedDetailsId:
  given format: Format[IndividualProvidedDetailsId] = JsonFormatsFactory.makeValueClassFormat

@Singleton
class IndividualProvidedDetailsIdGenerator:
  def nextIndividualProvidedDetailsId(): IndividualProvidedDetailsId = IndividualProvidedDetailsId(ObjectId.get().toHexString)
