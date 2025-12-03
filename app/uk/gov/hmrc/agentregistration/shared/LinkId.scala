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

import play.api.libs.json.Format
import play.api.mvc.PathBindable
import uk.gov.hmrc.agentregistration.shared.util.JsonFormatsFactory
import uk.gov.hmrc.agentregistration.shared.util.ValueClassBinder

import java.util.UUID
import javax.inject.Singleton

/** LinkId is a unique identifier used by the frontend to build shareable web links for distribution to the signatories of the application
  */
final case class LinkId(value: String)

object LinkId:

  given format: Format[LinkId] = JsonFormatsFactory.makeValueClassFormat
  given pathBindable: PathBindable[LinkId] = ValueClassBinder.valueClassBinder[LinkId](_.value)

@Singleton
class LinkIdGenerator:
  def nextLinkId(): LinkId = LinkId(UUID.randomUUID().toString)
