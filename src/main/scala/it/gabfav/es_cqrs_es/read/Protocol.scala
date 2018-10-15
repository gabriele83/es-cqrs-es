package it.gabfav.es_cqrs_es.read

import akka.persistence.query.EventEnvelope

object Protocol {
  case class GroupedEventEnvelope(seq: Seq[EventEnvelope])
}
