package it.gabfav.es_cqrs_es

import com.typesafe.config.{ Config, ConfigFactory }

import scala.collection.JavaConverters._
import scala.concurrent.duration.FiniteDuration
import scala.language.implicitConversions

object GlobalConfig {

  implicit def asFiniteDuration(d: java.time.Duration): FiniteDuration =
    scala.concurrent.duration.Duration.fromNanos(d.toNanos)

  val config: Config = ConfigFactory.load

  /* environment */
  val environment: String = config.getString("env")
  /** Service Name */
  val serviceName: String = config.getString("service")

  /** elastic search */
  // val esUser: String = sys.env("ES_USER")
  // val esPassword: String = sys.env("ES_PASSWORD")
  /** elastic search configs */
  val elasticCfg: Config = config getConfig "elastic"

  val esHost: String = elasticCfg getString "host"
  val esPort: Int = elasticCfg getInt "port"
  val esShards: Int = elasticCfg getInt "shards"
  val esReplicas: Int = elasticCfg getInt "replicas"
  val esIndexPrefix: String = elasticCfg getString "host"
}
