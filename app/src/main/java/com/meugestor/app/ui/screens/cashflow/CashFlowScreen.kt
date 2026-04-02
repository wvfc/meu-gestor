package com.meugestor.app.ui.screens.cashflow

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.StickyNote2
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.meugestor.app.MeuGestorApp
import com.meugestor.app.data.database.entity.AccountEntity
import com.meugestor.app.data.database.entity.CategoryEntity
import com.meugestor.app.data.database.entity.TransactionEntity
import com.meugestor.app.data.database.entity.TransactionStatus
import com.meugestor.app.data.database.entity.TransactionType
import com.meugestor.app.ui.components.EmptyState
import com.meugestor.app.ui.components.StatusBadge
import com.meugestor.app.ui.components.TypeFilterChips
import com.meugestor.app.ui.theme.ExpenseRed
import com.meugestor.app.ui.theme.IncomeGreen
import com.meugestor.app.util.CurrencyUtils
import com.meugestor.app.util.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashFlowScreen(app: MeuGestorApp, onNavigate: (String) -> Unit) {
    val viewModel: CashFlowViewModel = viewModel(
        factory = CashFlowViewModel.Factory(app.transactionRepository)
    )
    val state by viewModel.uiState.collectAsState()
    val accounts by app.accountRepository.getAllAccounts().collectAsState(initial = emptyList())
    val incomeCategories by app.categoryRepository.getByType("INCOME").collectAsState(initial = emptyList())
    val expenseCategories by app.categoryRepository.getByType("EXPENSE").collectAsState(initial = emptyList())

    Column(modifier = Modifier.fillMaxSize()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Entradas", style = MaterialTheme.typography.labelSmall)
                    Text(CurrencyUtils.formatBRL(state.totalIncome), color = IncomeGreen, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Saídas", style = MaterialTheme.typography.labelSmall)
                    Text(CurrencyUtils.formatBRL(state.totalExpense), color = ExpenseRed, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Saldo", style = MaterialTheme.typography.labelSmall)
                    Text(CurrencyUtils.formatBRL(state.balance), color = if (state.balance >= 0) IncomeGreen else ExpenseRed, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        TypeFilterChips(
            options = listOf(
                "ALL" to "Todos",
                "INCOME" to "Entradas",
                "EXPENSE" to "Saídas",
                "TRANSFER" to "Transferências"
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
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            singleLine = true
        )

        if (state.filteredTransactions.isEmpty() && !state.isLoading) {
            EmptyState(
                icon = Icons.Outlined.Receipt,
                title = "Nenhum lançamento encontrado",
                description = "Adicione uma receita ou despesa para começar",
                actionLabel = "Novo Lançamento",
                onAction = { viewModel.toggleAddDialog() }
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val grouped = state.filteredTransactions.groupBy { it.date }
                grouped.forEach { (date, transactions) ->
                    item {
                        Text(
                            text = DateUtils.formatDate(date),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    items(transactions) { tx -> CashFlowTransactionItem(tx) }
                    item { HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp)) }
                }
            }
        }
    }

    if (state.showAddDialog) {
        AddTransactionDialog(
            accounts = accounts,
            incomeCategories = incomeCategories,
            expenseCategories = expenseCategories,
            onDismiss = { viewModel.toggleAddDialog() },
            onAdd = { viewModel.addTransaction(it) }
        )
    }
}

@Composable
private fun CashFlowTransactionItem(transaction: TransactionEntity) {
    val isIncome = transaction.type == TransactionType.INCOME
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(40.dp),
            shape = MaterialTheme.shapes.small,
            color = if (isIncome) IncomeGreen.copy(alpha = 0.12f) else ExpenseRed.copy(alpha = 0.12f)
        ) {
            Icon(
                imageVector = if (isIncome) Icons.Filled.ArrowUpward else Icons.Filled.ArrowDownward,
                contentDescription = null,
                modifier = Modifier.padding(8.dp),
                tint = if (isIncome) IncomeGreen else ExpenseRed
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(transaction.description, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(DateUtils.formatDate(transaction.date), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (transaction.notes?.isNotEmpty() == true) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.Outlined.StickyNote2, null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "${if (isIncome) "+" else "-"} ${CurrencyUtils.formatBRL(transaction.amount)}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (isIncome) IncomeGreen else ExpenseRed
            )
            StatusBadge(transaction.status.name)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddTransactionDialog(
    accounts: List<AccountEntity>,
    incomeCategories: List<CategoryEntity>,
    expenseCategories: List<CategoryEntity>,
    onDismiss: () -> Unit,
    onAdd: (TransactionEntity) -> Unit
) {
    var description by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var isIncome by remember { mutableStateOf(true) }

    val categories = if (isIncome) incomeCategories else expenseCategories
    var selectedAccountId by remember(accounts) { mutableStateOf(accounts.firstOrNull()?.id) }
    var selectedCategoryId by remember(categories) { mutableStateOf(categories.firstOrNull()?.id) }

    LaunchedEffect(accounts, categories, isIncome) {
        if (selectedAccountId !in accounts.map { it.id }) selectedAccountId = accounts.firstOrNull()?.id
        if (selectedCategoryId !in categories.map { it.id }) selectedCategoryId = categories.firstOrNull()?.id
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Novo Lançamento") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row {
                    FilterChip(selected = isIncome, onClick = { isIncome = true }, label = { Text("Receita") }, modifier = Modifier.weight(1f))
                    Spacer(modifier = Modifier.width(8.dp))
                    FilterChip(selected = !isIncome, onClick = { isIncome = false }, label = { Text("Despesa") }, modifier = Modifier.weight(1f))
                }
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Descrição") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = amount, onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' || c == ',' } }, label = { Text("Valor (R$)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Text("Conta:", style = MaterialTheme.typography.labelMedium)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    accounts.forEach { account ->
                        FilterChip(selected = selectedAccountId == account.id, onClick = { selectedAccountId = account.id }, label = { Text(account.name) })
                    }
                }
                Text("Categoria:", style = MaterialTheme.typography.labelMedium)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    categories.forEach { category ->
                        FilterChip(selected = selectedCategoryId == category.id, onClick = { selectedCategoryId = category.id }, label = { Text(category.name) })
                    }
                }
                if (accounts.isEmpty() || categories.isEmpty()) {
                    Text("Cadastre ao menos uma conta e uma categoria válida antes de lançar.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val value = amount.replace(",", ".").toDoubleOrNull() ?: return@Button
                    val accountId = selectedAccountId ?: return@Button
                    val categoryId = selectedCategoryId ?: return@Button
                    onAdd(
                        TransactionEntity(
                            type = if (isIncome) TransactionType.INCOME else TransactionType.EXPENSE,
                            description = description,
                            amount = value,
                            categoryId = categoryId,
                            accountId = accountId,
                            date = DateUtils.today(),
                            status = TransactionStatus.PAID,
                            createdAt = DateUtils.today(),
                            updatedAt = DateUtils.today()
                        )
                    )
                },
                enabled = description.isNotBlank() && amount.isNotBlank() && selectedAccountId != null && selectedCategoryId != null && accounts.isNotEmpty() && categories.isNotEmpty()
            ) { Text("Adicionar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
