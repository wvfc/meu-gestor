package com.meugestor.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.YearMonth

class DateUtilsTest {

    @Test
    fun `vencimento hoje nao esta atrasado`() {
        val today = DateUtils.today()
        assertFalse(DateUtils.isOverdue(today))
        assertEquals(0, DateUtils.daysUntilDue(today))
    }

    @Test
    fun `ontem esta atrasado e amanha falta um dia`() {
        val yesterday = DateUtils.toIso(DateUtils.todayDate().minusDays(1))
        val tomorrow = DateUtils.toIso(DateUtils.todayDate().plusDays(1))
        assertTrue(DateUtils.isOverdue(yesterday))
        assertEquals(-1, DateUtils.daysUntilDue(yesterday))
        assertEquals(1, DateUtils.daysUntilDue(tomorrow))
    }

    @Test
    fun `formata datas`() {
        assertEquals("05/04/2025", DateUtils.formatDate("2025-04-05"))
        assertEquals("Abril 2025", DateUtils.formatYearMonth(YearMonth.of(2025, 4)))
        assertEquals("invalida", DateUtils.formatDate("invalida"))
    }

    @Test
    fun `soma meses respeitando fim de mes`() {
        assertEquals("2025-02-28", DateUtils.addMonths("2025-01-31", 1))
        assertEquals("2026-01-15", DateUtils.addMonths("2025-12-15", 1))
    }

    @Test
    fun `conversao de milissegundos do date picker`() {
        val millis = DateUtils.toUtcMillis("2025-04-05")!!
        assertEquals("2025-04-05", DateUtils.fromUtcMillis(millis))
        assertNull(DateUtils.toUtcMillis("x"))
    }

    @Test
    fun `semana comeca na segunda`() {
        val start = DateUtils.parse(DateUtils.getStartOfWeek())!!
        val end = DateUtils.parse(DateUtils.getEndOfWeek())!!
        assertEquals(java.time.DayOfWeek.MONDAY, start.dayOfWeek)
        assertEquals(java.time.DayOfWeek.SUNDAY, end.dayOfWeek)
        assertFalse(DateUtils.todayDate().isBefore(start))
        assertFalse(DateUtils.todayDate().isAfter(end))
    }
}
