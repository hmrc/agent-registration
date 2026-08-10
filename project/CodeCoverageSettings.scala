import sbt.Setting
import scoverage.ScoverageKeys

object CodeCoverageSettings {

  private val excludedPackages: Seq[String] = Seq(
    "<empty>",
    "Reverse.*",
    "uk.gov.hmrc.BuildInfo",
    "app.*",
    "prod.*",
    ".*Routes.*",
    "uk.gov.hmrc.agentregistration.testOnly.*",
    "uk.gov.hmrc.agentregistration.shared.*"
  )

  val settings: Seq[Setting[_]] = Seq(
    ScoverageKeys.coverageEnabled := true,
    ScoverageKeys.coverageExcludedPackages := excludedPackages.mkString(","),
    ScoverageKeys.coverageMinimumStmtTotal := 100,
    ScoverageKeys.coverageFailOnMinimum := false,
    ScoverageKeys.coverageHighlighting := true
  )
}
