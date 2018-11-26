name := "ScaMATA"

version := "0.2"

scalacOptions += "-deprecation"
scalacOptions += "-feature"
scalacOptions += "-Yrepl-sync"


javaOptions in Compile += "-Xms4G"
javaOptions in Compile += "-Xss2M"
javaOptions in Compile += "-Xmx4G"


mainClass in (Compile,run) := Some("org.scamata.util.MATASolver")
mainClass in assembly := Some("org.scamata.util.MATASolver")

//mainClass in (Compile,run) := Some("org.scamata.experiments.Test")
//mainClass in assembly := Some("org.scamata.experiments.Test")

fork := true

cancelable in Global := true

resolvers += "Artifactory-UCL" at "http://artifactory.info.ucl.ac.be/artifactory/libs-snapshot-local/"
resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"

scalaVersion := "2.12.4"


libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.5.11",
  "com.typesafe.akka" %% "akka-remote" % "2.5.11" ,
  "org.scalactic" %% "scalactic" % "3.0.1",
  "org.scalatest" %% "scalatest" % "3.0.1" % "test"
)

logBuffered in Test := false
