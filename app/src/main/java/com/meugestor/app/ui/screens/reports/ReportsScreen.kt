package com.meugestor.app.ui.screens.reports

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.meugestor.app.MeuGestorApp
import com.meugestor.app.ui.components.*
import com.meugestor.app.ui.theme.*
import com.meugestor.app.util.CurrencyUtils

@Composable
fun ReportsScreen(app: MeuGestorApp) {
    val viewModel: ReportsViewModel = viewModel(
        factory = ReportsViewModel.Factory(app.transactionRepository)
    )
    val state by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = state.selectedTab) {
            Tab(selected = state.selectedTab == 0, onClick = { viewModel.selectTab(0) },
                text = { Text("Gastos") })
            Tab(selected = state.selectedTab == 1, onClick = { viewModel.selectTab(1) },
                text = { Text("Ranking") })
        }

        when (state.selectedTab) {
            0 -> GastosTab(state, viewModel)
            1 -> RankingTab(state)
        }
    }
}

@Composable
private fun GastosTab(state: ReportsUiState, viewModel: ReportsViewModel) {
    val months = listOf("Jan","Fev","Mar","Abr","Mai","Jun","Jul","Ago","Set","Out","Nov","Dez")

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            PeriodFilterChips(selected = state.selectedPeriod, onSelect = { viewModel.selectPeriod(it) })
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SummaryMetricCard(
                    modifier = Modifier.weight(1f),
                    label = "Receitas",
                    value = state.totalMonthIncome,
                    color = IncomeGreen
                )
                SummaryMetricCard(
                    modifier = Modifier.weight(1f),
                    label = "Despesas",
                    value = state.totalMonthExpense,
                    color = ExpenseRed
                )
            }
        }

        item {
            SummaryMetricCard(
                label = "Média Mensal de Gastos",
                value = state.averageMonthlyExpense,
                color = MaterialTheme.colorScheme.secondary
            )
        }

        // Monthly income vs expense chart
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Receitas x Despesas (Anual)", style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(12.dp))
                    MonthlyComparisonChart(
                        incomeByMonth = state.monthlyTotals.map { months.getOrElse(it.month - 1) { "?" } to it.income },
                        expenseByMonth = state.monthlyTotals.map { months.getOrElse(it.month - 1) { "?" } to it.expense },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Monthly breakdown
        if (state.monthlyTotals.isNotEmpty()) {
            item {
                Text("Detalhamento Mensal", style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold)
            }
            itemsIndexed(state.monthlyTotals) { _, total ->
                val balance = total.income - total.expense
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(months.getOrElse(total.month - 1) { "?" },
                            style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        Column(horizontalAlignment = Alignment.End) {
                            Text("+ ${CurrencyUtils.formatBRL(total.income)}",
                                style = MaterialTheme.typography.bodySmall, color = IncomeGreen)
                            Text("- ${CurrencyUtils.formatBRL(total.expense)}",
                                style = MaterialTheme.typography.bodySmall, color = ExpenseRed)
                            Text(CurrencyUtils.formatBRL(balance),
                                style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold,
                                color = if (balance >= 0) IncomeGreen else ExpenseRed)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RankingTab(state: ReportsUiState) {
    val totalExpense = state.totalMonthExpense.coerceAtLeast(1.0)

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Top Maiores Despesas do Mês", style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold)
        }

        if (state.topExpenses.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Default.BarChart,
                    title = "Sem dados para ranking",
                    description = "Adicione lançamentos para ver o ranking"
                )
            }
        } else {
            itemsIndexed(state.topExpenses) { index, tx ->
                RankingItemRow(
                    position = index + 1,
                    label = tx.description,
                    amount = tx.amount,
                    percentage = (tx.amount / totalExpense * 100).toFloat(),
                    color = rankingColor(index)
                )
            }
        }

        item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }

        item {
            Text("Resumo Anual", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SummaryMetricCard(Modifier.weight(1f), "Receitas Anuais", state.totalYearIncome, IncomeGreen)
                SummaryMetricCard(Modifier.weight(1f), "Despesas Anuais", state.totalYearExpense, ExpenseRed)
            }
        }
    }
}

@Composable
private fun RankingItemRow(
    position: Int,
    label: String,
    amount: Double,
    percentage: Float,
    color: Color
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(32.dp),
                shape = MaterialTheme.shapes.small,
                color = color.copy(alpha = 0.15f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("$position", style = MaterialTheme.typography.labelLarge,
                        color = color, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { (percentage / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                    color = color,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(CurrencyUtils.formatBRL(amount), style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold)
                Text(CurrencyUtils.formatPercent(percentage.toDouble()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun SummaryMetricCard(
    modifier: Modifier = Modifier,
    label: String,
    value: Double,
    color: Color
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            Text(CurrencyUtils.formatBRL(value), style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold, color = color)
        }
    }
}

private fun rankingColor(index: Int): Color = when (index) {
    0 -> GoldAccent
    1 -> Color(0xFFC0C0C0)
    2 -> Color(0xFFCD7F32)
    else -> EmeraldGreen
}
