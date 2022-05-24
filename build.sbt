ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.8"

val AkkaVersion = "2.6.19"
val AkkaHttpVersion = "10.2.9"
val LogbackVersion = "1.2.9"
val SlickVersion = "3.3.3"
val MysqlDriverVersion = "8.0.13"
val PersistenceJdbcVersion = "5.0.4"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion,
  "com.typesafe.akka" %% "akka-stream" % AkkaVersion,
  "com.typesafe.akka" %% "akka-http" % AkkaHttpVersion,

  "com.typesafe.akka" %% "akka-http2-support" % AkkaHttpVersion,
  "com.typesafe.akka" %% "akka-discovery" % AkkaVersion,
  "com.typesafe.akka" %% "akka-cluster-typed" % AkkaVersion,
  "com.typesafe.akka" %% "akka-cluster-sharding-typed" % AkkaVersion,

  "ch.qos.logback" % "logback-classic" % "1.2.9",
  "org.scalatest" %% "scalatest" % "3.1.4" % Test,
  "com.typesafe.akka" %% "akka-actor-testkit-typed" % AkkaVersion % Test,

  "com.typesafe.akka" %% "akka-serialization-jackson" % AkkaVersion,

  "com.typesafe.akka" %% "akka-persistence-typed" % AkkaVersion,
  "com.typesafe.akka" %% "akka-persistence-testkit" % AkkaVersion % Test,

  "com.lightbend.akka" %% "akka-persistence-jdbc" % PersistenceJdbcVersion,
  "com.typesafe.akka" %% "akka-persistence-query" % AkkaVersion,

  "com.typesafe.slick" %% "slick" % SlickVersion,
  "com.typesafe.slick" %% "slick-hikaricp" % SlickVersion,
  "mysql" % "mysql-connector-java" % MysqlDriverVersion
)

assembly / assemblyJarName := "sea-battle.jar"

assembly / mainClass := Some("Main")

assemblyMergeStrategy in assembly := {
  case "reference.conf" => MergeStrategy.concat
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case x => MergeStrategy.first
}

enablePlugins(AkkaGrpcPlugin)