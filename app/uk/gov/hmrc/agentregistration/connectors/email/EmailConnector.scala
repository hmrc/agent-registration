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

package uk.gov.hmrc.agentregistration.connectors.email

import play.api.http.Status.ACCEPTED
import play.api.libs.json.Json
import play.api.libs.ws.WSBodyWritables.writeableOf_JsValue
import play.api.mvc.RequestHeader
import uk.gov.hmrc.agentregistration.config.AppConfig
import uk.gov.hmrc.agentregistration.shared.util.Errors
import uk.gov.hmrc.agentregistration.util.FutureUtil.andLogOnFailure
import uk.gov.hmrc.agentregistration.util.RequestAwareLogging
import uk.gov.hmrc.agentregistration.util.RequestSupport.given
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.http.StringContextOps
import uk.gov.hmrc.http.client.HttpClientV2

import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@Singleton
class EmailConnector @Inject() (
  appConfig: AppConfig,
  http: HttpClientV2
)(using ExecutionContext)
extends RequestAwareLogging:

  private val baseUrl: String = appConfig.emailBaseUrl

  def sendEmail(sendEmailRequest: SendEmailRequest)(using RequestHeader): Future[Unit] =
    val url: URL = url"$baseUrl/hmrc/email"
    http
      .post(url)
      .withBody(Json.toJson(sendEmailRequest))
      .execute[HttpResponse]
      .map: response =>
        response.status match
          case ACCEPTED => ()
          case status =>
            Errors.throwUpstreamErrorResponse(
              httpMethod = "POST",
              url = url,
              status = status,
              response = response,
              info = s"Failed to send email for template ${sendEmailRequest.templateId.id}"
            )
      .andLogOnFailure(s"Failed to send email for template ${sendEmailRequest.templateId.id}")
