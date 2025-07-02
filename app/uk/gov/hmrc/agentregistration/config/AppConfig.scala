package uk.gov.hmrc.agentregistration.config

import javax.inject.{Inject, Singleton}
import play.api.Configuration

@Singleton
class AppConfig @Inject()(config: Configuration):

  val appName: String = config.get[String]("appName")
