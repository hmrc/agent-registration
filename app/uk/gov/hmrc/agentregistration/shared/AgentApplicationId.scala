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

package uk.gov.hmrc.agentregistration.shared

import org.bson.types.ObjectId
import play.api.libs.json.Format
import play.api.mvc.PathBindable
import uk.gov.hmrc.agentregistration.shared.util.JsonFormatsFactory
import uk.gov.hmrc.agentregistration.shared.util.ValueClassBinder

import javax.inject.Singleton

/** Agent application Identifier, which is unique for an application
  */
final case class AgentApplicationId(value: String)

object AgentApplicationId:

  given format: Format[AgentApplicationId] = JsonFormatsFactory.makeValueClassFormat
  given pathBindable: PathBindable[AgentApplicationId] = ValueClassBinder.valueClassBinder[AgentApplicationId](_.value)

@Singleton
class AgentApplicationIdGenerator:
  def nextApplicationId(): AgentApplicationId = AgentApplicationId(ObjectId.get().toHexString)
