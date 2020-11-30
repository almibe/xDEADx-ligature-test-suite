ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := "dev.ligature"
ThisBuild / organizationName := "Ligature"
ThisBuild / scalaVersion     := "3.0.0-M2"

lazy val root = project
  .in(file("."))
  .settings(
    name := "ligature-test-suite",
    libraryDependencies += "dev.ligature" %% "ligature" % "0.1.0-SNAPSHOT",
    libraryDependencies += "org.scalameta" %% "munit" % "0.7.19"
  )
