import sbt._

object Dependencies {

  // Resolvers
  val spResolvers: Seq[Resolver] = Seq(
    "elasticsearch-releases" at "https://artifacts.elastic.co/maven"
  )

  private lazy val akkaVersion: String = "2.5.13"
  private lazy val akkaPersistenceInmemoryVersion: String = "2.5.1.1"
  private lazy val akkaPersistenceCassandraVersion: String = "0.90"
  private lazy val circeVersion: String = "0.10.0"
  private lazy val elastic4sVersion = "6.3.7"
  private lazy val log4jSlf4jVersion: String = "1.7.25"
  private lazy val logBackVersion: String = "1.2.3"
  private lazy val scalaLoggingVersion: String = "3.9.0"
  private lazy val spec2Version: String = "4.3.4"
  private lazy val scalaTestVersion: String = "3.0.5"
  private lazy val logbackContrib: String = "0.1.5"


  private val akkaDependencies: Seq[ModuleID] = Seq(
    "com.typesafe.akka" %% "akka-cluster" % akkaVersion,
    "com.typesafe.akka" %% "akka-cluster-sharding" % akkaVersion,
    "com.typesafe.akka" %% "akka-remote" % akkaVersion,
    "com.typesafe.akka" %% "akka-persistence" % akkaVersion,
    "com.typesafe.akka" %% "akka-persistence-query" % akkaVersion,
    "com.typesafe.akka" %% "akka-stream" % akkaVersion,
    "com.typesafe.akka" %% "akka-persistence-cassandra" % akkaPersistenceCassandraVersion,
    "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
    // testing
    "com.typesafe.akka" %% "akka-testkit" % akkaVersion % "test",
    "com.github.dnvriend" %% "akka-persistence-inmemory" % akkaPersistenceInmemoryVersion % "test",
    "com.typesafe.akka" %% "akka-persistence-cassandra-launcher" % akkaPersistenceCassandraVersion % Test
  )

  private val jsonDependencies: Seq[ModuleID] = Seq(
    "io.circe" %% "circe-core" % circeVersion,
    "io.circe" %% "circe-generic" % circeVersion,
    "io.circe" %% "circe-parser" % circeVersion,
    "io.circe" %% "circe-generic-extras" % circeVersion,
    "io.circe" %% "circe-java8" % circeVersion
  )

  private val elastic4sDependencies: Seq[ModuleID] = Seq(
    "com.sksamuel.elastic4s" %% "elastic4s-core" % elastic4sVersion,
    "com.sksamuel.elastic4s" %% "elastic4s-http" % elastic4sVersion,
    "com.sksamuel.elastic4s" %% "elastic4s-circe" % elastic4sVersion
  )

  private val generalDependencies: Seq[ModuleID] = Seq(
    //log
    "ch.qos.logback" % "logback-classic" % logBackVersion,
    "org.slf4j" % "log4j-over-slf4j" % log4jSlf4jVersion,
    "com.typesafe.scala-logging" %% "scala-logging" % scalaLoggingVersion,
    "ch.qos.logback.contrib" % "logback-jackson" % logbackContrib,
    "ch.qos.logback.contrib" % "logback-json-classic" % logbackContrib,
    //test
    "org.scalatest" %% "scalatest" % scalaTestVersion % "test",
    "org.specs2" %% "specs2-core" % spec2Version % "test",
    "org.specs2" % "specs2-junit_2.12" % spec2Version % "test"
  )

  val spDependencies: Seq[ModuleID] = generalDependencies ++ jsonDependencies ++ akkaDependencies ++ elastic4sDependencies
}
