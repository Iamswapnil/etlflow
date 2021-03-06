lazy val scala212 = "2.12.10"
lazy val supportedScalaVersions = List(scala212)

import Dependencies._

lazy val coreSettings = Seq(
  name := "etlflow-core",
  organization := "com.github.tharwaninitin",
  crossScalaVersions := supportedScalaVersions,
  libraryDependencies ++= zioLibs ++ sparkLibs ++ googleCloudLibs
    ++ loggingLibs ++ dbLibs ++ miscLibs
    ++ awsLibs ++ testLibs,
  dependencyOverrides ++= {
    Seq(
      "com.fasterxml.jackson.module" % "jackson-module-scala_2.12" % "2.6.7.1",
      "com.fasterxml.jackson.core" % "jackson-databind" % "2.6.7.1",
    )
  }
)

lazy val schedulerSettings = Seq(
  name := "etlflow-scheduler"
  , libraryDependencies ++= caliban ++ jwt
)

lazy val root = (project in file("."))
  .settings(
    crossScalaVersions := Nil, // crossScalaVersions must be set to Nil on the aggregating project
    publish / skip := true)
  .aggregate(core, scheduler)

lazy val core = (project in file("modules/core"))
  .settings(coreSettings)
  .enablePlugins(BuildInfoPlugin)
  .settings(
    initialCommands := "import etlflow._",
    buildInfoKeys := Seq[BuildInfoKey](
      resolvers,
      libraryDependencies in Compile,
      name, version, scalaVersion, sbtVersion
    ),
    buildInfoOptions += BuildInfoOption.BuildTime,
    buildInfoPackage := "etlflow",
    Test / parallelExecution := false,
    testFrameworks += (new TestFramework("zio.test.sbt.ZTestFramework"))
  )

lazy val scheduler = (project in file("modules/scheduler"))
  .settings(schedulerSettings)
  .settings(
    scalacOptions ++= Seq("-Ypartial-unification"),
    Test / parallelExecution := false
  )
  .dependsOn(core)



