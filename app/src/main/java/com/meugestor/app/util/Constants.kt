package com.meugestor.app.util

object Constants {
    const val DATABASE_NAME = "meu_gestor.db"
    const val DATE_FORMAT_ISO = "yyyy-MM-dd"
    const val DATE_FORMAT_BR = "dd/MM/yyyy"
    const val CURRENCY_CODE = "BRL"

    val DEFAULT_INCOME_CATEGORIES = listOf(
        "Salário", "Renda Extra", "Vendas", "Comissões",
        "Reembolsos", "Investimentos", "Outros Recebimentos"
    )

    val DEFAULT_EXPENSE_CATEGORIES = listOf(
        "Moradia", "Alimentação", "Transporte", "Saúde",
        "Educação", "Lazer", "Assinaturas", "Impostos",
        "Família", "Trabalho", "Vestuário", "Presentes",
        "Manutenção", "Imprevistos", "Outros"
    )

    val CATEGORY_ICONS = mapOf(
        "Salário" to "💰", "Moradia" to "🏠", "Alimentação" to "🍽️",
        "Transporte" to "🚗", "Saúde" to "🏥", "Educação" to "📚",
        "Lazer" to "🎮", "Assinaturas" to "📱", "Impostos" to "📋",
        "Família" to "👨‍👩‍👧‍👦", "Trabalho" to "💼", "Investimentos" to "📈",
        "Vestuário" to "👕", "Presentes" to "🎁", "Outros" to "📌"
    )

    val CATEGORY_COLORS = mapOf(
        "Moradia" to 0xFF4CAF50, "Alimentação" to 0xFFFF9800,
        "Transporte" to 0xFF2196F3, "Saúde" to 0xFFE91E63,
        "Educação" to 0xFF9C27B0, "Lazer" to 0xFFFFEB3B,
        "Assinaturas" to 0xFF00BCD4, "Impostos" to 0xFF795548,
        "Família" to 0xFFFF5722, "Trabalho" to 0xFF607D8B,
        "Salário" to 0xFF43A047, "Investimentos" to 0xFF1B5E20
    )
}
