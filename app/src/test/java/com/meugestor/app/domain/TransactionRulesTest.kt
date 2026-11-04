package com.meugestor.app.domain

import com.meugestor.app.data.database.entity.RecurrenceType
import com.meugestor.app.data.database.entity.TransactionEntity
import com.meugestor.app.data.database.entity.TransactionStatus
import com.meugestor.app.data.database.entity.TransactionType
import com.meugestor.app.util.DateUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class TransactionRulesTest {

    private fun tx(
        type: TransactionType = TransactionType.EXPENSE,
        status: TransactionStatus = TransactionStatus.PAID,
        amount: Double = 100.0,
        date: String = "2025-04-10",
        dueDate: String? = null,
        applied: Boolean = false
    ) = TransactionEntity(
        type = type, description = "Teste", amount = amount, accountId = 1, date = date,
        dueDate = dueDate, status = status, createdAt = date, updatedAt = date, balanceApplied = applied
    )

    @Test
    fun `somente lancamentos pagos movimentam o saldo`() {
        assertTrue(BalanceRules.shouldApply(tx(status = TransactionStatus.PAID)))
        assertFalse(BalanceRules.shouldApply(tx(status = TransactionStatus.PENDING)))
        assertFalse(BalanceRules.shouldApply(tx(status = TransactionStatus.CANCELLED)))
    }

    @Test
    fun `sinal do valor depende do tipo`() {
        assertEquals(-100.0, BalanceRules.signedAmount(tx(type = TransactionType.EXPENSE)), 0.0)
        assertEquals(100.0, BalanceRules.signedAmount(tx(type = TransactionType.INCOME)), 0.0)
        assertEquals(0.0, BalanceRules.appliedEffect(tx(applied = false)), 0.0)
        assertEquals(-100.0, BalanceRules.appliedEffect(tx(applied = true)), 0.0)
    }

    @Test
    fun `pendencia vencida aparece como atrasada`() {
        val today = LocalDate.of(2025, 4, 10)
        val overdue = tx(status = TransactionStatus.PENDING, dueDate = "2025-04-09")
        val dueToday = tx(status = TransactionStatus.PENDING, dueDate = "2025-04-10")
        val paidLate = tx(status = TransactionStatus.PAID, dueDate = "2025-04-01")
        assertEquals(TransactionStatus.OVERDUE, overdue.displayStatus(today))
        assertEquals(TransactionStatus.PENDING, dueToday.displayStatus(today))
        assertEquals(TransactionStatus.PAID, paidLate.displayStatus(today))
    }

    @Test
    fun `alternar status registra data de pagamento`() {
        val paid = tx(status = TransactionStatus.PENDING).withStatus(TransactionStatus.PAID, today = "2025-04-12")
        assertEquals("2025-04-12", paid.paymentDate)
        assertNull(paid.withStatus(TransactionStatus.PENDING, today = "2025-04-13").paymentDate)
    }

    @Test
    fun `repeticao mensal cria proximos meses pendentes`() {
        val draft = TransactionDraft(
            type = TransactionType.EXPENSE, description = "  Aluguel ", amount = 1500.0,
            date = "2025-01-31", isPaid = true, accountId = 1, categoryId = 6, notes = " ", repeatMonths = 3
        )
        val items = draft.toEntities(now = "2025-01-31")
        assertEquals(3, items.size)
        assertEquals(listOf("2025-01-31", "2025-02-28", "2025-03-31"), items.map { it.date })
        assertEquals(
            listOf(TransactionStatus.PAID, TransactionStatus.PENDING, TransactionStatus.PENDING),
            items.map { it.status }
        )
        assertEquals(listOf(null, "2025-02-28", "2025-03-31"), items.map { it.dueDate })
        assertEquals("Aluguel", items.first().description)
        assertNull(items.first().notes)
        assertTrue(items.all { it.recurrenceType == RecurrenceType.MONTHLY && it.isFixed })
    }

    @Test
    fun `lancamento unico nao vira serie`() {
        val item = TransactionDraft(
            type = TransactionType.INCOME, description = "Salário", amount = 5000.0,
            date = DateUtils.today(), isPaid = false, accountId = 1, categoryId = 1, notes = null
        ).toEntities().single()
        assertEquals(TransactionStatus.PENDING, item.status)
        assertEquals(item.date, item.dueDate)
        assertNull(item.totalInstallments)
        assertFalse(item.isPartOfSeries())
    }

    @Test
    fun `edicao mantem id e atualiza pagamento`() {
        val existing = tx(status = TransactionStatus.PENDING, dueDate = "2025-04-10").copy(id = 42)
        val draft = TransactionDraft(
            type = TransactionType.EXPENSE, description = "Luz", amount = 180.0, date = "2025-04-11",
            isPaid = true, accountId = 2, categoryId = null, notes = "conta de abril"
        )
        val updated = draft.applyTo(existing, now = "2025-04-12")
        assertEquals(42L, updated.id)
        assertEquals(TransactionStatus.PAID, updated.status)
        assertEquals("2025-04-12", updated.paymentDate)
        assertEquals(2L, updated.accountId)
        assertEquals("conta de abril", updated.notes)
    }
}
