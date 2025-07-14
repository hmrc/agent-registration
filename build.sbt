import uk.gov.hmrc.DefaultBuildSettings

ThisBuild / majorVersion := 0
ThisBuild / scalaVersion := "3.6.1"
ThisBuild / scalacOptions += "-Wconf:msg=Flag.*repeatedly:s"
val playPort: Int = 22202

val strictBuilding: SettingKey[Boolean] = StrictBuilding.strictBuilding //defining here so it can be set before running sbt like `sbt 'set Global / strictBuilding := true' ...`
StrictBuilding.strictBuildingSetting

lazy val microservice = Project("agent-registration", file("."))
  .enablePlugins(play.sbt.PlayScala, SbtDistributablesPlugin)
  .disablePlugins(JUnitXmlReportPlugin) //Required to prevent https://github.com/scalatest/scalatest/issues/1427
  .settings(
    libraryDependencies ++= AppDependencies.compile ++ AppDependencies.test,
    scalacOptions += "-Wconf:src=routes/.*:s",
    Compile / doc / scalacOptions := Seq(), //this will allow to have warnings in `doc` task
    Test / doc / scalacOptions := Seq(), //this will allow to have warnings in `doc` task
    scalacOptions -= "-Wunused:all",
    scalacOptions ++= ScalaCompilerFlags.scalaCompilerOptions,
    scalacOptions ++= {
      if (StrictBuilding.strictBuilding.value) ScalaCompilerFlags.strictScalaCompilerOptions else Nil
    },
    Test / parallelExecution := true,
    routesImport ++= Seq(
      "uk.gov.hmrc.agentregistration"
    )
  )
  .settings(resolvers += Resolver.jcenterRepo)
  .settings(CodeCoverageSettings.settings *)
  .settings(commands ++= SbtCommands.commands)
  .settings(SbtUpdatesSettings.sbtUpdatesSettings *)
  .settings(WartRemoverSettings.wartRemoverSettings)
  .settings(PlayKeys.playDefaultPort := playPort)
