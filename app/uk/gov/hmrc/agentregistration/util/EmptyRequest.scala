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

package uk.gov.hmrc.agentregistration.util

import play.api.libs.typedmap.TypedMap
import play.api.mvc.Headers
import play.api.mvc.RequestHeader
import play.api.mvc.request.RemoteConnection
import play.api.mvc.request.RequestTarget

object EmptyRequest:

  val emptyRequestHeader: RequestHeader =
    new RequestHeader:
      def target: RequestTarget = RequestTarget(
        uriString = "/",
        path = "scheduledRunner/dummyPath",
        queryString = Map.empty
      )

      def version: String = "HTTP/1.1"

      def method: String = "GET"

      def headers: Headers = Headers()

      def connection: RemoteConnection = RemoteConnection(
        remoteAddress = java.net.InetAddress.getLoopbackAddress,
        secure = false,
        clientCertificateChain = None
      )

      def attrs: TypedMap = TypedMap.empty
