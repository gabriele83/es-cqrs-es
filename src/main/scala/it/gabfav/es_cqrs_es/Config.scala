package it.gabfav.es_cqrs_es

import com.typesafe.config.{ Config, ConfigFactory }

import scala.collection.JavaConverters._
import scala.concurrent.duration.FiniteDuration
import scala.language.implicitConversions

object Config {

  implicit def asFiniteDuration(d: java.time.Duration): FiniteDuration =
    scala.concurrent.duration.Duration.fromNanos(d.toNanos)

  val config: Config = ConfigFactory.load

  /* environment */
  val environment: String = config.getString("env")
  /** Service Name */
  val serviceName: String = config.getString("service")

  /** elastic search configs */
  val elasticCfg: Config = config getConfig "elastic"

  val esHost: String = elasticCfg getString "host"
  val esIndexPrefix: String = elasticCfg getString "indexPrefix"
  lazy val eShards: Int = elasticCfg getInt "shards"
  lazy val eReplicas: Int = elasticCfg getInt "replicas"
}
