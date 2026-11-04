package com.meugestor.app.domain

import com.meugestor.app.data.database.entity.CreditCardEntity
import com.meugestor.app.data.database.entity.TransactionEntity
import com.meugestor.app.data.database.entity.TransactionStatus
import com.meugestor.app.data.database.entity.TransactionType
import com.meugestor.app.util.DateUtils
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.roundToLong

/**
 * Cálculos de fatura do cartão de crédito.
 *
 * A fatura é identificada pelo mês em que fecha: compras feitas até o dia de fechamento entram
 * na fatura do mês; compras após o fechamento entram na fatura do mês seguinte.
 */
object CreditCardInvoice {

    const val MAX_INSTALLMENTS = 48

    fun invoiceMonth(purchaseDate: LocalDate, closingDay: Int): YearMonth {
        val closing = closingDay.coerceIn(1, purchaseDate.lengthOfMonth())
        val month = YearMonth.from(purchaseDate)
        return if (purchaseDate.dayOfMonth > closing) month.plusMonths(1) else month
    }

    /**
     * Vencimento de uma fatura. Se o vencimento é depois do fechamento, vence no próprio mês
     * (ex.: fecha 19, vence 26); caso contrário vence no mês seguinte (ex.: fecha 25, vence 5).
     */
    fun dueDate(invoiceMonth: YearMonth, closingDay: Int, dueDay: Int): LocalDate {
        val dueMonth = if (dueDay > closingDay) invoiceMonth else invoiceMonth.plusMonths(1)
        return dueMonth.atDay(dueDay.coerceIn(1, dueMonth.lengthOfMonth()))
    }

    fun dueDateForPurchase(purchaseDate: LocalDate, card: CreditCardEntity, installmentIndex: Int = 0): LocalDate {
        val month = invoiceMonth(purchaseDate, card.closingDay).plusMonths(installmentIndex.toLong())
        return dueDate(month, card.closingDay, card.dueDay)
    }

    /** Vencimento efetivo de um lançamento do cartão (lançamentos antigos não têm due_date). */
    fun effectiveDue(transaction: TransactionEntity, card: CreditCardEntity): LocalDate {
        DateUtils.parse(transaction.dueDate)?.let { return it }
        val purchase = DateUtils.parse(transaction.date) ?: DateUtils.todayDate()
        return dueDateForPurchase(purchase, card)
    }

    /** Divide um valor em parcelas sem perder centavos (a diferença fica na primeira parcela). */
    fun splitAmount(total: Double, installments: Int): List<Double> {
        val count = installments.coerceIn(1, MAX_INSTALLMENTS)
        val totalCents = (total * 100).roundToLong()
        val base = totalCents / count
        val remainder = totalCents - base * count
        return List(count) { index ->
            val cents = if (index == 0) base + remainder else base
            cents / 100.0
        }
    }

    /**
     * Gera uma linha por parcela, cada uma com sua data e vencimento de fatura.
     * O primeiro item é a parcela 1; as demais devem ser ligadas a ela via parent_transaction_id.
     */
    fun buildInstallments(
        card: CreditCardEntity,
        description: String,
        totalAmount: Double,
        installments: Int,
        purchaseDate: LocalDate,
        accountId: Long,
        categoryId: Long?,
        status: TransactionStatus,
        notes: String? = null,
        now: String = DateUtils.today()
    ): List<TransactionEntity> {
        val amounts = splitAmount(totalAmount, installments)
        val count = amounts.size
        return amounts.mapIndexed { index, amount ->
            TransactionEntity(
                type = TransactionType.EXPENSE,
                description = description,
                amount = amount,
                categoryId = categoryId,
                accountId = accountId,
                creditCardId = card.id,
                date = DateUtils.toIso(purchaseDate.plusMonths(index.toLong())),
                dueDate = DateUtils.toIso(dueDateForPurchase(purchaseDate, card, index)),
                paymentDate = if (status == TransactionStatus.PAID) now else null,
                status = status,
                notes = notes,
                installmentNumber = index + 1,
                totalInstallments = count,
                createdAt = now,
                updatedAt = now
            )
        }
    }
}
