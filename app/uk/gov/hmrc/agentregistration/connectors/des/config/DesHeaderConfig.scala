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

package uk.gov.hmrc.agentregistration.connectors.des.config

import uk.gov.hmrc.agentregistration.config.AppConfig
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.HeaderNames

import java.net.URL
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

final case class HeadersConfig(
  explicitHeaders: Seq[(String, String)]
)

@Singleton
class DesHeaderConfig @Inject() (
  appConfig: AppConfig
):

  private val environment: String = appConfig.desEnvironment
  val authToken: String = appConfig.desAuthToken

  def makeHeaders(url: URL)(implicit hc: HeaderCarrier): HeadersConfig = {
    // determine if the request is being sent to an internal host (e.g. stubs) to avoid sending unnecessary headers
    // that are required when sending an off-platform request to DES
    val isInternalHost = appConfig.internalHostPatterns.exists(_.pattern.matcher(url.getHost).matches())
    val baseHeaders = Seq(
      "Environment" -> s"$environment",
      "CorrelationId" -> UUID.randomUUID().toString
    )
    val additionalHeaders =
      if (isInternalHost)
        Seq.empty
      else
        Seq(
          HeaderNames.authorisation -> s"Bearer $authToken",
          HeaderNames.xRequestId -> hc.requestId.map(_.value).getOrElse(UUID.randomUUID().toString)
        ) ++ hc.sessionId.fold(Seq.empty[(String, String)])(x => Seq(HeaderNames.xSessionId -> x.value))

    HeadersConfig(
      baseHeaders ++ additionalHeaders
    )
  }
