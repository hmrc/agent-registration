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

package uk.gov.hmrc.agentregistration.shared.risking

import org.bson.types.ObjectId
import play.api.libs.json.Format
import play.api.mvc.PathBindable
import uk.gov.hmrc.agentregistration.shared.util.JsonFormatsFactory
import uk.gov.hmrc.agentregistration.shared.util.ValueClassBinder

import javax.inject.Singleton

/** Person Reference used by Minerva as a unique identifier for an individual
  */
final case class PersonReference(value: String)

object PersonReference:

  given format: Format[PersonReference] = JsonFormatsFactory.makeValueClassFormat
  given pathBindable: PathBindable[PersonReference] = ValueClassBinder.valueClassBinder[PersonReference](_.value)

@Singleton
class PersonReferenceGenerator:
  def nextPersonReference(): PersonReference = PersonReference(ObjectId.get().toHexString)
