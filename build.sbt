name := "minesweeper"

version := "0.1.0"

ThisBuild / scalaVersion := "2.12.8"

lazy val minesweeper = (project in file("."))
  .settings(
    name := "Hello",
    libraryDependencies += "com.typesafe.akka" %% "akka-http"   % "10.1.8",
    libraryDependencies += "com.typesafe.akka" %% "akka-stream" % "2.5.22",
    libraryDependencies += "org.json4s" %% "json4s-jackson" % "3.6.5",
    libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.7" % Test,
  )
