@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.meugestor.app.ui.screens.goals

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.meugestor.app.MeuGestorApp
import com.meugestor.app.data.database.entity.CategoryEntity
import com.meugestor.app.data.database.entity.GoalEntity
import com.meugestor.app.data.database.entity.GoalPeriod
import com.meugestor.app.data.repository.GoalProgress
import com.meugestor.app.ui.common.CollectMessages
import com.meugestor.app.ui.components.ChipSelector
import com.meugestor.app.ui.components.ConfirmDialog
import com.meugestor.app.ui.components.EmptyState
import com.meugestor.app.ui.theme.*
import com.meugestor.app.util.CurrencyUtils
import com.meugestor.app.util.DateUtils

@Composable
fun GoalsScreen(app: MeuGestorApp) {
    val viewModel: GoalsViewModel = viewModel(
        factory = GoalsViewModel.Factory(app.goalRepository, app.transactionRepository, app.categoryRepository)
    )
    val state by viewModel.uiState.collectAsState()
    val expenseFlow = remember(app) { app.categoryRepository.getByType("EXPENSE") }
    val expenseCategories by expenseFlow.collectAsState(initial = emptyList())
    var showAddDialog by remember { mutableStateOf(false) }
    var goalToDelete by remember { mutableStateOf<GoalEntity?>(null) }

    CollectMessages(viewModel.messages)

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, "Nova Meta")
            }
        }
    ) { padding ->
        if (state.goals.isEmpty() && !state.isLoading) {
            EmptyState(
                icon = Icons.Outlined.Flag,
                title = "Nenhuma meta cadastrada",
                description = "Defina limites de gasto por categoria para manter suas finanças sob controle",
                actionLabel = "Nova Meta",
                onAction = { showAddDialog = true },
                modifier = Modifier.padding(padding).fillMaxSize()
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val alertCount = state.goals.count { it.percentUsed >= 90 }
                if (alertCount > 0) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = WarningOrange.copy(alpha = 0.1f))
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Warning, null, tint = WarningOrange)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "$alertCount meta(s) próxima(s) do limite",
                                    color = WarningOrange, style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
                items(state.goals, key = { it.goal.id }) { progress ->
                    GoalCard(progress, onDelete = { goalToDelete = progress.goal })
                }
            }
        }
    }

    if (showAddDialog) {
        AddGoalDialog(
            expenseCategories = expenseCategories,
            onDismiss = { showAddDialog = false },
            onAdd = { name, categoryId, target, period ->
                viewModel.addGoal(name, categoryId, target, period)
                showAddDialog = false
            }
        )
    }

    goalToDelete?.let { goal ->
        ConfirmDialog(
            title = "Excluir meta?",
            message = "A meta \"${goal.name}\" será excluída. Os lançamentos não são afetados.",
            confirmLabel = "Excluir",
            onConfirm = {
                viewModel.deleteGoal(goal)
                goalToDelete = null
            },
            onDismiss = { goalToDelete = null }
        )
    }
}

