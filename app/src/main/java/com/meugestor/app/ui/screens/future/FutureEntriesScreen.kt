@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

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
import com.meugestor.app.data.database.entity.TransactionType
import com.meugestor.app.ui.common.CollectMessages
import com.meugestor.app.ui.common.TransactionDialogsHost
import com.meugestor.app.ui.common.rememberTransactionDialogState
import com.meugestor.app.ui.common.rememberTransactionLookups
import com.meugestor.app.ui.components.EmptyState
import com.meugestor.app.ui.components.MonthlyComparisonChart
import com.meugestor.app.ui.components.TransactionRow
import com.meugestor.app.ui.theme.*
import com.meugestor.app.util.CurrencyUtils

@Composable
fun FutureEntriesScreen(app: MeuGestorApp) {
    val viewModel: FutureEntriesViewModel = viewModel(
        factory = FutureEntriesViewModel.Factory(app.transactionRepository, app.accountRepository)
    )
    val state by viewModel.uiState.collectAsState()
    val dialogs = rememberTransactionDialogState()
    val lookups = rememberTransactionLookups(app)

    CollectMessages(viewModel.messages)

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { dialogs.create(TransactionType.EXPENSE) },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Agendar") }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
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
                                Text(
                                    "Risco de Saldo Negativo", fontWeight = FontWeight.Bold, color = ExpenseRed,
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    state.firstNegativeMonth?.let { "O saldo estimado fica negativo em $it." }
                                        ?: "Seus compromissos futuros superam o saldo atual.",
                                    style = MaterialTheme.typography.bodySmall, color = ExpenseRed
                                )
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
                            Text(
                                "Saldo Previsto", style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                CurrencyUtils.formatBRL(state.projectedBalance),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (state.projectedBalance >= 0) IncomeGreen else ExpenseRed
                            )
                            Text(
                                "Saldo atual ${CurrencyUtils.formatBRL(state.currentBalance)} + a receber − a pagar",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
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
                        Text(
                            "Projeção dos Próximos Meses", style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        MonthlyComparisonChart(
                            incomeByMonth = state.monthlyProjections.map { it.label to it.income },
                            expenseByMonth = state.monthlyProjections.map { it.label to it.expense },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            LegendDot("Receitas", IncomeGreen)
                            LegendDot("Despesas", ExpenseRed)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        state.monthlyProjections.forEach { projection ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(projection.label, style = MaterialTheme.typography.bodySmall)
                                Text(
                                    "Saldo ao fim: ${CurrencyUtils.formatBRL(projection.endBalance)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = if (projection.endBalance >= 0) MaterialTheme.colorScheme.onSurface else ExpenseRed
                                )
                            }
                        }
                    }
                }
            }

            // Upcoming due items
            item {
                Text(
                    "Próximos Vencimentos (7 dias)", style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
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
                items(state.upcomingItems, key = { "upcoming_${it.id}" }) { item ->
                    TransactionRow(
                        transaction = item,
                        categoryName = lookups.categoryName(item),
                        showDueInfo = true,
                        onClick = { dialogs.openDetails(item) }
                    )
                }
            }

            if (state.futureIncomes.isNotEmpty()) {
                item {
                    SectionTitle("A Receber", state.totalReceivable, IncomeGreen)
                }
                items(state.futureIncomes, key = { "income_${it.id}" }) { item ->
                    TransactionRow(
                        transaction = item,
                        categoryName = lookups.categoryName(item),
                        showDueInfo = true,
                        onClick = { dialogs.openDetails(item) }
                    )
                }
            }

            if (state.futureExpenses.isNotEmpty()) {
                item {
                    SectionTitle("A Pagar", state.totalPayable, ExpenseRed)
                }
                items(state.futureExpenses, key = { "expense_${it.id}" }) { item ->
                    TransactionRow(
                        transaction = item,
                        categoryName = lookups.categoryName(item),
                        showDueInfo = true,
                        onClick = { dialogs.openDetails(item) }
                    )
                }
            }
        }
    }

    TransactionDialogsHost(app = app, viewModel = viewModel, state = dialogs, lookups = lookups)
}

@Composable
private fun SectionTitle(title: String, total: Double, color: androidx.compose.ui.graphics.Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(
            CurrencyUtils.formatBRL(total),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
private fun LegendDot(label: String, color: androidx.compose.ui.graphics.Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(modifier = Modifier.size(8.dp), shape = MaterialTheme.shapes.extraSmall, color = color) {}
        Spacer(modifier = Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall)
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
            Text(
                CurrencyUtils.formatBRL(value), style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold, color = color
            )
        }
    }
}
