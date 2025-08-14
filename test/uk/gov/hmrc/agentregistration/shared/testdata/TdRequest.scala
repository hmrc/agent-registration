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

import play.api.mvc.AnyContentAsEmpty
import play.api.mvc.Request
import play.api.test.FakeRequest
import TdSupport.*

trait TdRequest:

  lazy val fakeBackendRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
    .withAuthToken()
    .withRequestId()
    .withTrueClientIp()
    .withTrueClientPort()
    .withDeviceId()

  lazy val backendRequest: Request[?] = fakeBackendRequest

  lazy val requestNotLoggedIn: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
    .withRequestId()
    .withTrueClientIp()
    .withTrueClientPort()
    .withDeviceId()

  lazy val authToken: String = "authorization-value-123"
  lazy val akamaiReputationValue: String = "akamai-reputation-value-123"
  lazy val requestId: String = "request-id-value-123"
  lazy val trueClientIp: String = "client-ip-123"
  lazy val trueClientPort: String = "client-port-123"
  lazy val deviceIdInRequest: String = "device-id-123"
