package it.gabfav.es_cqrs_es.write

/**
 * Response protocol for repository actors.
 */
object Protocol {

  /**
   * Ok response. The command has been executed an the resulting event persisted.
   */
  case object ACK

  /**
   * No operation response. The command has been correctly accepted, but has not produced any event (it has not modified the state) so nothing has changed.
   */
  case object NOP

  /**
   * Error response. The command has not been accepted.
   */
  case class Error(message: String)

}
