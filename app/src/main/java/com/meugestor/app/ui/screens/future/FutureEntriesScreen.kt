package com.meugestor.app.ui.screens.future

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.meugestor.app.data.database.entity.TransactionEntity
import com.meugestor.app.data.database.entity.TransactionType
import com.meugestor.app.ui.components.EmptyState
import com.meugestor.app.ui.components.MonthlyComparisonChart
import com.meugestor.app.ui.theme.*
import com.meugestor.app.util.CurrencyUtils
import com.meugestor.app.util.DateUtils

@Composable
fun FutureEntriesScreen(app: MeuGestorApp) {
    val viewModel: FutureEntriesViewModel = viewModel(
        factory = FutureEntriesViewModel.Factory(app.transactionRepository, app.accountRepository)
    )
    val state by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Risk warning
        if (state.negativeBalanceRisk) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = ExpenseRed.copy(alpha = 0.1f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, null, tint = ExpenseRed)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Risco de Saldo Negativo", fontWeight = FontWeight.Bold, color = ExpenseRed,
                                style = MaterialTheme.typography.titleSmall)
                            Text("Seus compromissos futuros superam o saldo atual",
                                style = MaterialTheme.typography.bodySmall, color = ExpenseRed)
                        }
                    }
                }
            }
        }

        // Summary cards
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ProjectionCard(
                    modifier = Modifier.weight(1f),
                    title = "A Receber",
                    value = state.totalReceivable,
                    icon = Icons.Default.TrendingUp,
                    color = IncomeGreen
                )
                ProjectionCard(
                    modifier = Modifier.weight(1f),
                    title = "A Pagar",
                    value = state.totalPayable,
                    icon = Icons.Default.TrendingDown,
                    color = ExpenseRed
                )
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Saldo Previsto", style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            CurrencyUtils.formatBRL(state.projectedBalance),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (state.projectedBalance >= 0) IncomeGreen else ExpenseRed
                        )
                    }
                    Icon(
                        if (state.projectedBalance >= 0) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                        null,
                        tint = if (state.projectedBalance >= 0) IncomeGreen else ExpenseRed,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
        }

        // Monthly projection chart
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Projeção Mensal", style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(12.dp))
                    MonthlyComparisonChart(
                        incomeByMonth = state.monthlyProjections.map { it.label to it.income },
                        expenseByMonth = state.monthlyProjections.map { it.label to it.expense },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(10.dp).padding(1.dp),
                                contentAlignment = Alignment.Center) {
                                Surface(modifier = Modifier.size(8.dp), shape = MaterialTheme.shapes.extraSmall,
                                    color = IncomeGreen) {}
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Receitas", style = MaterialTheme.typography.labelSmall)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(modifier = Modifier.size(8.dp), shape = MaterialTheme.shapes.extraSmall,
                                color = ExpenseRed) {}
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Despesas", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }

        // Upcoming due items
        item {
            Text("Próximos Vencimentos", style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold)
        }
        if (state.upcomingItems.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Outlined.EventAvailable,
                    title = "Nenhum vencimento próximo",
                    description = "Você está em dia com seus compromissos"
                )
            }
        } else {
            items(state.upcomingItems) { item ->
                UpcomingItem(item)
            }
        }

        // Future expenses
        if (state.futureExpenses.isNotEmpty()) {
            item {
                Text("Despesas Pendentes", style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold)
            }
            items(state.futureExpenses) { item ->
                UpcomingItem(item)
            }
        }
    }
}

@Composable
private fun ProjectionCard(
    modifier: Modifier = Modifier,
    title: String,
    value: Double,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: androidx.compose.ui.graphics.Color
) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(title, style = MaterialTheme.typography.labelMedium)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(CurrencyUtils.formatBRL(value), style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
private fun UpcomingItem(transaction: TransactionEntity) {
    val daysLeft = DateUtils.daysUntilDue(transaction.dueDate ?: transaction.date)
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (transaction.type == TransactionType.INCOME) Icons.Default.ArrowUpward
                else Icons.Default.ArrowDownward,
                contentDescription = null,
                tint = if (transaction.type == TransactionType.INCOME) IncomeGreen else ExpenseRed,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(transaction.description, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                Text(
                    text = when {
                        daysLeft < 0 -> "Vencido há ${-daysLeft} dias"
                        daysLeft == 0 -> "Vence hoje"
                        else -> "Vence em $daysLeft dias — ${DateUtils.formatDate(transaction.dueDate ?: transaction.date)}"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = when {
                        daysLeft < 0 -> ExpenseRed
                        daysLeft <= 3 -> WarningOrange
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
            Text(
                CurrencyUtils.formatBRL(transaction.amount),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (transaction.type == TransactionType.INCOME) IncomeGreen else ExpenseRed
            )
        }
    }
}
