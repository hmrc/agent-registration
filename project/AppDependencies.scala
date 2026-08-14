import sbt.Keys.libraryDependencies
import sbt.*

object AppDependencies {

  private val bootstrapVersion = "10.8.0"
  private val hmrcMongoVersion = "2.13.0"

  val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"             %% "bootstrap-backend-play-30"  % bootstrapVersion,
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-play-30"         % hmrcMongoVersion,
    "uk.gov.hmrc"             %% "crypto-json-play-30"        % "8.4.0",
    "uk.gov.hmrc.objectstore" %% "object-store-client-common" % "2.6.0",
    "uk.gov.hmrc"             %% "internal-auth-client-play-30" % "4.4.0",
    "com.softwaremill.quicklens" %% "quicklens" % "1.9.15", // Updated for Scala 3 compatibility

  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"             %% "bootstrap-test-play-30"     % bootstrapVersion            % Test,
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-test-play-30"    % hmrcMongoVersion            % Test,
  )
}
