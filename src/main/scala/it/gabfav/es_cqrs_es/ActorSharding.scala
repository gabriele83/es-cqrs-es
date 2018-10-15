package it.gabfav.es_cqrs_es

import akka.actor.{ ActorRef, ActorSystem }
import akka.cluster.sharding.ClusterSharding
import it.gabfav.es_cqrs_es.write.BankAccountWriteActor

trait ActorSharding {

  implicit val system: ActorSystem

  def bankAccountRegion: ActorRef = ClusterSharding(system).shardRegion(BankAccountWriteActor.Name)

}
