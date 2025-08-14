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

package uk.gov.hmrc.agentregistration.testsupport

import com.google.inject.AbstractModule
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.IntegrationPatience
import org.scalatest.freespec.AnyFreeSpecLike
import org.scalatest.time.Millis
import org.scalatest.time.Seconds
import org.scalatest.time.Span
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.Logging
import play.api.Mode
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.inject.guice.GuiceableModule
import play.api.test.DefaultTestServerFactory
import play.api.test.TestServerFactory
import play.core.server.ServerConfig
import uk.gov.hmrc.agentregistration.testsupport.RichMatchers
import uk.gov.hmrc.agentregistration.testsupport.testdata.TdAll
import uk.gov.hmrc.agentregistration.testsupport.wiremock.WireMockSupport
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport
import uk.gov.hmrc.mongo.test.MongoSupport

import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import scala.concurrent.ExecutionContext

trait ISpec
extends AnyFreeSpecLike,
  BeforeAndAfterEach,
  GuiceOneServerPerSuite,
  WireMockSupport,
  RichMatchers,
  MongoSupport:

  given ExecutionContext = app.injector.instanceOf[ExecutionContext]

  private val testServerPort = ISpec.testServerPort
  protected val baseUrl: String = s"http://localhost:${testServerPort.toString}"

  lazy val tdAll: TdAll = TdAll()
  lazy val frozenInstant: Instant = tdAll.instant
  lazy val clock: Clock = Clock.fixed(frozenInstant, ZoneId.of("UTC"))

  protected def configMap: Map[String, Any] =
    Map[String, Any](
      "auditing.consumer.baseUri.port" -> WireMockSupport.port,
      "auditing.enabled" -> false,
      "auditing.traceRequests" -> false,
      "microservice.services.auth.port" -> WireMockSupport.port,
      "mongodb.uri" -> mongoUri
    ) ++ configOverrides

  protected def configOverrides: Map[String, Any] = Map[String, Any]()

  lazy val overridesModule: AbstractModule =
    new AbstractModule:
      override def configure(): Unit = bind(classOf[Clock]).toInstance(clock)

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .overrides(GuiceableModule.fromGuiceModules(Seq(overridesModule)))
    .configure(configMap).build()

  override protected def testServerFactory: TestServerFactory = CustomTestServerFactory

  object CustomTestServerFactory
  extends DefaultTestServerFactory:
    override protected def serverConfig(app: Application): ServerConfig =
      val sc = ServerConfig(
        port = Some(testServerPort),
        sslPort = None,
        mode = Mode.Test,
        rootDir = app.path
      )
      sc.copy(configuration = sc.configuration.withFallback(overrideServerConfiguration(app)))

  override def beforeEach(): Unit = {
    super.beforeEach()
    dropDatabase()
  }

object ISpec:
  val testServerPort: Int = 19001
