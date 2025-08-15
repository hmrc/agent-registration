/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.agentregistration.testsupport.wiremock

import com.github.tomakehurst.wiremock.client.MappingBuilder
import com.github.tomakehurst.wiremock.client.WireMock as wm
import com.github.tomakehurst.wiremock.http.HttpHeader
import com.github.tomakehurst.wiremock.http.HttpHeaders
import com.github.tomakehurst.wiremock.matching.ContentPattern
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder
import com.github.tomakehurst.wiremock.matching.StringValuePattern
import com.github.tomakehurst.wiremock.matching.UrlPattern
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.http.Status

import scala.jdk.CollectionConverters.MapHasAsJava
import scala.jdk.CollectionConverters.SeqHasAsJava

object StubMaker:

  sealed trait HttpMethod

  object HttpMethod:

    case object GET
    extends HttpMethod

    case object POST
    extends HttpMethod

    case object DELETE
    extends HttpMethod

    case object PUT
    extends HttpMethod

  def make(
    httpMethod: HttpMethod = HttpMethod.GET,
    urlPattern: UrlPattern = wm.urlPathEqualTo("/example/path"),
    queryParams: Map[String, StringValuePattern] = Map.empty,
    requestHeaders: Seq[(String, StringValuePattern)] = Nil,
    requestBody: Option[ContentPattern[?]] = None, // for example equalToJson(Json.prettyPrint(Json.toJson(caseClass)))
    responseBody: String = "",
    responseStatus: Int = Status.OK,
    responseHeaders: Seq[(String, String)] = Nil,
    atPriority: Option[Integer] = None
  ): StubMapping =
    val builder = requestHeaders
      .foldLeft(initialMappingBuilder(httpMethod)(urlPattern))((acc, c) => acc.withHeader(c._1, c._2))
      .withQueryParams(queryParams.asJava)

    val withRequestBody =
      requestBody match
        case Some(body) => builder.withRequestBody(body)
        case None => builder

    val withPriority =
      atPriority match
        case Some(priority) => withRequestBody.atPriority(priority)
        case None => withRequestBody

    val withResponse = withPriority.willReturn(
      wm.aResponse()
        .withStatus(responseStatus)
        .withBody(responseBody)
        .withHeaders(new HttpHeaders(responseHeaders.map(t => new HttpHeader(t._1, t._2)).asJava))
    )

    wm.stubFor(withResponse)

  def verify(
    httpMethod: HttpMethod = HttpMethod.GET,
    urlPattern: UrlPattern = wm.urlPathEqualTo("/example/path"),
    queryParams: Map[String, StringValuePattern] = Map.empty,
    requestHeaders: Seq[(String, StringValuePattern)] = Nil,
    count: Int = 1
  ): Unit =
    val basePattern =
      requestHeaders
        .foldLeft(initialRequestPatternBuilder(httpMethod)(urlPattern))((acc, c) => acc.withHeader(c._1, c._2))

    val patternWithParams = queryParams.foldLeft(basePattern)((acc, c) => acc.withQueryParam(c._1, c._2))

    wm.verify(wm.exactly(count), patternWithParams)

  private def initialMappingBuilder(httpMethod: HttpMethod): UrlPattern => MappingBuilder =
    httpMethod match
      case HttpMethod.GET => wm.get
      case HttpMethod.POST => wm.post
      case HttpMethod.DELETE => wm.delete
      case HttpMethod.PUT => wm.put

  private def initialRequestPatternBuilder(httpMethod: HttpMethod): UrlPattern => RequestPatternBuilder =
    httpMethod match
      case HttpMethod.GET => wm.getRequestedFor
      case HttpMethod.POST => wm.postRequestedFor
      case HttpMethod.DELETE => wm.deleteRequestedFor
      case HttpMethod.PUT => wm.putRequestedFor
