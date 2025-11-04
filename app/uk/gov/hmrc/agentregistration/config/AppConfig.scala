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

package uk.gov.hmrc.agentregistration.config

import javax.inject.Inject
import javax.inject.Singleton
import play.api.Configuration
import uk.gov.hmrc.auth.core.Enrolment
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.concurrent.duration.FiniteDuration
import scala.util.matching.Regex

@Singleton
class AppConfig @Inject() (
  servicesConfig: ServicesConfig,
  configuration: Configuration
):

  val appName: String = configuration.get[String]("appName")

  val hmrcAsAgentEnrolment: Enrolment = Enrolment(key = "HMRC-AS-AGENT")
  val desBaseUrl: String = servicesConfig.baseUrl("des")
  val desEnvironment: String = servicesConfig.getString("microservice.services.des.environment")
  val desAuthToken: String = servicesConfig.getString("microservice.services.des.authorization-token")
  val internalHostPatterns: Seq[Regex] = configuration.get[Seq[String]]("internalServiceHostPatterns").map(_.r)

  object AgentApplicationRepo:
    val ttl: FiniteDuration = ConfigHelper.readFiniteDuration("mongodb.application-repo-ttl", servicesConfig)
