package com.meugestor.app.util

import com.meugestor.app.data.database.entity.*

/**
 * Dados de exemplo para desenvolvimento e testes.
 * Inserir via AppDatabase.PrepopulateCallback ou em testes unitários.
 */
object SampleData {

    val sampleAccounts = listOf(
        AccountEntity(
            name = "Conta Corrente", bankName = "Nubank",
            accountType = AccountType.CHECKING, balance = 3500.00, initialBalance = 5000.00,
            color = "#8A05BE", icon = "account_balance", createdAt = "2025-01-01"
        ),
        AccountEntity(
            name = "Poupança", bankName = "Caixa Econômica",
            accountType = AccountType.SAVINGS, balance = 8200.00, initialBalance = 8000.00,
            color = "#005CA9", icon = "savings", createdAt = "2025-01-01"
        ),
        AccountEntity(
            name = "Carteira", bankName = "Dinheiro",
            accountType = AccountType.CASH, balance = 450.00, initialBalance = 0.0,
            color = "#2E7D52", icon = "wallet", createdAt = "2025-01-01"
        ),
        AccountEntity(
            name = "Reserva de Emergência", bankName = "Inter",
            accountType = AccountType.EMERGENCY, balance = 15000.00, initialBalance = 15000.00,
            color = "#FF6B00", icon = "shield", notes = "Meta: 6 meses de despesas",
            createdAt = "2025-01-01"
        ),
        AccountEntity(
            name = "CDB / Tesouro", bankName = "XP Investimentos",
            accountType = AccountType.INVESTMENT, balance = 22000.00, initialBalance = 20000.00,
            color = "#1B5E20", icon = "trending_up", notes = "Renda fixa",
            createdAt = "2025-01-01"
        )
    )

    val sampleCreditCards = listOf(
        CreditCardEntity(
            name = "Nubank Roxinho", bankName = "Nubank", brand = CreditCardBrand.MASTERCARD,
            totalLimit = 8000.00, closingDay = 19, dueDay = 26,
            color = "#8A05BE", createdAt = "2025-01-01"
        ),
        CreditCardEntity(
            name = "Itaú Visa Gold", bankName = "Itaú", brand = CreditCardBrand.VISA,
            totalLimit = 12000.00, closingDay = 10, dueDay = 20,
            color = "#EC7000", createdAt = "2025-01-01"
        )
    )

