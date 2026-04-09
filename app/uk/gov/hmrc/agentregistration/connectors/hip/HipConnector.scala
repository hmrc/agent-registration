/*
 * Copyright 2026 HM Revenue & Customs
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

package uk.gov.hmrc.agentregistration.connectors.hip

import play.api.http.Status.*
import play.api.libs.json.*
import play.api.libs.ws.WSBodyWritables.writeableOf_JsValue
import play.api.mvc.RequestHeader
import uk.gov.hmrc.agentregistration.config.AppConfig
import uk.gov.hmrc.agentregistration.shared.Nino
import uk.gov.hmrc.agentregistration.shared.PayeRef
import uk.gov.hmrc.agentregistration.shared.SaUtr
import uk.gov.hmrc.agentregistration.shared.UcrIdentifiers
import uk.gov.hmrc.agentregistration.shared.Utr
import uk.gov.hmrc.agentregistration.shared.Vrn
import uk.gov.hmrc.agentregistration.util.RequestSupport.given
import uk.gov.hmrc.http.*
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.client.HttpClientV2

import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

/** Connector for the UCR (Unified Customer Registry) Customer API v2, accessed via HIP (Hybrid Integration Platform).
  *
  * Provides individual and organisation identifier searches against the UCR.
  *
  * @see
  *   https://admin.tax.service.gov.uk/api-hub/apis/details/ucr-customer-api-v2
  */
@Singleton
class HipConnector @Inject() (
  appConfig: AppConfig,
  hipHeaders: HipHeaders,
  http: HttpClientV2
)(using val ec: ExecutionContext):

  private val baseUrl: String = appConfig.hipBaseUrl

  /** UCR Customer API v2 - Search Individual By Identifier. Searches for an individual's VRNs and PAYE refs (EMPREFs) by NINO or SA-UTR.
    * @see
    *   https://admin.tax.service.gov.uk/api-hub/apis/details/ucr-customer-api-v2 "Search Individual By Identifier"
    */
  def searchByIdentifier(
    identifier: Nino | SaUtr
  )(implicit
    rh: RequestHeader
  ): Future[UcrIdentifiers] =
    val apiUrl: URL = url"$baseUrl/customer/v2/api/individuals/identifier-search"

    val (identifierType, identifierValue) =
      identifier match
        case nino: Nino => ("NINO", nino.value)
        case saUtr: SaUtr => ("UTR", saUtr.value)

    val requestBody = Json.obj(
      "identifier" -> Json.obj(
        "type" -> identifierType,
        "value" -> identifierValue
      ),
      "excludeDeceased" -> true,
      "registryMarker" -> "GREEN"
    )

    http
      .post(apiUrl)
      .setHeader(hipHeaders.makeHeaders()*)
      .withBody(requestBody)
      .execute[HttpResponse]
      .map: response =>
        response.status match
          case s if HttpErrorFunctions.is2xx(s) => extractIdentifiers(response.json)
          case status =>
            throw UpstreamErrorResponse(
              s"[HIP-UCR-IdentifierSearch-POST] returned status: $status",
              INTERNAL_SERVER_ERROR
            )

  /** UCR Customer API v2 - Search Organisation By Identifier. Searches for an organisation's VRNs and PAYE refs (EMPREFs) by UTR.
    * @see
    *   https://admin.tax.service.gov.uk/api-hub/apis/details/ucr-customer-api-v2 "Search Organisation By Identifier"
    */
  def searchOrganisationByIdentifier(
    utr: Utr
  )(implicit
    rh: RequestHeader
  ): Future[UcrIdentifiers] =
    val apiUrl: URL = url"$baseUrl/customer/v2/api/organisations/identifier-search"

    val requestBody = Json.obj(
      "identifier" -> Json.obj(
        "type" -> "UTR",
        "value" -> utr.value
      ),
      "registryMarker" -> "GREEN"
    )

    http
      .post(apiUrl)
      .setHeader(hipHeaders.makeHeaders()*)
      .withBody(requestBody)
      .execute[HttpResponse]
      .map: response =>
        response.status match
          case s if HttpErrorFunctions.is2xx(s) => extractIdentifiers(response.json)
          case status =>
            throw UpstreamErrorResponse(
              s"[HIP-UCR-OrganisationIdentifierSearch-POST] returned status: $status",
              INTERNAL_SERVER_ERROR
            )

  private def extractIdentifiers(json: JsValue): UcrIdentifiers =
    val results = (json \ "results").as[List[JsValue]]
    val identifiers = results.flatMap: result =>
      (result \ "identifiers").as[List[JsValue]].map: id =>
        val idType = (id \ "identifier" \ "type").as[String]
        val idValue = (id \ "identifier" \ "value").as[String]
        (idType, idValue)
    val vrns = identifiers.collect { case ("VRN", value) => Vrn(value) }
    val payeRefs = identifiers.collect { case ("EMPREF", value) => PayeRef(value) }
    UcrIdentifiers(vrns = vrns, payeRefs = payeRefs)
