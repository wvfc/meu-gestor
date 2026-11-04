@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.meugestor.app.ui.screens.reserves

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.meugestor.app.MeuGestorApp
import com.meugestor.app.data.database.entity.AccountEntity
import com.meugestor.app.data.database.entity.AccountType
import com.meugestor.app.ui.components.ConfirmDialog
import com.meugestor.app.ui.components.EmptyState
import com.meugestor.app.ui.theme.*
import com.meugestor.app.util.CurrencyUtils
import com.meugestor.app.util.DateUtils

@Composable
fun ReservesScreen(app: MeuGestorApp) {
    val viewModel: ReservesViewModel = viewModel(
        factory = ReservesViewModel.Factory(app.accountRepository)
    )
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.toggleAddDialog() }) {
                Icon(Icons.Default.Add, "Nova Conta")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("Patrimônio Total", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(CurrencyUtils.formatBRL(state.totalBalance), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }

            item {
                Text("Resumo por Tipo", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                val total = state.totalBalance.coerceAtLeast(1.0)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        Triple("Conta Corrente", state.totalChecking, Color(0xFF42A5F5)),
                        Triple("Poupança", state.totalSavings, IncomeGreen),
                        Triple("Investimentos", state.totalInvestments, GoldAccent),
                        Triple("Dinheiro Físico", state.totalCash, Color(0xFFAB47BC)),
                        Triple("Reserva de Emergência", state.totalEmergency, EmeraldGreenDark)
                    ).forEach { (label, value, color) ->
                        if (value != 0.0) ReserveSummaryRow(label, value, (value / total * 100).toFloat(), color)
                    }
                }
            }

            item {
                Text("Contas Cadastradas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }

            if (state.accounts.isEmpty() && !state.isLoading) {
                item {
                    EmptyState(
                        icon = Icons.Outlined.AccountBalance,
                        title = "Nenhuma conta cadastrada",
                        description = "Adicione suas contas e acompanhe seu patrimônio",
                        actionLabel = "Adicionar Conta",
                        onAction = { viewModel.toggleAddDialog() }
                    )
                }
            } else {
                item {
                    Text(
                        "Toque em uma conta para ajustar o saldo ou excluí-la.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                items(state.accounts, key = { it.id }) { account ->
                    AccountCard(account = account, onAdjustClick = { viewModel.openAdjustDialog(account) })
                }
            }
        }
    }

    if (state.showAddDialog) {
        AddAccountDialog(
            onDismiss = { viewModel.toggleAddDialog() },
            onAdd = { viewModel.addAccount(it) }
        )
    }

    val accountToAdjust = state.accountToAdjust
    if (state.showAdjustDialog && accountToAdjust != null) {
        AdjustBalanceDialog(
            account = accountToAdjust,
            onDismiss = { viewModel.closeAdjustDialog() },
            onSave = { viewModel.saveAdjustedBalance(it) },
            onDelete = { viewModel.deleteAccount(accountToAdjust) }
        )
    }
}

@Composable
private fun ReserveSummaryRow(label: String, value: Double, percentage: Float, color: Color) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.width(140.dp)) { Text(label, style = MaterialTheme.typography.bodySmall) }
        Column(modifier = Modifier.weight(1f)) {
            LinearProgressIndicator(progress = { (percentage / 100f).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth(), color = color, trackColor = MaterialTheme.colorScheme.surfaceVariant)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(CurrencyUtils.formatBRL(value), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(90.dp))
    }
}

