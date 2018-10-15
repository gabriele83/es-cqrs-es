package it.gabfav.es_cqrs_es.write

import akka.actor.DiagnosticActorLogging
import akka.event.LoggingReceive
import akka.persistence._
import akka.persistence.journal.Tagged

/**
 * General functionality of a repository that has a persistent state.
 *
 * Subclasses must implement their own receiveCommand because pattern matching can't be generalized over type-parametrized case classes.
 *
 * @tparam S Type of the state of the repository
 * @tparam C Type of the command that the actor receives
 * @tparam E Type of the event that the actor persists
 * @tparam F Type of the failure that the applied command throws
 */
trait RepositoryActor[S, C, E, F] extends PersistentActor with DiagnosticActorLogging {

  /**
   * Internal state of the actor
   */
  var state: S
  /**
   * Interval between two snapshots
   */
  protected val snapShotInterval = 1000
  /**
   * How many snapshots to maintain
   */
  protected val selectionCriteriaInterval: Int = 3 * snapShotInterval

  /**
   * Accept a command, and return the event that represent the changes that the command produces
   *
   * @param state   Current state
   * @param command Command to apply
   * @return Event that results from the applied command, if results in one, or an error.
   */
  protected def acceptCommand(state: S, command: C): Either[F, Option[E]]

  /**
   * Calculate the next state after the event has been applied
   *
   * @param current Current state
   * @param event   Event that has been recorded
   * @return The state after the event has been applied.
   */
  protected def update(current: S, event: E): S

  /**
   * Handle a state modification command with this algorithm:
   *
   * - _accept_ the command in an event
   * - if all ok, persist the event, apply it (and eventually snapshot) and respond ACK to the sender
   * - if no event is generated, respond NOP to the sender
   * - if ko, repond to the sender with the error.
   *
   * @param command      Command to apply
   * @param currentState Current state to modify
   */
  protected def handleCommand(command: C, currentState: S, tags: Set[String]): Unit = {
    acceptCommand(currentState, command) match {
      case Right(Some(event)) ⇒ persist(Tagged(event, tags)) { tagged ⇒
        state = update(currentState, tagged.payload.asInstanceOf[E])
        saveSnapshotF(state)
        //sender() ! Protocol.ACK
        println("handleCommand OK --> " + state)
      }
      case Right(None) ⇒
        // sender() ! Protocol.NOP
        println("handleCommand NOP --> " + state)
      case Left(error) ⇒
        //  sender() ! convertCommandFailure(error)
        println("handleCommand ERROR --> " + error)
    }
  }

  /**
   * Hook to convert the command's error into protocol's error message
   *
   * @param failure Error that results from an applied command error
   * @return protocol error message
   */
  protected def convertCommandFailure(failure: F): Protocol.Error

  protected def saveSnapshotF(state: S): Unit

  /**
   * Handle the whole protocol of possible messages resulting from managing the snapshots.
   */
  protected def receiveSnapshotCommand: Receive = LoggingReceive {
    case SaveSnapshotSuccess(metadata) ⇒
      log.info(s"Successful saving of a snapshot - metadata: $metadata")
      if (metadata.sequenceNr > selectionCriteriaInterval) {
        deleteSnapshots(SnapshotSelectionCriteria(metadata.sequenceNr - selectionCriteriaInterval, metadata.timestamp - 1))
      }
    case SaveSnapshotFailure(metadata, reason) ⇒
      log.warning(s"Failed saving of a snapshot - metadata: $metadata - error: $reason")
    case DeleteSnapshotSuccess(metadata) ⇒
      log.info(s"Successful deleting of a snapshot - metadata: $metadata")
    case DeleteSnapshotsSuccess(criteria) ⇒
      log.info(s"Successful deleting of a snapshot - criteria: $criteria")
    case DeleteSnapshotFailure(metadata, cause) ⇒
      log.warning(s"Failed saving of a snapshot - metadata: $metadata - error: $cause")
    case DeleteSnapshotsFailure(criteria, cause) ⇒
      log.warning(s"Failed saving of a snapshot - criteria: $criteria - error: $cause")
  }

  /**
   * The deriving actor must implement this method to handle its class of events for snapshot recovery.
   */
  protected def receiveSnapshotRecover: Receive

  /**
   * Final cases (completed and unknown) managing messages that can be received while restarting.
   */
  override def receiveRecover: Receive = receiveSnapshotRecover andThen LoggingReceive {
    case RecoveryCompleted ⇒ log.info("Recovery completed: " + state)
    case unknown           ⇒ log.warning(s"Received unknown message in receiveRecover: $unknown")
  }
}
