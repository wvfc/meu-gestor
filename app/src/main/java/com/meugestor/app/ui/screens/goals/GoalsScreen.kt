package com.meugestor.app.ui.screens.goals

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.meugestor.app.MeuGestorApp
import com.meugestor.app.data.database.entity.GoalEntity
import com.meugestor.app.data.database.entity.GoalPeriod
import com.meugestor.app.data.repository.GoalProgress
import com.meugestor.app.ui.components.EmptyState
import com.meugestor.app.ui.theme.*
import com.meugestor.app.util.CurrencyUtils
import com.meugestor.app.util.DateUtils

@Composable
fun GoalsScreen(app: MeuGestorApp, onNavigate: (String) -> Unit) {
    val viewModel: GoalsViewModel = viewModel(
        factory = GoalsViewModel.Factory(app.goalRepository)
    )
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.toggleAddDialog() }) {
                Icon(Icons.Default.Add, "Nova Meta")
            }
        }
    ) { padding ->
        if (state.goals.isEmpty() && !state.isLoading) {
            EmptyState(
                icon = Icons.Outlined.Flag,
                title = "Nenhuma meta cadastrada",
                description = "Defina metas de gastos para manter suas finanças sob controle",
                actionLabel = "Nova Meta",
                onAction = { viewModel.toggleAddDialog() },
                modifier = Modifier.padding(padding).fillMaxSize()
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    // Summary
                    val alertCount = state.goals.count { it.percentUsed >= 90 }
                    if (alertCount > 0) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = WarningOrange.copy(alpha = 0.1f))
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Warning, null, tint = WarningOrange)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("$alertCount meta(s) próxima(s) do limite",
                                    color = WarningOrange, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
                items(state.goals) { progress ->
                    GoalCard(progress)
                }
            }
        }
    }

    if (state.showAddDialog) {
        AddGoalDialog(
            onDismiss = { viewModel.toggleAddDialog() },
            onAdd = { viewModel.addGoal(it) }
        )
    }
}

@Composable
private fun GoalCard(progress: GoalProgress) {
    val percent = progress.percentUsed.toFloat().coerceIn(0f, 100f)
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
                    Text(progress.goal.name, style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold)
                    Text(
                        when (progress.goal.period) {
                            GoalPeriod.WEEKLY -> "Meta Semanal"
                            GoalPeriod.MONTHLY -> "Meta Mensal"
                            GoalPeriod.YEARLY -> "Meta Anual"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (percent >= 100f) {
                    Icon(Icons.Default.Warning, null, tint = ExpenseRed, modifier = Modifier.size(24.dp))
                } else if (percent >= 70f) {
                    Icon(Icons.Default.NotificationsActive, null, tint = WarningOrange, modifier = Modifier.size(24.dp))
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
                    if (remaining > 0) "Restam ${CurrencyUtils.formatBRL(remaining)}" else "Limite ultrapassado!",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (remaining > 0) MaterialTheme.colorScheme.onSurfaceVariant else ExpenseRed
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddGoalDialog(onDismiss: () -> Unit, onAdd: (GoalEntity) -> Unit) {
    var name by remember { mutableStateOf("") }
    var target by remember { mutableStateOf("") }
    var period by remember { mutableStateOf(GoalPeriod.MONTHLY) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nova Meta de Gasto") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it },
                    label = { Text("Nome da meta") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    value = target, onValueChange = { target = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Valor limite (R$)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Text("Período:", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(GoalPeriod.WEEKLY to "Semanal", GoalPeriod.MONTHLY to "Mensal", GoalPeriod.YEARLY to "Anual")
                        .forEach { (p, label) ->
                            FilterChip(selected = period == p, onClick = { period = p },
                                label = { Text(label) })
                        }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val value = target.toDoubleOrNull() ?: return@Button
                    val start = DateUtils.getStartOfMonth()
                    val end = DateUtils.getEndOfMonth()
                    onAdd(GoalEntity(
                        name = name, categoryId = 1, targetAmount = value,
                        period = period, startDate = start, endDate = end,
                        createdAt = DateUtils.today()
                    ))
                },
                enabled = name.isNotBlank() && target.isNotBlank()
            ) { Text("Criar Meta") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
