package com.meugestor.app.util

import java.text.NumberFormat
import java.util.Locale

object CurrencyUtils {
    private val brLocale = Locale("pt", "BR")
    private val currencyFormat = NumberFormat.getCurrencyInstance(brLocale)

    fun formatBRL(value: Double): String = currencyFormat.format(value)

    fun formatPercent(value: Double): String = String.format(brLocale, "%.1f%%", value)

    fun formatCompact(value: Double): String {
        return when {
            value >= 1_000_000 -> String.format(brLocale, "R$ %.1fM", value / 1_000_000)
            value >= 1_000 -> String.format(brLocale, "R$ %.1fK", value / 1_000)
            else -> formatBRL(value)
        }
    }
}
