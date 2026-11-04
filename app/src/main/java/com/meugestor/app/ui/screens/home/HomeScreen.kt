@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.meugestor.app.ui.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.meugestor.app.MeuGestorApp
import com.meugestor.app.data.database.entity.TransactionType
import com.meugestor.app.ui.common.CollectMessages
import com.meugestor.app.ui.common.TransactionDialogsHost
import com.meugestor.app.ui.common.rememberTransactionDialogState
import com.meugestor.app.ui.common.rememberTransactionLookups
import com.meugestor.app.ui.components.EmptyState
import com.meugestor.app.ui.components.TransactionRow
import com.meugestor.app.ui.navigation.Screen
import com.meugestor.app.ui.theme.*
import com.meugestor.app.util.CurrencyUtils
import com.meugestor.app.util.DateUtils

@Composable
fun HomeScreen(app: MeuGestorApp, onNavigate: (String) -> Unit) {
    val viewModel: HomeViewModel = viewModel(
        factory = HomeViewModel.Factory(app.transactionRepository, app.accountRepository)
    )
    val state by viewModel.uiState.collectAsState()
    val dialogs = rememberTransactionDialogState()
    val lookups = rememberTransactionLookups(app)

    CollectMessages(viewModel.messages)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Greeting
        item {
            Column {
                Text(
                    text = "Olá! 👋",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = DateUtils.formatMonthYear(DateUtils.today()),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Main balance card
        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Saldo Total",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = CurrencyUtils.formatBRL(state.totalBalance),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        BalanceDetail("Em contas", state.totalInAccounts)
                        BalanceDetail("Em dinheiro", state.totalCash)
                        BalanceDetail("A pagar", state.totalPayable)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Saldo previsto após pendências: ${CurrencyUtils.formatBRL(state.projectedBalance)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                    )
                }
            }
        }

        // Income/Expense cards row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SummaryCard(
                    modifier = Modifier.weight(1f),
                    title = "Receitas do mês",
                    value = state.monthlyIncome,
                    icon = Icons.Filled.TrendingUp,
                    valueColor = IncomeGreen
                )
                SummaryCard(
                    modifier = Modifier.weight(1f),
                    title = "Despesas do mês",
                    value = state.monthlyExpenses,
                    icon = Icons.Filled.TrendingDown,
                    valueColor = ExpenseRed
                )
            }
        }

        // Quick actions
        item {
            Text("Ações Rápidas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    AssistChip(
                        onClick = { dialogs.create(TransactionType.INCOME) },
                        label = { Text("Nova Receita") },
                        leadingIcon = { Icon(Icons.Outlined.Add, null, tint = IncomeGreen) }
                    )
                }
                item {
                    AssistChip(
                        onClick = { dialogs.create(TransactionType.EXPENSE) },
                        label = { Text("Nova Despesa") },
                        leadingIcon = { Icon(Icons.Outlined.Remove, null, tint = ExpenseRed) }
                    )
                }
                item {
                    AssistChip(
                        onClick = { onNavigate(Screen.CreditCards.route) },
                        label = { Text("Cartões") },
                        leadingIcon = { Icon(Icons.Outlined.CreditCard, null) }
                    )
                }
                item {
                    AssistChip(
                        onClick = { onNavigate(Screen.Reports.route) },
                        label = { Text("Relatórios") },
                        leadingIcon = { Icon(Icons.Outlined.BarChart, null) }
                    )
                }
            }
        }

        // Upcoming due items
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Contas a Vencer", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    if (state.overdueCount > 0) {
                        Text(
                            "${state.overdueCount} em atraso",
                            style = MaterialTheme.typography.bodySmall,
                            color = ExpenseRed
                        )
                    }
                }
                TextButton(onClick = { onNavigate(Screen.Future.route) }) {
                    Text("Ver futuro")
                }
            }
        }
        if (state.upcomingDueItems.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Text(
                        text = "Nenhuma conta vencendo nos próximos 7 dias",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(state.upcomingDueItems, key = { "due_${it.id}" }) { item ->
                TransactionRow(
                    transaction = item,
                    categoryName = lookups.categoryName(item),
                    showDueInfo = true,
                    onClick = { dialogs.openDetails(item) }
                )
            }
        }

        // Recent transactions
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Últimos Lançamentos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                TextButton(onClick = { onNavigate(Screen.CashFlow.route) }) {
                    Text("Ver todos")
                }
            }
        }
        if (state.recentTransactions.isEmpty() && !state.isLoading) {
            item {
                EmptyState(
                    icon = Icons.Outlined.AccountBalanceWallet,
                    title = "Sem lançamentos ainda",
                    description = "Adicione sua primeira receita ou despesa",
                    actionLabel = "Novo Lançamento",
                    onAction = { dialogs.create(TransactionType.EXPENSE) }
                )
            }
        } else {
            items(state.recentTransactions, key = { "recent_${it.id}" }) { tx ->
                TransactionRow(
                    transaction = tx,
                    categoryName = lookups.categoryName(tx),
                    onClick = { dialogs.openDetails(tx) }
                )
            }
        }
    }

    TransactionDialogsHost(app = app, viewModel = viewModel, state = dialogs, lookups = lookups)
}

@Composable
private fun BalanceDetail(label: String, value: Double) {
    Column {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
        )
        Text(
            CurrencyUtils.formatBRL(value),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimary,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun SummaryCard(
    modifier: Modifier = Modifier,
    title: String,
    value: Double,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    valueColor: androidx.compose.ui.graphics.Color
) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = valueColor, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.labelLarge)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = CurrencyUtils.formatBRL(value),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = valueColor
            )
        }
    }
}
