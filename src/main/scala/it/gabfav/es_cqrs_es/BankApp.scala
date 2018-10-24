package it.gabfav.es_cqrs_es

import akka.actor.{ ActorSystem, PoisonPill }
import akka.cluster.sharding.{ ClusterSharding, ClusterShardingSettings }
import akka.cluster.singleton.{ ClusterSingletonManager, ClusterSingletonManagerSettings }
import akka.event.{ Logging, LoggingAdapter }
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.sksamuel.elastic4s.http.ElasticClient
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.typesafe.config.ConfigFactory
import it.gabfav.es_cqrs_es.domain.BankAccount._
import it.gabfav.es_cqrs_es.read.{ BankAccountEventReader, ESHelper }
import it.gabfav.es_cqrs_es.write.BankAccountWriteActor

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

object BankApp extends App with ActorSharding {

  implicit val system: ActorSystem = {

    val config = args.headOption match {
      case Some(port) ⇒
        ConfigFactory.parseString(
          s"""
        akka.remote.netty.tcp.port=$port
        akka.remote.artery.canonical.port=$port
        """
        ).withFallback(ConfigFactory.load())
      case None ⇒
        ConfigFactory.load()
    }
    // Create an Akka system
    ActorSystem(Config.serviceName, config)
  }

  implicit val dispatcher: ExecutionContext = system.dispatcher

  implicit val mat: ActorMaterializer = ActorMaterializer()
  implicit val timeout: Timeout = Timeout(20 seconds)
  implicit val logger: LoggingAdapter = Logging(system, getClass)

  private implicit val esClient: ElasticClient = ESHelper.elasticClient

  esClient.execute(ESHelper.createOffsetIndexRequest) onComplete { _ ⇒
    createClusterShardingActors()
    createClusterSingletonActors()
    doTest()
  }

  sys.addShutdownHook(esClient.close())

  private def createClusterShardingActors(): Unit = {
    ClusterSharding(system).start(
      typeName        = BankAccountWriteActor.Name,
      entityProps     = BankAccountWriteActor.props,
      settings        = ClusterShardingSettings(system),
      extractEntityId = BankAccountWriteActor.idExtractor,
      extractShardId  = BankAccountWriteActor.shardResolver
    )
  }

  private def createClusterSingletonActors(): Unit = {
    system.actorOf(ClusterSingletonManager.props(
      singletonProps     = BankAccountEventReader.props(esClient),
      terminationMessage = PoisonPill,
      settings           = ClusterSingletonManagerSettings(system)
    ), BankAccountEventReader.Name)
  }

  private def doTest(): Unit = {

    system.scheduler.scheduleOnce(10 seconds) {
      val id1 = "id1"
      val id2 = "id2"
      val id3 = "id3"

      bankAccountRegion ! CreateBankAccount(id1, "Sandro Rossi")
      bankAccountRegion ! AddAmount(id1, 10, 1)
      bankAccountRegion ! SubtractAmount(id1, 5, 2)
      bankAccountRegion ! SubtractAmount(id1, 3, 3)
      bankAccountRegion ! SubtractAmount(id1, 2, 4)

      bankAccountRegion ! CreateBankAccount(id2, "Franco Bianchi")
      bankAccountRegion ! AddAmount(id2, 10, 1)
      bankAccountRegion ! SubtractAmount(id2, 5, 2)
      bankAccountRegion ! SubtractAmount(id2, 3, 3)
      bankAccountRegion ! SubtractAmount(id2, 2, 4)

      bankAccountRegion ! CreateBankAccount(id3, "Paolo Verdi")
      bankAccountRegion ! AddAmount(id3, 10, 1)
      bankAccountRegion ! AddAmount(id3, 10, 2)
      bankAccountRegion ! AddAmount(id3, 10, 3)
      bankAccountRegion ! SubtractAmount(id3, 100, 4)

    }
  }

}
