package it.gabfav.es_cqrs_es.adapter

import java.time.Instant

import akka.persistence.journal.{ EventAdapter, EventSeq, Tagged }
import it.gabfav.es_cqrs_es.domain.BankAccount
import it.gabfav.es_cqrs_es.domain.BankAccount._
import it.gabfav.es_cqrs_es.domain.proto.bankAccount._
import BankAccountAdapter._

class BankAccountAdapter extends EventAdapter {

  override def manifest(event: Any): String = event.getClass.getSimpleName

  override def fromJournal(event: Any, manifest: String): EventSeq = event match {
    case msg: BankAccountCreatedMessage ⇒
      EventSeq.single(BankAccountCreated(msg.id, msg.accountHolder, Instant.ofEpochMilli(msg.instant)))
    case msg: AmountAddedMessage ⇒
      EventSeq.single(AmountAdded(msg.id, msg.amount, Instant.ofEpochMilli(msg.instant)))
    case msg: AmountSubtractedMessage ⇒
      EventSeq.single(AmountSubtracted(msg.id, msg.amount, Instant.ofEpochMilli(msg.instant)))
    case _ ⇒
      EventSeq.empty
  }

  override def toJournal(event: Any): Any = event match {
    case evt: BankAccountCreated ⇒
      Tagged(BankAccountCreatedMessage(evt.id, evt.accountHolder, evt.instant.toEpochMilli), Set(BankAccountTag))
    case evt: AmountAdded ⇒
      Tagged(AmountAddedMessage(evt.id, evt.amount, evt.instant.toEpochMilli), Set(BankAccountTag))
    case evt: AmountSubtracted ⇒
      Tagged(AmountSubtractedMessage(evt.id, evt.amount, evt.instant.toEpochMilli), Set(BankAccountTag))
    case _ ⇒
      throw new RuntimeException(s"Cannot serialize '${event.getClass.getName}' to protobuf")
  }
}

object BankAccountAdapter {

  val BankAccountTag = "BankAccount"

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
