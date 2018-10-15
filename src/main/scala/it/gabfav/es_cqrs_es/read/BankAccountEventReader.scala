package it.gabfav.es_cqrs_es.read

import akka.NotUsed
import akka.actor.Props
import akka.event.LoggingReceive
import akka.persistence.cassandra.query.scaladsl.CassandraReadJournal
import akka.persistence.query.{ EventEnvelope, NoOffset, Offset }
import akka.stream.scaladsl.Source
import it.gabfav.es_cqrs_es.read.BankAccountEventReader._
import it.gabfav.es_cqrs_es.read.Protocol.GroupedEventEnvelope
import it.gabfav.es_cqrs_es.read.ReadJournalStreamManagerActor.{ AckMessage, RestartStream }
import it.gabfav.es_cqrs_es.write.BankAccountWriteActor

import scala.concurrent.Future
import scala.concurrent.duration._

// TODO ElasticSearch index definition
// TODO singleton
class BankAccountEventReader extends ReadJournalStreamManagerActor[GroupedEventEnvelope] {

  //override def preStart(): Unit = context.system.scheduler.scheduleOnce(messagesDelay, self, ReadOffset)(context.dispatcher)
  override def preStart(): Unit = context.system.scheduler.scheduleOnce(messagesDelay, self, RestartStream)(context.dispatcher)

  override protected def createSource(readJournal: CassandraReadJournal, offset: Offset): Source[GroupedEventEnvelope, NotUsed] = readJournal
    .eventsByTag(BankAccountWriteActor.BankAccountTag, offset)
    .groupedWithin(groupSize, streamWindow)
    .map(GroupedEventEnvelope(_))

  /*
    private def manageJournalMessage: Receive = LoggingReceive {
     case _: EventEnvelope ⇒ sender ! AckMessage // do nothing
   }

   override def receive: Receive = manageJournalMessage orElse manageJournalStream
    */

  /*override def receive: Receive = LoggingReceive {
    case ReadOffset ⇒ getElasticSearchOffset
    case _ => // TODO ignore ????
  }

  def connected(esOffset: Offset): Receive = LoggingReceive {
    ???
  }*/

  private def manageJournalMessage: Receive = LoggingReceive {
    case eventEnvelope: EventEnvelope ⇒
      println("READ ---------> " + eventEnvelope.event)
      sender ! AckMessage // do nothing
  }

  override def receive: Receive = manageJournalMessage orElse manageJournalStream

}

object BankAccountEventReader {

  val Name = "bank-account-event-reader"

  def props: Props = Props(new BankAccountEventReader)

  private val groupSize = 100
  private val streamWindow = 1 seconds
  private val messagesDelay = 5 seconds

  /** internal protocol */
  case object ReadOffset

  def getElasticSearchOffset: Future[Offset] = {
    //TODO
    Future.successful(NoOffset)
  }
}