@Composable
private fun GoalCard(progress: GoalProgress, onDelete: () -> Unit) {
    // O percentual real é exibido mesmo acima de 100%; só a barra é limitada.
    val percent = progress.percentUsed.toFloat().coerceAtLeast(0f)
    val progressFraction = (percent / 100f).coerceIn(0f, 1f)
    val progressColor = when {
        percent >= 100f -> ExpenseRed
        percent >= 90f -> WarningOrange
        percent >= 70f -> GoldAccent
        else -> IncomeGreen
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        progress.goal.name, style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    val periodLabel = when (progress.goal.period) {
                        GoalPeriod.WEEKLY -> "Meta Semanal"
                        GoalPeriod.MONTHLY -> "Meta Mensal"
                        GoalPeriod.YEARLY -> "Meta Anual"
                    }
                    Text(
                        listOfNotNull(periodLabel, progress.categoryName).joinToString(" • "),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "${DateUtils.formatDate(progress.periodStart)} a ${DateUtils.formatDate(progress.periodEnd)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (percent >= 100f) {
                    Icon(Icons.Default.Warning, null, tint = ExpenseRed, modifier = Modifier.size(24.dp))
                } else if (percent >= 70f) {
                    Icon(Icons.Default.NotificationsActive, null, tint = WarningOrange, modifier = Modifier.size(24.dp))
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Outlined.Delete, contentDescription = "Excluir meta")
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Gasto: ${CurrencyUtils.formatBRL(progress.spentAmount)}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "Limite: ${CurrencyUtils.formatBRL(progress.goal.targetAmount)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progressFraction },
                modifier = Modifier.fillMaxWidth(),
                color = progressColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "${percent.toInt()}% utilizado",
                    style = MaterialTheme.typography.labelSmall,
                    color = progressColor,
                    fontWeight = FontWeight.SemiBold
                )
                val remaining = progress.goal.targetAmount - progress.spentAmount
                Text(
                    if (remaining >= 0) "Restam ${CurrencyUtils.formatBRL(remaining)}"
                    else "Ultrapassado em ${CurrencyUtils.formatBRL(-remaining)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (remaining >= 0) MaterialTheme.colorScheme.onSurfaceVariant else ExpenseRed
                )
            }
            // Milestone indicators
            if (percent >= 70f) {
                Spacer(modifier = Modifier.height(8.dp))
                val msg = when {
                    percent >= 100f -> "Limite atingido! Considere revisar esta categoria."
                    percent >= 90f -> "Atenção: 90% do limite utilizado."
                    else -> "70% do limite utilizado."
                }
                Text(msg, style = MaterialTheme.typography.bodySmall, color = progressColor)
            }
        }
    }
}

@Composable
private fun AddGoalDialog(
    expenseCategories: List<CategoryEntity>,
    onDismiss: () -> Unit,
    onAdd: (name: String, categoryId: Long, target: Double, period: GoalPeriod) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var target by remember { mutableStateOf("") }
    var period by remember { mutableStateOf(GoalPeriod.MONTHLY) }
    var selectedCategoryId by remember { mutableStateOf(expenseCategories.firstOrNull()?.id) }

    LaunchedEffect(expenseCategories) {
        if (expenseCategories.isNotEmpty() && expenseCategories.none { it.id == selectedCategoryId }) {
            selectedCategoryId = expenseCategories.first().id
        }
    }

    val targetValue = CurrencyUtils.parseAmount(target)
    val selectedCategory = expenseCategories.find { it.id == selectedCategoryId }
    val canSave = targetValue != null && targetValue > 0 && selectedCategory != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nova Meta de Gasto") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Categoria acompanhada:", style = MaterialTheme.typography.labelMedium)
                if (expenseCategories.isEmpty()) {
                    Text(
                        "Nenhuma categoria de despesa disponível.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                } else {
                    ChipSelector(
                        options = expenseCategories,
                        isSelected = { it.id == selectedCategoryId },
                        label = { it.name },
                        onSelect = { selectedCategoryId = it.id }
                    )
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome da meta (opcional)") },
                    placeholder = { Text(selectedCategory?.name ?: "") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = target,
                    onValueChange = { target = CurrencyUtils.filterAmountInput(it) },
                    label = { Text("Valor limite (R$)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Período:", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(GoalPeriod.WEEKLY to "Semanal", GoalPeriod.MONTHLY to "Mensal", GoalPeriod.YEARLY to "Anual")
                        .forEach { (p, label) ->
                            FilterChip(
                                selected = period == p, onClick = { period = p },
                                label = { Text(label) }
                            )
                        }
                }
                Text(
                    "O gasto é calculado automaticamente no período corrente (semana, mês ou ano).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val value = targetValue ?: return@Button
                    val category = selectedCategory ?: return@Button
                    onAdd(name.ifBlank { category.name }, category.id, value, period)
                },
                enabled = canSave
            ) { Text("Criar Meta") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
