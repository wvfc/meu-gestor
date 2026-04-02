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
import com.meugestor.app.ui.components.EmptyState
import com.meugestor.app.ui.components.StatusBadge
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

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Greeting
        item {
            Column {
                Text(
                    text = "Olá! \uD83D\uDC4B",
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
                        Column {
                            Text("Em contas", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f))
                            Text(CurrencyUtils.formatBRL(state.totalInAccounts),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.SemiBold)
                        }
                        Column {
                            Text("Em dinheiro", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f))
                            Text(CurrencyUtils.formatBRL(state.totalCash),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.SemiBold)
                        }
                        Column {
                            Text("A pagar", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f))
                            Text(CurrencyUtils.formatBRL(state.totalPayable),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.SemiBold)
                        }
                    }
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
                    title = "Receitas",
                    value = state.monthlyIncome,
                    icon = Icons.Filled.TrendingUp,
                    valueColor = IncomeGreen
                )
                SummaryCard(
                    modifier = Modifier.weight(1f),
                    title = "Despesas",
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
                        onClick = { onNavigate(Screen.CashFlow.route) },
                        label = { Text("Nova Receita") },
                        leadingIcon = { Icon(Icons.Outlined.Add, null, tint = IncomeGreen) }
                    )
                }
                item {
                    AssistChip(
                        onClick = { onNavigate(Screen.CashFlow.route) },
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
            Text("Contas a Vencer", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
        if (state.upcomingDueItems.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Text(
                        text = "Nenhuma conta próxima do vencimento",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(state.upcomingDueItems) { item ->
                DueItemCard(item)
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
                    description = "Adicione sua primeira receita ou despesa"
                )
            }
        } else {
            items(state.recentTransactions.take(5)) { tx ->
                TransactionListItem(tx)
            }
        }
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

@Composable
private fun DueItemCard(transaction: com.meugestor.app.data.database.entity.TransactionEntity) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(transaction.description, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text(
                    text = "Vence: ${DateUtils.formatDate(transaction.dueDate ?: "")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    CurrencyUtils.formatBRL(transaction.amount),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = ExpenseRed
                )
                StatusBadge(transaction.status.name)
            }
        }
    }
}

@Composable
private fun TransactionListItem(transaction: com.meugestor.app.data.database.entity.TransactionEntity) {
    val isIncome = transaction.type.name == "INCOME"
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(40.dp),
            shape = MaterialTheme.shapes.small,
            color = if (isIncome) IncomeGreen.copy(alpha = 0.1f) else ExpenseRed.copy(alpha = 0.1f)
        ) {
            Icon(
                imageVector = if (isIncome) Icons.Filled.TrendingUp else Icons.Filled.TrendingDown,
                contentDescription = null,
                modifier = Modifier.padding(8.dp),
                tint = if (isIncome) IncomeGreen else ExpenseRed
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(transaction.description, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
            Text(
                DateUtils.formatDate(transaction.date),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = "${if (isIncome) "+" else "-"} ${CurrencyUtils.formatBRL(transaction.amount)}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (isIncome) IncomeGreen else ExpenseRed
        )
    }
}
