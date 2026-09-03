ThisBuild / majorVersion := 0
ThisBuild / scalaVersion := "3.7.0"
val playPort: Int = 22202
ThisBuild / scalafmtOnCompile := true
ThisBuild / semanticdbEnabled := true
ThisBuild / scalafixOnCompile := true

val strictBuilding: SettingKey[Boolean] = StrictBuilding.strictBuilding //defining here so it can be set before running sbt like `sbt 'set Global / strictBuilding := true' ...`
StrictBuilding.strictBuildingSetting

lazy val microservice = Project("agent-registration", file("."))
  .enablePlugins(play.sbt.PlayScala, SbtDistributablesPlugin)
  .disablePlugins(JUnitXmlReportPlugin) //Required to prevent https://github.com/scalatest/scalatest/issues/1427
  .settings(
    libraryDependencies ++= AppDependencies.compile ++ AppDependencies.test,
    Compile / doc / scalacOptions := Seq(), //this will allow to have warnings in `doc` task
    Test / doc / scalacOptions := Seq(), //this will allow to have warnings in `doc` task
    scalacOptions ++= ScalaCompilerFlags.scalaCompilerOptions,
    scalacOptions ++= {
      if (StrictBuilding.strictBuilding.value) ScalaCompilerFlags.strictScalaCompilerOptions else Nil
    },
    Test / parallelExecution := true,
    routesImport ++= Seq(
      "uk.gov.hmrc.agentregistration",
      "uk.gov.hmrc.agentregistration.RoutesExports.*"
    )
  )
  .settings(CodeCoverageSettings.settings *)
  .settings(commands ++= SbtCommands.commands)
  .settings(SbtUpdatesSettings.sbtUpdatesSettings *)
  .settings(WartRemoverSettings.wartRemoverSettings)
  .settings(PlayKeys.playDefaultPort := playPort)