@Composable
private fun AccountCard(account: AccountEntity, onAdjustClick: () -> Unit) {
    val typeLabel = when (account.accountType) {
        AccountType.CHECKING -> "Conta Corrente"
        AccountType.SAVINGS -> "Poupança"
        AccountType.INVESTMENT -> "Investimento"
        AccountType.CASH -> "Dinheiro"
        AccountType.EMERGENCY -> "Reserva de Emergência"
    }
    val typeColor = when (account.accountType) {
        AccountType.CHECKING -> Color(0xFF42A5F5)
        AccountType.SAVINGS -> IncomeGreen
        AccountType.INVESTMENT -> GoldAccent
        AccountType.CASH -> Color(0xFFAB47BC)
        AccountType.EMERGENCY -> EmeraldGreenDark
    }

    Card(modifier = Modifier.fillMaxWidth().clickable { onAdjustClick() }) {
        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Surface(modifier = Modifier.size(48.dp), shape = MaterialTheme.shapes.medium, color = typeColor.copy(alpha = 0.15f)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.AccountBalance, null, tint = typeColor, modifier = Modifier.size(24.dp))
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(account.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(account.bankName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Surface(color = typeColor.copy(alpha = 0.1f), shape = MaterialTheme.shapes.extraSmall) {
                    Text(typeLabel, style = MaterialTheme.typography.labelSmall, color = typeColor, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                }
            }
            Text(CurrencyUtils.formatBRL(account.balance), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = if (account.balance >= 0) IncomeGreen else ExpenseRed)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddAccountDialog(onDismiss: () -> Unit, onAdd: (AccountEntity) -> Unit) {
    var name by remember { mutableStateOf("") }
    var bank by remember { mutableStateOf("") }
    var balance by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(AccountType.CHECKING) }

    val types = listOf(
        AccountType.CHECKING to "Corrente",
        AccountType.SAVINGS to "Poupança",
        AccountType.INVESTMENT to "Investimento",
        AccountType.CASH to "Dinheiro",
        AccountType.EMERGENCY to "Emergência"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nova Conta") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nome da conta") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = bank, onValueChange = { bank = it }, label = { Text("Banco / Instituição") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    value = balance,
                    onValueChange = { balance = CurrencyUtils.filterAmountInput(it, allowNegative = true) },
                    label = { Text("Saldo Inicial (R$)") },
                    placeholder = { Text("0,00") },
                    singleLine = true,
                    isError = balance.isNotBlank() && CurrencyUtils.parseAmount(balance) == null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Tipo:", style = MaterialTheme.typography.labelMedium)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    types.chunked(3).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { (t, label) ->
                                FilterChip(selected = type == t, onClick = { type = t }, label = { Text(label) })
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val bal = if (balance.isBlank()) 0.0 else CurrencyUtils.parseAmount(balance) ?: return@Button
                    onAdd(AccountEntity(name = name.trim(), bankName = bank.trim(), accountType = type, balance = bal, initialBalance = bal, color = "#2E7D52", icon = "account_balance", createdAt = DateUtils.today()))
                },
                enabled = name.isNotBlank() && bank.isNotBlank() &&
                    (balance.isBlank() || CurrencyUtils.parseAmount(balance) != null)
            ) { Text("Adicionar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
private fun AdjustBalanceDialog(
    account: AccountEntity,
    onDismiss: () -> Unit,
    onSave: (Double) -> Unit,
    onDelete: () -> Unit
) {
    var balance by remember(account) { mutableStateOf(CurrencyUtils.toInputString(account.balance)) }
    var confirmDelete by remember(account) { mutableStateOf(false) }

    if (confirmDelete) {
        ConfirmDialog(
            title = "Excluir conta?",
            message = "\"${account.name}\" e todos os lançamentos vinculados a ela serão excluídos. Esta ação não pode ser desfeita.",
            confirmLabel = "Excluir",
            onConfirm = onDelete,
            onDismiss = { confirmDelete = false }
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ajustar saldo") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(account.name, style = MaterialTheme.typography.titleSmall)
                OutlinedTextField(
                    value = balance,
                    onValueChange = { balance = CurrencyUtils.filterAmountInput(it, allowNegative = true) },
                    label = { Text("Novo saldo (R$)") },
                    singleLine = true,
                    isError = CurrencyUtils.parseAmount(balance) == null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "Use para corrigir o saldo conforme o extrato do banco.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(onClick = { confirmDelete = true }) {
                    Text("Excluir conta", color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val value = CurrencyUtils.parseAmount(balance) ?: return@Button
                onSave(value)
            }, enabled = CurrencyUtils.parseAmount(balance) != null) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
