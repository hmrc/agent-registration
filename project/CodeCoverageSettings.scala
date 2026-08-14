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
    //    ScoverageKeys.coverageEnabled := true, // breaks when run through jenkins see https://github.com/scoverage/sbt-scoverage/issues/84#issuecomment-263026890
    ScoverageKeys.coverageExcludedPackages := excludedPackages.mkString(","),
    ScoverageKeys.coverageMinimumStmtTotal := 100,
    ScoverageKeys.coverageFailOnMinimum := false,
    ScoverageKeys.coverageHighlighting := true
  )
}
