package it.gabfav.es_cqrs_es.read

import akka.NotUsed
import akka.actor.Props
import akka.event.LoggingReceive
import akka.persistence.cassandra.query.scaladsl.CassandraReadJournal
import akka.persistence.query.{ NoOffset, Offset, TimeBasedUUID }
import akka.stream.scaladsl.Source
import com.sksamuel.elastic4s.circe._
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.http.bulk.BulkResponse
import com.sksamuel.elastic4s.http.get.GetResponse
import com.sksamuel.elastic4s.http.{ ElasticClient, Response }
import io.circe.generic.auto._
import it.gabfav.es_cqrs_es.adapter.BankAccountAdapter
import it.gabfav.es_cqrs_es.domain.BankAccount.BankAccountEvent
import it.gabfav.es_cqrs_es.read.BankAccountEventReader._
import it.gabfav.es_cqrs_es.read.Protocol.GroupedEventEnvelope
import it.gabfav.es_cqrs_es.read.ReadJournalStreamManagerActor.AckMessage

import scala.concurrent.duration._
import scala.concurrent.{ ExecutionContext, Future }
import scala.util.{ Failure, Success }

class BankAccountEventReader(implicit client: ElasticClient) extends ReadJournalStreamManagerActor[GroupedEventEnvelope] {

  private implicit val executionContext: ExecutionContext = context.dispatcher

  override def preStart(): Unit = context.system.scheduler.scheduleOnce(messagesDelay, self, ReadOffset)(context.dispatcher)

  override protected def createSource(readJournal: CassandraReadJournal, offset: Offset): Source[GroupedEventEnvelope, NotUsed] = readJournal
    .eventsByTag(BankAccountAdapter.BankAccountTag, offset)
    .groupedWithin(groupSize, streamWindow)
    .map(GroupedEventEnvelope(_))

  private def manageJournalMessage: Receive = LoggingReceive {
    case groupedEvents: GroupedEventEnvelope ⇒
      val offset2event = groupedEvents.seq map (ee ⇒ ee.offset.asInstanceOf[TimeBasedUUID] -> ee.event.asInstanceOf[BankAccountEvent])
      val maxOffset: TimeBasedUUID = offset2event.maxBy(_._1)._1
      val events: Seq[BankAccountEvent] = offset2event map (_._2)
      val res = indexEventsAndOffset(events, maxOffset)
      // TODO ...
      sender ! AckMessage // do nothing
  }

  override def receive: Receive = {
    case ReadOffset ⇒
      readOffset onComplete {
        case Success(offset) ⇒
          self ! OffsetReaded(offset)
        case Failure(e) ⇒
          self ! OffsetReaded(NoOffset)
          log.error(e, s"Offset: ${ESHelper.bankAccountOffsetId} not found")
      }
    case OffsetReaded(offset) ⇒
      context.become(withOffset(offset))
    case unknown ⇒
      log.error(s"Received unknown message in receiveCommand (sender: $sender - message: $unknown)")
  }

  def withOffset(offset: Offset): Receive = {
    startStream(offset)
    manageJournalMessage orElse manageJournalStream
  }

}

object BankAccountEventReader {

  val Name = "bank-account-event-reader"

  def props(implicit client: ElasticClient): Props = Props(new BankAccountEventReader)

  private val groupSize = 100
  private val streamWindow = 1 seconds
  private val messagesDelay = 5 seconds

  /** internal protocol */
  case object ReadOffset

  case class OffsetReaded(offset: Offset)

  private def readOffset(implicit client: ElasticClient, executionContext: ExecutionContext): Future[Offset] = {
    client.execute(ESHelper.getOffsetRequest(ESHelper.bankAccountOffsetId))
      .map { read: Response[GetResponse] ⇒
        if (read.isSuccess && read.result.exists) {
          read.result.to[TimeBasedUUID]
        } else {
          NoOffset
        }
      }
  }

  private def indexEventsAndOffset(
    events: Seq[BankAccountEvent],
    offset: TimeBasedUUID
  )(implicit client: ElasticClient, executionContext: ExecutionContext): Future[Response[BulkResponse]] = {
    client.execute(
      bulk(ESHelper.indexEventsRequest(events) :+ ESHelper.indexOffsetRequest(ESHelper.bankAccountOffsetId)(offset))
    )
  }

}
