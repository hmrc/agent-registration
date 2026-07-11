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

package uk.gov.hmrc.agentregistration.shared.util

import play.api.libs.json.*

object JsonConfig:

  /** Default configuration for Play Json */
  val jsonConfiguration: JsonConfiguration = jsonConfiguration()

  val jsonConfigurationForFixes: JsonConfiguration = jsonConfiguration(
    typeNamingF = _.split('.').takeRight(3).mkString(".")
  )

  def jsonConfiguration(
    discriminator: String = "type",
    typeNamingF: String => String = _.split('.').last // Extract just the class name (the argument is the class name)
  ): JsonConfiguration = JsonConfiguration(
    discriminator = discriminator,
    typeNaming = JsonNaming.apply(typeNamingF)
  )
