package it.gabfav.es_cqrs_es.domain

/**
 *  Event Sourcing: the core idea of event sourcing is that whenever we make a change to the state of a system, we record
 *  that state change as an event, and we can confidently rebuild the system state by reprocessing the events at any time
 *  in the future. The event store becomes the principal source of truth, and the system state is purely derived from it.
 */
object Domain {

  /**
   * A domain entity is a component of the domain model.
   */
  trait DomainEntity

  /**
   *
   */
  trait DomainError {
    val message: String
  }

  /**
   * An event represents something that took place in the domain. They are always named with a past-participle verb.
   * Since an event represents something in the past, it can be considered a statement of fact and used to take decisions
   * in other parts of the system.
   *
   * @tparam T
   */
  trait DomainEvent[T <: DomainEntity] {
    def applyTo(domainEntity: T): T
  }

  /**
   * A command represents a request changes to the domain. They are named with a verb in the imperative.
   * Unlike an event, a command is not a statement of fact it's only a request, and thus may be refused.
   *
   * @tparam T
   * @tparam F
   * @tparam E
   */
  trait DomainCommand[T <: DomainEntity, +F <: DomainError, +E <: DomainEvent[T]] {
    def applyTo(domainEntity: T): Either[F, Option[E]]
  }
}
