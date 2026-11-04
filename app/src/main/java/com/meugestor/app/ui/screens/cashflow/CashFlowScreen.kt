@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.meugestor.app.ui.screens.cashflow

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Receipt
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
import com.meugestor.app.ui.components.TypeFilterChips
import com.meugestor.app.ui.theme.ExpenseRed
import com.meugestor.app.ui.theme.IncomeGreen
import com.meugestor.app.util.CurrencyUtils
import com.meugestor.app.util.DateUtils
import java.time.YearMonth

@Composable
fun CashFlowScreen(app: MeuGestorApp) {
    val viewModel: CashFlowViewModel = viewModel(
        factory = CashFlowViewModel.Factory(app.transactionRepository)
    )
    val state by viewModel.uiState.collectAsState()
    val dialogs = rememberTransactionDialogState()
    val lookups = rememberTransactionLookups(app)

    CollectMessages(viewModel.messages)

    // Antes o único botão de inclusão ficava no estado vazio: depois do primeiro
    // lançamento não havia mais como adicionar outro.
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            FloatingActionButton(onClick = { dialogs.create(TransactionType.EXPENSE) }) {
                Icon(Icons.Default.Add, contentDescription = "Novo lançamento")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            MonthSelector(
                month = state.month,
                onPrevious = { viewModel.previousMonth() },
                onNext = { viewModel.nextMonth() },
                onCurrent = { viewModel.goToCurrentMonth() }
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    TotalColumn("Entradas", state.totalIncome, IncomeGreen)
                    TotalColumn("Saídas", state.totalExpense, ExpenseRed)
                    TotalColumn("Saldo", state.balance, if (state.balance >= 0) IncomeGreen else ExpenseRed)
                }
            }

            TypeFilterChips(
                options = listOf(
                    CashFlowFilter.ALL.name to "Todos",
                    CashFlowFilter.INCOME.name to "Entradas",
                    CashFlowFilter.EXPENSE.name to "Saídas",
                    CashFlowFilter.PENDING.name to if (state.pendingCount > 0) "Pendentes (${state.pendingCount})" else "Pendentes"
                ),
                selected = state.currentFilter.name,
                onSelect = { viewModel.setFilter(CashFlowFilter.valueOf(it)) },
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("Buscar por descrição...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = {
                    if (state.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) { Icon(Icons.Default.Clear, null) }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                singleLine = true
            )

            if (state.filteredTransactions.isEmpty() && !state.isLoading) {
                EmptyState(
                    icon = Icons.Outlined.Receipt,
                    title = "Nenhum lançamento encontrado",
                    description = if (state.transactions.isEmpty()) {
                        "Nenhum lançamento em ${DateUtils.formatYearMonth(state.month)}"
                    } else {
                        "Ajuste o filtro ou a busca"
                    },
                    actionLabel = "Novo Lançamento",
                    onAction = { dialogs.create(TransactionType.EXPENSE) }
                )
            } else {
                val grouped = remember(state.filteredTransactions) {
                    state.filteredTransactions.groupBy { it.date }
                }
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    // Espaço extra no fim para o último item não ficar atrás do botão "+".
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    grouped.forEach { (date, transactions) ->
                        item(key = "header_$date") {
                            Text(
                                text = DateUtils.formatDate(date),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        items(transactions, key = { it.id }) { tx ->
                            TransactionRow(
                                transaction = tx,
                                categoryName = lookups.categoryName(tx),
                                onClick = { dialogs.openDetails(tx) }
                            )
                        }
                        item(key = "divider_$date") {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        }
                    }
                }
            }
        }
    }

    TransactionDialogsHost(app = app, viewModel = viewModel, state = dialogs, lookups = lookups)
}

@Composable
private fun MonthSelector(
    month: YearMonth,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onCurrent: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.Default.ChevronLeft, contentDescription = "Mês anterior")
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                DateUtils.formatYearMonth(month),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            if (month != YearMonth.now()) {
                TextButton(onClick = onCurrent, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) {
                    Text("Voltar ao mês atual", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        IconButton(onClick = onNext) {
            Icon(Icons.Default.ChevronRight, contentDescription = "Próximo mês")
        }
    }
}

@Composable
private fun TotalColumn(label: String, value: Double, color: androidx.compose.ui.graphics.Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall)
        Text(
            CurrencyUtils.formatBRL(value),
            color = color,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
