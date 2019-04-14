name := "minesweeper"

version := "0.1.0"

ThisBuild / scalaVersion := "2.12.8"

lazy val minesweeper = (project in file("."))
  .settings(
    name := "Hello",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-http"           % "10.1.8",
      "com.typesafe.akka" %% "akka-stream"         % "2.5.22",
      "org.json4s"        %% "json4s-jackson"      % "3.6.5",
      "org.scalatest"     %% "scalatest"           % "3.0.7"  % Test,
      "com.typesafe.akka" %% "akka-stream-testkit" % "2.5.19" % Test,
      "com.typesafe.akka" %% "akka-http-testkit"   % "10.1.8" % Test
    )
  )
