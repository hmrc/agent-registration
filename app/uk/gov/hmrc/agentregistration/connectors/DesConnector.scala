/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.agentregistration.connectors

import play.api.http.Status.*
import play.api.libs.json.*
import play.api.libs.ws.WSBodyWritables.writeableOf_JsValue
import play.api.mvc.RequestHeader
import uk.gov.hmrc.agentregistration.config.AppConfig
import uk.gov.hmrc.agentregistration.shared.DesBusinessAddress
import uk.gov.hmrc.agentregistration.shared.DesRegistrationResponse
import uk.gov.hmrc.agentregistration.shared.Utr
import uk.gov.hmrc.agentregistration.util.RequestSupport.given
import uk.gov.hmrc.http.*
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.client.HttpClientV2

import java.net.URL
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

final case class DesRegistrationRequest(
  requiresNameMatch: Boolean = false,
  regime: String = "ITSA",
  isAnAgent: Boolean
)

object DesRegistrationRequest {
  implicit val formats: Format[DesRegistrationRequest] = Json.format[DesRegistrationRequest]
}

final case class HeadersConfig(
  hc: HeaderCarrier,
  explicitHeaders: Seq[(String, String)]
)

@Singleton
class DesConnector @Inject() (
  appConfig: AppConfig,
  http: HttpClientV2
)(using val ec: ExecutionContext) {

  val baseUrl: String = appConfig.desBaseUrl
  private val environment: String = appConfig.desEnvironment
  val authToken: String = appConfig.desAuthToken

  private val Environment = "Environment"
  private val CorrelationId = "CorrelationId"

  def getBusinessPartnerRecord(
    utr: Utr
  )(implicit
    rh: RequestHeader
  ): Future[Option[DesRegistrationResponse]] = getRegistrationJson(utr).map {
    case Some(r) =>
      println(s"DES response JSON: $r")
      Some(
        DesRegistrationResponse(
          organisationName = (r \ "organisation" \ "organisationName").asOpt[String],
          address =
            (r \ "address").validate[DesBusinessAddress] match {
              case JsSuccess(value, _) => value
              case JsError(_) => throw new Exception("DES response has a bad address format")
            },
          emailAddress = (r \ "agencyDetails" \ "agencyEmail")
            .asOpt[String]
            .orElse((r \ "contactDetails" \ "emailAddress").asOpt[String]),
          primaryPhoneNumber = (r \ "contactDetails" \ "primaryPhoneNumber").asOpt[String]
        )
      )
    case _ => None
  }

  private def getRegistrationJson(
    utr: Utr
  )(implicit rh: RequestHeader): Future[Option[JsValue]] = {
    val url: URL = url"$baseUrl/registration/individual/utr/${utr.value}"
    val headersConfig = makeHeadersConfig(url)
    http
      .post(url)
      .setHeader(headersConfig.explicitHeaders*)
      .withBody(Json.toJson(DesRegistrationRequest(isAnAgent = false)))
      .execute[HttpResponse]
      .map { response =>
        response.status match {
          case OK => Option(response.json)
          case NOT_FOUND => None
          case error =>
            throw UpstreamErrorResponse(
              s"[DES-GetAgentRegistration-POST] returned status: $error",
              INTERNAL_SERVER_ERROR
            )
        }
      }
      .recover { case badRequest: BadRequestException => throw new Exception(s"400 Bad Request response from DES for utr ${utr.value}", badRequest) }
  }

  private def makeHeadersConfig(url: URL)(implicit hc: HeaderCarrier): HeadersConfig = {
    val isInternalHost = appConfig.internalHostPatterns.exists(_.pattern.matcher(url.getHost).matches())
    val baseHeaders = Seq(
      Environment -> s"$environment",
      CorrelationId -> UUID.randomUUID().toString
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
      if (isInternalHost) then hc.copy(authorization = Some(Authorization(s"Bearer $authToken"))) else hc,
      baseHeaders ++ additionalHeaders
    )
  }

}