    val sampleTransactions = listOf(
        // Receitas
        TransactionEntity(
            type = TransactionType.INCOME, description = "Salário",
            amount = 7500.00, categoryId = 1, accountId = 1,
            date = "2025-04-05", status = TransactionStatus.PAID,
            isFixed = true, recurrenceType = RecurrenceType.MONTHLY,
            createdAt = "2025-04-05", updatedAt = "2025-04-05"
        ),
        TransactionEntity(
            type = TransactionType.INCOME, description = "Freelance - App Design",
            amount = 1200.00, categoryId = 2, accountId = 1,
            date = "2025-04-12", status = TransactionStatus.PAID,
            createdAt = "2025-04-12", updatedAt = "2025-04-12"
        ),
        // Despesas pagas
        TransactionEntity(
            type = TransactionType.EXPENSE, description = "Supermercado",
            amount = 650.00, categoryId = 5, accountId = 1,
            date = "2025-04-02", status = TransactionStatus.PAID,
            isFixed = false, createdAt = "2025-04-02", updatedAt = "2025-04-02"
        ),
        TransactionEntity(
            type = TransactionType.EXPENSE, description = "Aluguel",
            amount = 1800.00, categoryId = 6, accountId = 1,
            date = "2025-04-05", dueDate = "2025-04-05",
            status = TransactionStatus.PAID, isFixed = true,
            recurrenceType = RecurrenceType.MONTHLY,
            createdAt = "2025-04-05", updatedAt = "2025-04-05"
        ),
        TransactionEntity(
            type = TransactionType.EXPENSE, description = "Academia",
            amount = 120.00, categoryId = 8, accountId = 1,
            date = "2025-04-01", dueDate = "2025-04-01",
            status = TransactionStatus.PAID, isFixed = true,
            recurrenceType = RecurrenceType.MONTHLY,
            createdAt = "2025-04-01", updatedAt = "2025-04-01"
        ),
        TransactionEntity(
            type = TransactionType.EXPENSE, description = "Netflix",
            amount = 39.90, categoryId = 12, accountId = 1,
            date = "2025-04-03", status = TransactionStatus.PAID,
            isFixed = true, recurrenceType = RecurrenceType.MONTHLY,
            createdAt = "2025-04-03", updatedAt = "2025-04-03"
        ),
        TransactionEntity(
            type = TransactionType.EXPENSE, description = "Spotify",
            amount = 21.90, categoryId = 12, accountId = 1,
            date = "2025-04-03", status = TransactionStatus.PAID,
            isFixed = true, recurrenceType = RecurrenceType.MONTHLY,
            createdAt = "2025-04-03", updatedAt = "2025-04-03"
        ),
        TransactionEntity(
            type = TransactionType.EXPENSE, description = "Combustível",
            amount = 280.00, categoryId = 7, accountId = 1,
            date = "2025-04-08", status = TransactionStatus.PAID,
            createdAt = "2025-04-08", updatedAt = "2025-04-08"
        ),
        TransactionEntity(
            type = TransactionType.EXPENSE, description = "Farmácia",
            amount = 95.50, categoryId = 8, accountId = 1,
            date = "2025-04-10", status = TransactionStatus.PAID,
            createdAt = "2025-04-10", updatedAt = "2025-04-10"
        ),
        // Pendentes
        TransactionEntity(
            type = TransactionType.EXPENSE, description = "Internet",
            amount = 119.90, categoryId = 12, accountId = 1,
            date = "2025-04-15", dueDate = "2025-04-20",
            status = TransactionStatus.PENDING, isFixed = true,
            recurrenceType = RecurrenceType.MONTHLY,
            createdAt = "2025-04-01", updatedAt = "2025-04-01"
        ),
        TransactionEntity(
            type = TransactionType.EXPENSE, description = "IPTU",
            amount = 350.00, categoryId = 13, accountId = 1,
            date = "2025-04-20", dueDate = "2025-04-25",
            status = TransactionStatus.PENDING,
            createdAt = "2025-04-01", updatedAt = "2025-04-01"
        )
    )

    val sampleGoals = listOf(
        GoalEntity(
            name = "Alimentação", categoryId = 5, targetAmount = 800.00,
            period = GoalPeriod.MONTHLY,
            startDate = "2025-04-01", endDate = "2025-04-30",
            createdAt = "2025-04-01"
        ),
        GoalEntity(
            name = "Lazer", categoryId = 10, targetAmount = 400.00,
            period = GoalPeriod.MONTHLY,
            startDate = "2025-04-01", endDate = "2025-04-30",
            createdAt = "2025-04-01"
        ),
        GoalEntity(
            name = "Combustível", categoryId = 7, targetAmount = 350.00,
            period = GoalPeriod.MONTHLY,
            startDate = "2025-04-01", endDate = "2025-04-30",
            createdAt = "2025-04-01"
        )
    )

    val sampleTaxChecklist = listOf(
        "Informe de rendimentos do empregador",
        "Informes de rendimentos de bancos e corretoras",
        "Recibos de consultas médicas (titulares e dependentes)",
        "Recibos de plano de saúde",
        "Comprovantes de despesas com educação",
        "Comprovante de previdência privada (PGBL)",
        "Comprovante de doações com dedução",
        "Escrituras ou contratos de imóveis",
        "Documentos de veículos",
        "Informes de investimentos (CDB, LCI, LCA, Tesouro)",
        "DARF de ganho de capital (venda de imóvel ou ações)",
        "Comprovantes de despesas profissionais (autônomos)",
        "CPF dos dependentes",
        "Título de eleitor (caso primeira declaração)"
    )
}
