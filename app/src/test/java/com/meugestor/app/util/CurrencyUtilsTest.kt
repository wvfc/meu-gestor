package com.meugestor.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CurrencyUtilsTest {

    private fun assertParsed(expected: Double, input: String) {
        val value = CurrencyUtils.parseAmount(input)
        assertTrue("\"$input\" deveria ser convertido", value != null)
        assertEquals("\"$input\"", expected, value!!, 0.0001)
    }

    @Test
    fun `aceita formato brasileiro`() {
        assertParsed(1234.56, "1.234,56")
        assertParsed(1234.56, "1234,56")
        assertParsed(0.5, "0,5")
        assertParsed(1_234_567.0, "1.234.567")
        assertParsed(10.0, "R$ 10")
    }

    @Test
    fun `aceita formato internacional`() {
        assertParsed(1234.56, "1234.56")
        assertParsed(1234.56, "1,234.56")
        assertParsed(12.5, "12.5")
    }

    @Test
    fun `ponto com tres casas eh separador de milhar`() {
        assertParsed(1234.0, "1.234")
    }

    @Test
    fun `aceita negativos`() {
        assertParsed(-50.25, "-50,25")
    }

    @Test
    fun `rejeita entradas invalidas`() {
        assertNull(CurrencyUtils.parseAmount(""))
        assertNull(CurrencyUtils.parseAmount("abc"))
        assertNull(CurrencyUtils.parseAmount("-"))
        assertNull(CurrencyUtils.parseAmount("12a"))
    }

    @Test
    fun `filtro de digitacao mantem sinal apenas no inicio`() {
        assertEquals("12,5", CurrencyUtils.filterAmountInput("1a2,5"))
        assertEquals("-125", CurrencyUtils.filterAmountInput("-12-5", allowNegative = true))
        assertEquals("125", CurrencyUtils.filterAmountInput("12-5", allowNegative = false))
    }

    @Test
    fun `valor para edicao usa virgula e volta ao mesmo numero`() {
        val text = CurrencyUtils.toInputString(1234.5)
        assertEquals("1234,50", text)
        assertParsed(1234.5, text)
    }

    @Test
    fun `formata em reais`() {
        val formatted = CurrencyUtils.formatBRL(1234.56).replace(' ', ' ')
        assertEquals("R$ 1.234,56", formatted)
    }
}
