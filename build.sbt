name := "Scala_UE02"

version := "0.1"

scalaVersion := "2.13.1"
scalacOptions += "-deprecation"

val akkaVersion = "2.6.0"
val akkaHttpVersion = "10.1.11"
val logbackVersion = "1.2.3"

libraryDependencies += "com.typesafe.akka" %% "akka-actor" % akkaVersion
libraryDependencies += "com.typesafe.akka" %% "akka-stream" % akkaVersion
libraryDependencies += "com.typesafe.akka" %% "akka-http-core" % akkaHttpVersion

libraryDependencies += "com.typesafe.akka" %% "akka-slf4j" % akkaVersion
libraryDependencies += "ch.qos.logback" % "logback-classic" % logbackVersion
