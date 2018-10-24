package it.gabfav.es_cqrs_es.read

import akka.NotUsed
import akka.actor.{ DiagnosticActorLogging, Status }
import akka.event.LoggingReceive
import akka.persistence.cassandra.query.scaladsl.CassandraReadJournal
import akka.persistence.query.{ NoOffset, Offset, PersistenceQuery }
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{ Sink, Source }

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

trait ReadJournalStreamManagerActor[T]
  extends DiagnosticActorLogging {

  import ReadJournalStreamManagerActor._

  implicit val mat: ActorMaterializer = ActorMaterializer()

  protected def createSource(
    readJournal: CassandraReadJournal,
    offset:      Offset
  ): Source[T, NotUsed]

  protected def startStream(offset: Offset = NoOffset): Unit = {
    log.info(s"Consumer start stream on Cassandra journal: ${self.path.name}")
    // obtain read journal by plugin id
    val readJournal: CassandraReadJournal = PersistenceQuery(context.system)
      .readJournalFor[CassandraReadJournal](CassandraReadJournal.Identifier)
    // issue query to journal
    val source = createSource(readJournal, offset)
    // create stream
    source.to(Sink.actorRefWithAck(self, InitMessage, AckMessage, CompleteMessage)).run()
    log.info(s"Journal reader started on Cassandra journal - ${self.path.name} - offset: $offset")
  }

  protected def manageJournalStream: Receive = LoggingReceive {
    case RestartStream ⇒
      startStream()
    case InitMessage ⇒
      sender ! AckMessage
      log.info(s"Consumer initialize stream on Cassandra journal - ${self.path.name}")
    case Status.Failure(cause) ⇒
      context.system.scheduler.scheduleOnce(30 seconds, self, RestartStream)
      log.error(s"Consumer on Cassandra journal received failure message: ${cause.getMessage}  - ${self.path.name}")
    case CompleteMessage ⇒
      log.warning(s"Consumer on Cassandra journal received complete message: - ${self.path.name}")
      context.system.scheduler.scheduleOnce(30 seconds, self, RestartStream)
    case AckMessage ⇒
      log.warning(s"Consumer on Cassandra journal received AckMessage message while in consuming: message ignored! - ${self.path.name}")
    case other ⇒
      log.error(s"Consumer on Cassandra journal received unknown message: $other  - ${self.path.name}")
  }

  /** Complete the stream */
  override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
    log.error(reason, s"Actor restarted due to unhandled exception when processing $message - ${self.path.name}")
    super.preRestart(reason, message)
  }

}

object ReadJournalStreamManagerActor {

  case object RestartStream

  sealed trait SinkMessage

  case object InitMessage extends SinkMessage

  case object AckMessage extends SinkMessage

  case object CompleteMessage extends SinkMessage

  case object Stop extends SinkMessage

}
