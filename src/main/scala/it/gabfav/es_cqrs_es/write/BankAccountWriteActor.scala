package it.gabfav.es_cqrs_es.write

import akka.actor.Props
import akka.cluster.sharding.ShardRegion
import akka.event.LoggingReceive
import akka.persistence.{ RecoveryCompleted, SnapshotOffer }
import it.gabfav.es_cqrs_es.adapter
import it.gabfav.es_cqrs_es.domain.BankAccount
import it.gabfav.es_cqrs_es.domain.BankAccount._
import it.gabfav.es_cqrs_es.domain.proto.bankAccount.BankAccountMessage
import BankAccountWriteActor._

class BankAccountWriteActor extends RepositoryActor[BankAccount, BankAccountCommand, BankAccountEvent, BankAccountError] {

  override def persistenceId: String = s"${Name}_${self.path.name}"

  var state: BankAccount = BankAccount()

  override def receiveCommand: Receive = receiveSnapshotCommand orElse LoggingReceive {
    case cmd: BankAccountCommand ⇒ handleCommand(cmd, state)
    case unknown                 ⇒ log.error(s"Received unknown message in receiveCommand (sender: ${sender()} - message: $unknown)")
  }

  override protected def update(state: BankAccount, event: BankAccountEvent): BankAccount = event.applyTo(state)

  override def receiveRecover: Receive = LoggingReceive {
    case event: BankAccountEvent ⇒
      state = update(state, event)
    case SnapshotOffer(_, snapshot: BankAccountMessage) ⇒
      state = adapter.BankAccountAdapter.toEntity(snapshot)
    case RecoveryCompleted ⇒ log.info(s"Recovery completed: $state")
  }

  override protected def saveSnapshotF(state: BankAccount): Unit = if (lastSequenceNr % snapShotInterval == 0 && lastSequenceNr != 0) {
    saveSnapshot(adapter.BankAccountAdapter.fromEntity(state))
  }

  override protected def acceptCommand(state: BankAccount, command: BankAccountCommand): Either[BankAccountError, Option[BankAccountEvent]] =
    command.applyTo(state)

  override protected def convertCommandFailure(failure: BankAccountError): Protocol.Error = Protocol.Error(failure.message)

}

object BankAccountWriteActor {

  val Name: String = "bank-account-repository"

  def props: Props = Props(new BankAccountWriteActor)

  /* AKKA sharding */
  val idExtractor: ShardRegion.ExtractEntityId = {
    case cmd: BankAccountCommand ⇒ (cmd.id, cmd)
  }

  val shardResolver: ShardRegion.ExtractShardId = {
    case cmd: BankAccountCommand     ⇒ cmd.id
    case ShardRegion.StartEntity(id) ⇒ id
  }
}
