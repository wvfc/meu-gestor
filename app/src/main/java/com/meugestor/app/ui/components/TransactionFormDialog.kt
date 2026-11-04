@file:OptIn(ExperimentalMaterial3Api::class)

package com.meugestor.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.meugestor.app.data.database.entity.AccountEntity
import com.meugestor.app.data.database.entity.CategoryEntity
import com.meugestor.app.data.database.entity.TransactionEntity
import com.meugestor.app.data.database.entity.TransactionStatus
import com.meugestor.app.data.database.entity.TransactionType
import com.meugestor.app.domain.TransactionDraft
import com.meugestor.app.util.CurrencyUtils
import com.meugestor.app.util.DateUtils

/**
 * Formulário único de receita/despesa, usado tanto para criar quanto para editar.
 * Permite escolher data, situação (pago/pendente), conta, categoria e repetição mensal.
 */
@Composable
fun TransactionFormDialog(
    accounts: List<AccountEntity>,
    incomeCategories: List<CategoryEntity>,
    expenseCategories: List<CategoryEntity>,
    onDismiss: () -> Unit,
    onSave: (TransactionDraft) -> Unit,
    initialType: TransactionType = TransactionType.EXPENSE,
    editing: TransactionEntity? = null
) {
    val isEditing = editing != null
    // Compras no cartão sempre são despesas.
    val typeLocked = editing?.creditCardId != null

    var type by remember { mutableStateOf(editing?.type ?: initialType) }
    var description by remember { mutableStateOf(editing?.description ?: "") }
    var amountText by remember {
        mutableStateOf(editing?.let { CurrencyUtils.toInputString(it.amount) } ?: "")
    }
    var date by remember { mutableStateOf(editing?.date ?: DateUtils.today()) }
    var isPaid by remember { mutableStateOf(editing?.let { it.status == TransactionStatus.PAID } ?: true) }
    var notes by remember { mutableStateOf(editing?.notes ?: "") }
    var repeatText by remember { mutableStateOf("1") }
    var selectedAccountId by remember { mutableStateOf(editing?.accountId ?: accounts.firstOrNull()?.id) }
    var selectedCategoryId by remember { mutableStateOf(editing?.categoryId) }
    var showDatePicker by remember { mutableStateOf(false) }

    val isIncome = type == TransactionType.INCOME
    val categories = if (isIncome) incomeCategories else expenseCategories

    // Mantém as seleções válidas quando as listas carregam ou o tipo muda.
    LaunchedEffect(accounts) {
        if (accounts.isNotEmpty() && accounts.none { it.id == selectedAccountId }) {
            selectedAccountId = accounts.first().id
        }
    }
    LaunchedEffect(categories) {
        if (categories.isNotEmpty() && categories.none { it.id == selectedCategoryId }) {
            selectedCategoryId = categories.first().id
        }
    }

    val amount = CurrencyUtils.parseAmount(amountText)
    val amountValid = amount != null && amount > 0
    val repeatMonths = repeatText.toIntOrNull()?.coerceIn(1, TransactionDraft.MAX_REPEAT_MONTHS) ?: 1
    val accountId = selectedAccountId?.takeIf { id -> accounts.any { it.id == id } }
    val canSave = description.isNotBlank() && amountValid && accountId != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEditing) "Editar lançamento" else "Novo lançamento") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (!typeLocked) {
                    Row {
                        FilterChip(
                            selected = isIncome,
                            onClick = { type = TransactionType.INCOME },
                            label = { Text("Receita") },
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        FilterChip(
                            selected = !isIncome,
                            onClick = { type = TransactionType.EXPENSE },
                            label = { Text("Despesa") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descrição") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = CurrencyUtils.filterAmountInput(it) },
                    label = { Text("Valor (R$)") },
                    placeholder = { Text("0,00") },
                    singleLine = true,
                    isError = amountText.isNotBlank() && !amountValid,
                    supportingText = if (amountText.isNotBlank() && !amountValid) {
                        { Text("Informe um valor maior que zero") }
                    } else {
                        null
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                DateFieldButton(
                    label = if (isPaid) "Data" else "Vencimento",
                    isoDate = date,
                    onClick = { showDatePicker = true }
                )

                Text("Situação:", style = MaterialTheme.typography.labelMedium)
                Row {
                    FilterChip(
                        selected = isPaid,
                        onClick = { isPaid = true },
                        label = { Text(if (isIncome) "Recebido" else "Pago") },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FilterChip(
                        selected = !isPaid,
                        onClick = { isPaid = false },
                        label = { Text(if (isIncome) "A receber" else "A pagar") },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (!isPaid) {
                    Text(
                        "Pendências não alteram o saldo até serem marcadas como pagas/recebidas.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (!isEditing) {
                    OutlinedTextField(
                        value = repeatText,
                        onValueChange = { repeatText = it.filter(Char::isDigit).take(2) },
                        label = { Text("Repetir por quantos meses") },
                        supportingText = {
                            Text(
                                if (repeatMonths > 1) {
                                    "Cria $repeatMonths lançamentos mensais; os próximos ficam pendentes."
                                } else {
                                    "1 = lançamento único"
                                }
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Text("Conta:", style = MaterialTheme.typography.labelMedium)
                if (accounts.isEmpty()) {
                    Text(
                        "Nenhuma conta cadastrada. Cadastre uma conta em Reservas (menu lateral).",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                } else {
                    ChipSelector(
                        options = accounts,
                        isSelected = { it.id == selectedAccountId },
                        label = { it.name },
                        onSelect = { selectedAccountId = it.id }
                    )
                }

                if (categories.isNotEmpty()) {
                    Text("Categoria:", style = MaterialTheme.typography.labelMedium)
                    ChipSelector(
                        options = categories,
                        isSelected = { it.id == selectedCategoryId },
                        label = { it.name },
                        onSelect = { selectedCategoryId = it.id }
                    )
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Observações (opcional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                enabled = canSave,
                onClick = {
                    val value = amount ?: return@Button
                    val account = accountId ?: return@Button
                    onSave(
                        TransactionDraft(
                            type = type,
                            description = description,
                            amount = value,
                            date = date,
                            isPaid = isPaid,
                            accountId = account,
                            categoryId = selectedCategoryId?.takeIf { id -> categories.any { it.id == id } },
                            notes = notes,
                            repeatMonths = if (isEditing) 1 else repeatMonths
                        )
                    )
                }
            ) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )

    if (showDatePicker) {
        AppDatePickerDialog(
            initialDate = date,
            onDismiss = { showDatePicker = false },
            onDateSelected = { date = it }
        )
    }
}
