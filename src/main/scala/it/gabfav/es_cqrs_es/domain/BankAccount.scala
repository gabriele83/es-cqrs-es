package it.gabfav.es_cqrs_es.domain

import java.time.Instant

import it.gabfav.es_cqrs_es.domain.Domain._

case class BankAccount(
  id:            String,
  accountHolder: String,
  amount:        Long    = 0,
  creation:      Instant = Instant.now(),
  lastEdit:      Instant = Instant.EPOCH,
  version:       Int     = 0
) extends DomainEntity

object BankAccount {

  type BankAccountCommandResult = Either[BankAccountError, Option[BankAccountEvent]]

  /**
   * Zero element construction
   */
  def apply(): BankAccount = new BankAccount("", "")

  def apply(id: String, accountHolder: String): BankAccount = new BankAccount(id, accountHolder)

  /**
   * Error class for Bank Account Commands
   */
  case class BankAccountError(message: String) extends DomainError

  /**
   * Base Bank Account Event trait
   */
  sealed trait BankAccountEvent extends DomainEvent[BankAccount] {
    val id: String
  }

  /**
   * Base Bank Account Command
   *
   * Each command should always specify: id of the detail that should receive the command, tenant of reference, author and timestamp of the command.
   */
  sealed trait BankAccountCommand extends DomainCommand[BankAccount, BankAccountError, BankAccountEvent] {
    val id: String
    val version: Int

    /**
     * Compose the id/tenant/version check to the command validation.
     */
    protected def checkCommand(bankAccount: BankAccount)(result: ⇒ BankAccountCommandResult): BankAccountCommandResult =
      if (id == bankAccount.id && version == bankAccount.version) {
        result
      } else {
        Left(BankAccountError(s"Incompatible command error - Product details: $bankAccount, command: $this"))
      }
  }

  final case class BankAccountCreated(id: String, accountHolder: String, instant: Instant) extends BankAccountEvent {
    def applyTo(bankAccount: BankAccount): BankAccount = bankAccount.copy(
      id            = id,
      accountHolder = accountHolder,
      lastEdit      = instant,
      version       = bankAccount.version + 1
    )
  }

  final case class CreateBankAccount(id: String, accountHolder: String) extends BankAccountCommand {
    /**
     * Creation version will always be zero.
     */
    val version = 0

    def applyTo(productDetails: BankAccount): BankAccountCommandResult =
      Right(Some(BankAccountCreated(id, accountHolder, Instant.now())))
  }

  final case class AmountAdded(id: String, amount: Long, instant: Instant) extends BankAccountEvent {
    def applyTo(bankAccount: BankAccount): BankAccount = bankAccount.copy(
      lastEdit = instant,
      amount   = bankAccount.amount + amount,
      version  = bankAccount.version + 1
    )
  }

  final case class AddAmount(id: String, amount: Long, version: Int) extends BankAccountCommand {
    def applyTo(bankAccount: BankAccount): BankAccountCommandResult =
      checkCommand(bankAccount) {
        Right(Some(AmountAdded(id, amount, Instant.now())))
      }
  }

  final case class AmountSubtracted(id: String, amount: Long, instant: Instant) extends BankAccountEvent {
    def applyTo(bankAccount: BankAccount): BankAccount = bankAccount.copy(
      lastEdit = instant,
      amount   = bankAccount.amount - amount,
      version  = bankAccount.version + 1
    )
  }

  final case class SubtractAmount(id: String, amount: Long, version: Int) extends BankAccountCommand {
    def applyTo(bankAccount: BankAccount): BankAccountCommandResult =
      checkCommand(bankAccount) {
        if (bankAccount.amount >= amount) {
          Right(Some(AmountSubtracted(id, amount, Instant.now())))
        } else {
          Left(BankAccountError(s"Bank credit exhausted: ${bankAccount.amount}"))
        }
      }
  }
}
