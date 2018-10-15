package it.gabfav.es_cqrs_es.adapter

import java.time.Instant

import akka.persistence.journal.{ EventAdapter, EventSeq }
import it.gabfav.es_cqrs_es.domain.BankAccount
import it.gabfav.es_cqrs_es.domain.BankAccount._
import it.gabfav.es_cqrs_es.domain.proto.bankAccount._

class BankAccountAdapter extends EventAdapter {

  override def manifest(event: Any): String = event.getClass.getSimpleName

  override def fromJournal(event: Any, manifest: String): EventSeq = event match {
    case msg: BankAccountCreatedMessage ⇒
      println("OK 1")
      EventSeq.single(BankAccountCreated(msg.id, msg.accountHolder, Instant.ofEpochMilli(msg.instant)))
    case msg: AmountAddedMessage ⇒
      println("OK 2")
      EventSeq.single(AmountAdded(msg.id, msg.amount, Instant.ofEpochMilli(msg.instant)))
    case msg: AmountSubtractedMessage ⇒
      println("OK 3")
      EventSeq.single(AmountSubtracted(msg.id, msg.amount, Instant.ofEpochMilli(msg.instant)))
    case x ⇒
      println("KO 4: " + x)
      EventSeq.empty
  }

  override def toJournal(event: Any): Any = event match {
    case evt: BankAccountCreated ⇒
      println("OK A")
      BankAccountCreatedMessage(evt.id, evt.accountHolder, evt.instant.toEpochMilli)
    case evt: AmountAdded ⇒
      println("OK B")
      AmountAddedMessage(evt.id, evt.amount, evt.instant.toEpochMilli)
    case evt: AmountSubtracted ⇒
      println("OK C")
      AmountSubtractedMessage(evt.id, evt.amount, evt.instant.toEpochMilli)
    case _ ⇒
      println("KO D")
      throw new RuntimeException(s"Cannot serialize '${event.getClass.getName}' to protobuf")
  }
}

object BankAccountAdapter {

  def fromEntity(entity: BankAccount): BankAccountMessage = BankAccountMessage()
    .withId(entity.id)
    .withAccountHolder(entity.accountHolder)
    .withAmount(entity.amount)
    .withCreation(entity.creation.toEpochMilli)
    .withLastEdit(entity.lastEdit.toEpochMilli)
    .withVersion(entity.version)

  def toEntity(proto: BankAccountMessage): BankAccount = BankAccount(
    id            = proto.id,
    accountHolder = proto.accountHolder,
    amount        = proto.amount,
    creation      = Instant.ofEpochMilli(proto.creation),
    lastEdit      = Instant.ofEpochMilli(proto.lastEdit),
    version       = proto.version
  )
}
