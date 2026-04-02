package com.meugestor.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val title: String,
    val icon: ImageVector? = null,
    val selectedIcon: ImageVector? = null
) {
    data object Home : Screen("home", "Início", Icons.Outlined.Home, Icons.Filled.Home)
    data object CashFlow : Screen("cashflow", "Caixa", Icons.Outlined.AccountBalanceWallet, Icons.Filled.AccountBalanceWallet)
    data object CreditCards : Screen("credit_cards", "Cartões", Icons.Outlined.CreditCard, Icons.Filled.CreditCard)
    data object Future : Screen("future", "Futuro", Icons.Outlined.CalendarMonth, Icons.Filled.CalendarMonth)
    data object Reports : Screen("reports", "Relatórios", Icons.Outlined.BarChart, Icons.Filled.BarChart)
    data object TaxOrganizer : Screen("tax_organizer", "IR", Icons.Outlined.Description, Icons.Filled.Description)
    data object Goals : Screen("goals", "Metas", Icons.Outlined.Flag, Icons.Filled.Flag)
    data object Reserves : Screen("reserves", "Reservas", Icons.Outlined.Savings, Icons.Filled.Savings)
    data object Settings : Screen("settings", "Configurações", Icons.Outlined.Settings, Icons.Filled.Settings)
    data object Onboarding : Screen("onboarding", "Bem-vindo")
    data object AddTransaction : Screen("add_transaction", "Novo Lançamento")
    data object AddAccount : Screen("add_account", "Nova Conta")
    data object AddCreditCard : Screen("add_credit_card", "Novo Cartão")
    data object AddGoal : Screen("add_goal", "Nova Meta")

    companion object {
        val bottomNavItems = listOf(Home, CashFlow, CreditCards, Future, Reports)
        val drawerItems = listOf(TaxOrganizer, Goals, Reserves, Settings)
    }
}
