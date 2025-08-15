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

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import org.scalatest.BeforeAndAfterAll
import org.scalatest.BeforeAndAfterEach
import org.scalatest.Suite
import play.api.Logger

trait WireMockSupport
extends BeforeAndAfterAll,
  BeforeAndAfterEach:
  self: Suite =>

  private val logger = Logger(getClass)
  implicit val wireMockServer: WireMockServer = WireMockServer(WireMockConfiguration.wireMockConfig().port(WireMockSupport.port))

  WireMock.configureFor(WireMockSupport.port)

  override protected def beforeAll(): Unit =
    logger.info("Starting wiremock server...")
    wireMockServer.start()
    logger.info(s"Starting wiremock server - done, running=${wireMockServer.isRunning.toString} on ${wireMockServer.port().toString} port")

  override def beforeEach(): Unit =
    logger.info("Resetting wire mock server ...")
    WireMock.reset()
    logger.info("Resetting wire mock server - done")

  override protected def afterAll(): Unit =
    logger.info("Stopping wire mock server ...")
    wireMockServer.stop()
    logger.info("Stopping wire mock server - done")

object WireMockSupport:
  lazy val port: Int = 11111
