scalaVersion := "2.11.4"

libraryDependencies ++= Seq(
  "io.spray" %% "spray-can" % "1.3.2",
  "io.spray" %% "spray-routing" % "1.3.2",
  "io.spray" %% "spray-httpx" % "1.3.2",
  "io.spray" %% "spray-json" % "1.3.1",
  "com.typesafe.akka" %% "akka-actor" % "2.3.7",
  "com.typesafe.akka" %% "akka-testkit" % "2.3.7",
  "org.scalatest" % "scalatest_2.11" % "2.2.1" % "test"
)
