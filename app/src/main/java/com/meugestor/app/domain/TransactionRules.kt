package com.meugestor.app.domain

import com.meugestor.app.data.database.entity.TransactionEntity
import com.meugestor.app.data.database.entity.TransactionStatus
import com.meugestor.app.data.database.entity.TransactionType
import com.meugestor.app.util.DateUtils
import java.time.LocalDate

/**
 * Regras centrais de como um lançamento afeta o saldo das contas.
 *
 * Um lançamento só movimenta o saldo quando está realizado (status PAID). Isso vale para
 * receitas, despesas e compras no cartão: a compra no cartão passa a sair da conta quando é
 * marcada como paga (pagamento da fatura). Lançamentos pendentes aparecem em "A pagar/A receber"
 * e no saldo previsto, mas não no saldo atual.
 */
object BalanceRules {

    fun shouldApply(transaction: TransactionEntity): Boolean =
        transaction.status == TransactionStatus.PAID

    /** Valor com sinal que o lançamento representa para a conta. */
    fun signedAmount(transaction: TransactionEntity): Double = when (transaction.type) {
        TransactionType.INCOME -> transaction.amount
        TransactionType.EXPENSE -> -transaction.amount
        TransactionType.TRANSFER -> -transaction.amount
        TransactionType.ADJUSTMENT -> transaction.amount
    }

    /** Efeito que já está aplicado no saldo (zero se o lançamento ainda não foi aplicado). */
    fun appliedEffect(transaction: TransactionEntity): Double =
        if (transaction.balanceApplied) signedAmount(transaction) else 0.0
}

/** Data de vencimento efetiva: vencimento informado ou, na falta dele, a data do lançamento. */
fun TransactionEntity.effectiveDueIso(): String = dueDate ?: date

fun TransactionEntity.effectiveDueDate(): LocalDate? = DateUtils.parse(effectiveDueIso())

fun TransactionEntity.isOpen(): Boolean =
    status == TransactionStatus.PENDING || status == TransactionStatus.OVERDUE

/** Pendente com vencimento anterior a hoje. */
fun TransactionEntity.isOverdue(today: LocalDate = DateUtils.todayDate()): Boolean {
    if (!isOpen()) return false
    val due = effectiveDueDate() ?: return false
    return due.isBefore(today)
}

/** Status para exibição: pendências vencidas aparecem como "Atrasado". */
fun TransactionEntity.displayStatus(today: LocalDate = DateUtils.todayDate()): TransactionStatus =
    if (isOverdue(today)) TransactionStatus.OVERDUE else status

/** Id do lançamento que originou a série (parcelas ou repetições). */
fun TransactionEntity.seriesRootId(): Long = parentTransactionId ?: id

fun TransactionEntity.isPartOfSeries(): Boolean =
    parentTransactionId != null || (totalInstallments ?: 1) > 1

/** Novo status ao alternar entre realizado e pendente. */
fun TransactionEntity.withStatus(newStatus: TransactionStatus, today: String = DateUtils.today()): TransactionEntity =
    copy(
        status = newStatus,
        paymentDate = if (newStatus == TransactionStatus.PAID) today else null,
        updatedAt = today
    )
