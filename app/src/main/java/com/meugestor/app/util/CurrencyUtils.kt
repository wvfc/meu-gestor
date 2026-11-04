package com.meugestor.app.util

import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

object CurrencyUtils {
    private val brLocale = Locale("pt", "BR")

    // NumberFormat não é thread-safe; cada thread recebe a sua instância.
    private val currencyFormat = ThreadLocal.withInitial { NumberFormat.getCurrencyInstance(brLocale) }

    fun formatBRL(value: Double): String = currencyFormat.get()!!.format(value)

    fun formatPercent(value: Double): String = String.format(brLocale, "%.1f%%", value)

    fun formatCompact(value: Double): String {
        val sign = if (value < 0) "-" else ""
        val absolute = abs(value)
        return when {
            absolute >= 1_000_000 -> String.format(brLocale, "%sR$ %.1fM", sign, absolute / 1_000_000)
            absolute >= 1_000 -> String.format(brLocale, "%sR$ %.1fK", sign, absolute / 1_000)
            else -> formatBRL(value)
        }
    }

    /**
     * Filtra a digitação de um campo de valor, mantendo apenas dígitos, separadores e,
     * opcionalmente, o sinal negativo no início.
     */
    fun filterAmountInput(input: String, allowNegative: Boolean = false): String {
        val filtered = input.filter { it.isDigit() || it == ',' || it == '.' || (allowNegative && it == '-') }
        if (!allowNegative) return filtered
        val negative = filtered.startsWith("-")
        val digits = filtered.replace("-", "")
        return if (negative) "-$digits" else digits
    }

    /**
     * Converte valores digitados no padrão brasileiro ou internacional:
     * "1.234,56", "1234,56", "1234.56", "1,234.56" e "R$ 10" são aceitos.
     */
    fun parseAmount(input: String): Double? {
        var text = input.trim()
            .replace("R$", "")
            .replace(" ", "")
            .replace(" ", "")
        if (text.isEmpty()) return null

        val negative = text.startsWith("-")
        text = text.removePrefix("-")
        if (text.isEmpty() || text.any { !it.isDigit() && it != ',' && it != '.' }) return null

        val lastComma = text.lastIndexOf(',')
        val lastDot = text.lastIndexOf('.')
        val normalized = when {
            lastComma >= 0 && lastDot >= 0 -> {
                // O separador que aparece por último é o decimal.
                if (lastComma > lastDot) text.replace(".", "").replace(',', '.')
                else text.replace(",", "")
            }
            lastComma >= 0 -> {
                if (text.count { it == ',' } > 1) text.replace(",", "") else text.replace(',', '.')
            }
            lastDot >= 0 -> {
                val decimals = text.length - lastDot - 1
                // "1.234" ou "1.234.567" são milhares; "12.5" é decimal.
                if (text.count { it == '.' } > 1 || decimals == 3) text.replace(".", "") else text
            }
            else -> text
        }
        val value = normalized.toDoubleOrNull() ?: return null
        return if (negative) -value else value
    }

    /** Formata um número para edição em campo de texto (ex.: 1234.5 -> "1234,50"). */
    fun toInputString(value: Double): String = String.format(Locale.US, "%.2f", value).replace('.', ',')
}
