package com.meugestor.app.domain

import com.meugestor.app.data.database.entity.CreditCardBrand
import com.meugestor.app.data.database.entity.CreditCardEntity
import com.meugestor.app.data.database.entity.TransactionStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class CreditCardInvoiceTest {

    private fun card(closing: Int, due: Int) = CreditCardEntity(
        id = 7, name = "Teste", bankName = "Banco", brand = CreditCardBrand.VISA,
        totalLimit = 5000.0, closingDay = closing, dueDay = due, color = "#000000", createdAt = "2025-01-01"
    )

    @Test
    fun `compra ate o fechamento entra na fatura do mes`() {
        assertEquals(YearMonth.of(2025, 4), CreditCardInvoice.invoiceMonth(LocalDate.of(2025, 4, 19), 19))
        assertEquals(YearMonth.of(2025, 5), CreditCardInvoice.invoiceMonth(LocalDate.of(2025, 4, 20), 19))
    }

    @Test
    fun `fechamento maior que o mes usa o ultimo dia`() {
        // Fevereiro tem 28 dias: compra no dia 28 com fechamento 30 ainda é da fatura de fevereiro.
        assertEquals(YearMonth.of(2025, 2), CreditCardInvoice.invoiceMonth(LocalDate.of(2025, 2, 28), 30))
    }

    @Test
    fun `vencimento depois do fechamento vence no mesmo mes`() {
        assertEquals(LocalDate.of(2025, 4, 26), CreditCardInvoice.dueDate(YearMonth.of(2025, 4), 19, 26))
    }

    @Test
    fun `vencimento antes do fechamento vence no mes seguinte`() {
        assertEquals(LocalDate.of(2025, 5, 5), CreditCardInvoice.dueDate(YearMonth.of(2025, 4), 25, 5))
        // Fatura de dezembro com vencimento no dia 10 cai em janeiro do ano seguinte.
        assertEquals(LocalDate.of(2026, 1, 10), CreditCardInvoice.dueDate(YearMonth.of(2025, 12), 28, 10))
    }

    @Test
    fun `divide parcelas sem perder centavos`() {
        val parts = CreditCardInvoice.splitAmount(100.0, 3)
        assertEquals(listOf(33.34, 33.33, 33.33), parts)
        assertEquals(100.0, parts.sum(), 0.0001)
        assertEquals(listOf(10.0), CreditCardInvoice.splitAmount(10.0, 0))
    }

    @Test
    fun `gera uma linha por parcela com vencimentos mensais`() {
        val items = CreditCardInvoice.buildInstallments(
            card = card(closing = 19, due = 26),
            description = "Notebook",
            totalAmount = 3000.0,
            installments = 3,
            purchaseDate = LocalDate.of(2025, 4, 25),
            accountId = 1,
            categoryId = 5,
            status = TransactionStatus.PENDING,
            now = "2025-04-25"
        )
        assertEquals(3, items.size)
        assertEquals(listOf("2025-05-26", "2025-06-26", "2025-07-26"), items.map { it.dueDate })
        assertEquals(listOf("2025-04-25", "2025-05-25", "2025-06-25"), items.map { it.date })
        assertEquals(listOf(1, 2, 3), items.map { it.installmentNumber })
        items.forEach {
            assertEquals(3, it.totalInstallments)
            assertEquals(7L, it.creditCardId)
            assertEquals(1000.0, it.amount, 0.0001)
            assertNull(it.paymentDate)
        }
    }

    @Test
    fun `lancamento antigo sem vencimento usa a data da compra`() {
        val tx = CreditCardInvoice.buildInstallments(
            card = card(closing = 10, due = 20), description = "x", totalAmount = 50.0,
            installments = 1, purchaseDate = LocalDate.of(2025, 3, 15), accountId = 1,
            categoryId = null, status = TransactionStatus.PENDING, now = "2025-03-15"
        ).first().copy(dueDate = null)
        assertEquals(LocalDate.of(2025, 4, 20), CreditCardInvoice.effectiveDue(tx, card(closing = 10, due = 20)))
    }
}
