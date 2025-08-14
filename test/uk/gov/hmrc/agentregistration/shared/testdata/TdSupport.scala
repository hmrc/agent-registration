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

package uk.gov.hmrc.agentregistration.shared.testdata

import play.api.test.FakeRequest
import uk.gov.hmrc.http.HeaderNames
import uk.gov.hmrc.http.SessionKeys

import java.time.LocalDate

object TdSupport:

  extension [T](r: FakeRequest[T])

    def withAuthToken(authToken: String = "Bearer f25e5168-04b7-44d5-9dc6-460b2656e2b3"): FakeRequest[T] = r.withHeaders((HeaderNames.authorisation, authToken))

    def withAkamaiReputationHeader(
      akamaiReputationValue: String = "akamai-reputation-value-123"
    ): FakeRequest[T] = r.withHeaders(HeaderNames.akamaiReputation -> akamaiReputationValue)

    def withRequestId(requestId: String = "request-id-value-123"): FakeRequest[T] = r.withHeaders(HeaderNames.xRequestId -> requestId)

    def withTrueClientIp(ip: String = "client-ip-123"): FakeRequest[T] = r.withHeaders(HeaderNames.trueClientIp -> ip)

    def withTrueClientPort(port: String = "client-port-123"): FakeRequest[T] = r.withHeaders(HeaderNames.trueClientPort -> port)

    def withDeviceId(deviceId: String = "device-id-123"): FakeRequest[T] = r.withHeaders(HeaderNames.deviceID -> deviceId)

  given [T]: Conversion[T, Option[T]] with
    def apply(t: T): Option[T] = Some(t)

  given Conversion[String, LocalDate] with
    def apply(s: String): LocalDate = LocalDate.parse(s)

  given Conversion[String, Option[LocalDate]] with
    def apply(s: String): Option[LocalDate] = Some(LocalDate.parse(s))
