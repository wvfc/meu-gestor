package com.meugestor.app.domain

import com.meugestor.app.data.database.entity.RecurrenceType
import com.meugestor.app.data.database.entity.TransactionEntity
import com.meugestor.app.data.database.entity.TransactionStatus
import com.meugestor.app.data.database.entity.TransactionType
import com.meugestor.app.util.DateUtils

/** Dados preenchidos no formulário de lançamento (receita ou despesa em conta). */
data class TransactionDraft(
    val type: TransactionType,
    val description: String,
    val amount: Double,
    val date: String,
    val isPaid: Boolean,
    val accountId: Long,
    val categoryId: Long?,
    val notes: String?,
    val repeatMonths: Int = 1
) {
    companion object {
        const val MAX_REPEAT_MONTHS = 36
    }

    private val status: TransactionStatus
        get() = if (isPaid) TransactionStatus.PAID else TransactionStatus.PENDING

    /**
     * Converte o rascunho em lançamentos. Com repetição mensal, o primeiro usa o status
     * escolhido e os meses seguintes são criados como pendentes (aparecem em "Futuro").
     */
    fun toEntities(now: String = DateUtils.today()): List<TransactionEntity> {
        val baseDate = DateUtils.parse(date) ?: DateUtils.todayDate()
        val count = repeatMonths.coerceIn(1, MAX_REPEAT_MONTHS)
        return List(count) { index ->
            val itemStatus = if (index == 0) status else TransactionStatus.PENDING
            val itemDate = DateUtils.toIso(baseDate.plusMonths(index.toLong()))
            TransactionEntity(
                type = type,
                description = description.trim(),
                amount = amount,
                categoryId = categoryId,
                accountId = accountId,
                date = itemDate,
                dueDate = if (itemStatus == TransactionStatus.PENDING) itemDate else null,
                paymentDate = if (itemStatus == TransactionStatus.PAID) now else null,
                recurrenceType = if (count > 1) RecurrenceType.MONTHLY else RecurrenceType.NONE,
                status = itemStatus,
                isFixed = count > 1,
                notes = notes?.trim()?.takeIf { it.isNotEmpty() },
                installmentNumber = if (count > 1) index + 1 else null,
                totalInstallments = if (count > 1) count else null,
                createdAt = now,
                updatedAt = now
            )
        }
    }

    /** Aplica o rascunho sobre um lançamento existente (edição de um único item). */
    fun applyTo(existing: TransactionEntity, now: String = DateUtils.today()): TransactionEntity {
        val newStatus = if (existing.status == TransactionStatus.CANCELLED) existing.status else status
        return existing.copy(
            type = type,
            description = description.trim(),
            amount = amount,
            categoryId = categoryId,
            accountId = accountId,
            date = date,
            dueDate = when {
                newStatus != TransactionStatus.PENDING -> existing.dueDate
                existing.creditCardId != null -> existing.dueDate ?: date
                else -> date
            },
            status = newStatus,
            paymentDate = when {
                newStatus != TransactionStatus.PAID -> null
                existing.status == TransactionStatus.PAID -> existing.paymentDate ?: now
                else -> now
            },
            notes = notes?.trim()?.takeIf { it.isNotEmpty() },
            updatedAt = now
        )
    }
}
