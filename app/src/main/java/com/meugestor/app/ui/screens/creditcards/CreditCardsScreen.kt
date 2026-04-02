@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
package com.meugestor.app.ui.screens.creditcards

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.meugestor.app.MeuGestorApp
import com.meugestor.app.data.database.entity.*
import com.meugestor.app.ui.components.EmptyState
import com.meugestor.app.ui.components.StatusBadge
import com.meugestor.app.ui.theme.*
import com.meugestor.app.util.CurrencyUtils
import com.meugestor.app.util.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreditCardsScreen(app: MeuGestorApp, onNavigate: (String) -> Unit) {
    val viewModel: CreditCardsViewModel = viewModel(
        factory = CreditCardsViewModel.Factory(app.creditCardRepository, app.transactionRepository)
    )
    val state by viewModel.uiState.collectAsState()
    val accounts by app.accountRepository.getAllAccounts().collectAsState(initial = emptyList())
    val expenseCategories by app.categoryRepository.getByType("EXPENSE").collectAsState(initial = emptyList())
    val pagerState = rememberPagerState { state.cards.size.coerceAtLeast(1) }

    LaunchedEffect(pagerState.currentPage) {
        viewModel.selectCard(pagerState.currentPage)
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.toggleAddDialog() }) {
                Icon(Icons.Default.Add, "Novo Cartão")
            }
        }
    ) { padding ->
        if (state.cards.isEmpty() && !state.isLoading) {
            EmptyState(
                icon = Icons.Default.CreditCard,
                title = "Nenhum cartão cadastrado",
                description = "Adicione seu primeiro cartão de crédito",
                actionLabel = "Adicionar Cartão",
                onAction = { viewModel.toggleAddDialog() },
                modifier = Modifier.padding(padding)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    HorizontalPager(
                        state = pagerState,
                        contentPadding = PaddingValues(horizontal = 32.dp),
                        pageSpacing = 16.dp
                    ) { page ->
                        if (page < state.cards.size) {
                            CreditCardVisual(cardDetails = state.cards[page], modifier = Modifier.fillMaxWidth())
                        }
                    }
                }

                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        repeat(state.cards.size) { index ->
                            Box(
                                modifier = Modifier
                                    .padding(4.dp)
                                    .size(if (pagerState.currentPage == index) 10.dp else 6.dp)
                                    .clip(MaterialTheme.shapes.small)
                                    .background(
                                        if (pagerState.currentPage == index) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                    )
                            )
                        }
                    }
                }

                if (state.cards.isNotEmpty() && pagerState.currentPage < state.cards.size) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Lançamentos do Cartão", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            TextButton(onClick = { viewModel.toggleExpenseDialog() }) {
                                Text("Adicionar compra")
                            }
                        }
                    }
                    if (state.selectedCardTransactions.isEmpty()) {
                        item {
                            Text("Nenhum lançamento neste cartão", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 8.dp))
                        }
                    } else {
                        items(state.selectedCardTransactions) { tx -> CardTransactionItem(tx) }
                    }
                }
            }
        }
    }

    if (state.showAddDialog) {
        AddCardDialog(onDismiss = { viewModel.toggleAddDialog() }, onAdd = { viewModel.addCard(it) })
    }

    if (state.showExpenseDialog && state.cards.isNotEmpty()) {
        AddCardExpenseDialog(
            cardId = state.cards[state.selectedCardIndex.coerceAtMost((state.cards.size - 1).coerceAtLeast(0))].card.id,
            accounts = accounts,
            expenseCategories = expenseCategories,
            onDismiss = { viewModel.toggleExpenseDialog() },
            onAdd = { viewModel.addExpense(it) }
        )
    }
}

@Composable
private fun CreditCardVisual(cardDetails: CreditCardWithDetails, modifier: Modifier = Modifier) {
    val card = cardDetails.card
    val usedPercent = if (card.totalLimit > 0) (cardDetails.usedLimit / card.totalLimit).toFloat() else 0f
    val gradient = Brush.linearGradient(
        colors = listOf(EmeraldGreenDark, BluePetrol),
        start = Offset(0f, 0f),
        end = Offset(400f, 300f)
    )

    Card(modifier = modifier.height(200.dp), elevation = CardDefaults.cardElevation(8.dp)) {
        Box(modifier = Modifier.fillMaxSize().background(gradient).padding(20.dp)) {
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text(card.bankName, style = MaterialTheme.typography.titleSmall, color = White.copy(alpha = 0.9f))
                        Text(card.name, style = MaterialTheme.typography.bodySmall, color = White.copy(alpha = 0.7f))
                    }
                    Text(card.brand.name, style = MaterialTheme.typography.labelLarge, color = GoldAccent, fontWeight = FontWeight.Bold)
                }

                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Limite Total", style = MaterialTheme.typography.labelSmall, color = White.copy(alpha = 0.7f))
                        Text("Disponível", style = MaterialTheme.typography.labelSmall, color = White.copy(alpha = 0.7f))
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(CurrencyUtils.formatBRL(card.totalLimit), style = MaterialTheme.typography.bodyMedium, color = White, fontWeight = FontWeight.Bold)
                        Text(CurrencyUtils.formatBRL(cardDetails.availableLimit), style = MaterialTheme.typography.bodyMedium, color = if (usedPercent > 0.8f) GoldAccent else White, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(progress = { usedPercent.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth(), color = if (usedPercent > 0.8f) GoldAccent else White, trackColor = White.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Fecha dia ${card.closingDay}", style = MaterialTheme.typography.labelSmall, color = White.copy(alpha = 0.7f))
                        Text("Vence dia ${card.dueDay}", style = MaterialTheme.typography.labelSmall, color = White.copy(alpha = 0.7f))
                    }
                }
            }
        }
    }
}

@Composable
private fun CardTransactionItem(transaction: TransactionEntity) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(transaction.description, style = MaterialTheme.typography.bodyMedium)
            Row {
                Text(DateUtils.formatDate(transaction.date), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (transaction.totalInstallments != null && transaction.totalInstallments > 1) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("${transaction.installmentNumber}/${transaction.totalInstallments}x", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(CurrencyUtils.formatBRL(transaction.amount), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = ExpenseRed)
            StatusBadge(transaction.status.name)
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddCardDialog(onDismiss: () -> Unit, onAdd: (CreditCardEntity) -> Unit) {
    var name by remember { mutableStateOf("") }
    var bank by remember { mutableStateOf("") }
    var limit by remember { mutableStateOf("") }
    var closingDay by remember { mutableStateOf("10") }
    var dueDay by remember { mutableStateOf("20") }
    var selectedBrand by remember { mutableStateOf(CreditCardBrand.VISA) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Novo Cartão") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nome do cartão") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = bank, onValueChange = { bank = it }, label = { Text("Banco") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = limit, onValueChange = { limit = it.filter { c -> c.isDigit() || c == '.' } }, label = { Text("Limite (R$)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = closingDay, onValueChange = { closingDay = it.filter { c -> c.isDigit() }.take(2) }, label = { Text("Fechamento") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = dueDay, onValueChange = { dueDay = it.filter { c -> c.isDigit() }.take(2) }, label = { Text("Vencimento") }, singleLine = true, modifier = Modifier.weight(1f))
                }
                Text("Bandeira:", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(CreditCardBrand.VISA, CreditCardBrand.MASTERCARD, CreditCardBrand.ELO).forEach { brand ->
                        FilterChip(selected = selectedBrand == brand, onClick = { selectedBrand = brand }, label = { Text(brand.name) })
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onAdd(CreditCardEntity(name = name, bankName = bank, brand = selectedBrand, totalLimit = limit.toDoubleOrNull() ?: 0.0, closingDay = closingDay.toIntOrNull() ?: 10, dueDay = dueDay.toIntOrNull() ?: 20, color = "#2E7D52", createdAt = DateUtils.today()))
                },
                enabled = name.isNotBlank() && bank.isNotBlank() && limit.isNotBlank()
            ) { Text("Adicionar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddCardExpenseDialog(
    cardId: Long,
    accounts: List<AccountEntity>,
    expenseCategories: List<CategoryEntity>,
    onDismiss: () -> Unit,
    onAdd: (TransactionEntity) -> Unit
) {
    var description by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var installments by remember { mutableStateOf("1") }
    var selectedAccountId by remember(accounts) { mutableStateOf(accounts.firstOrNull()?.id) }
    var selectedCategoryId by remember(expenseCategories) { mutableStateOf(expenseCategories.firstOrNull()?.id) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nova compra no cartão") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Descrição") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = amount, onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' || c == ',' } }, label = { Text("Valor (R$)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = installments, onValueChange = { installments = it.filter(Char::isDigit) }, label = { Text("Parcelas") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Text("Conta de origem:", style = MaterialTheme.typography.labelMedium)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    accounts.forEach { account ->
                        FilterChip(selected = selectedAccountId == account.id, onClick = { selectedAccountId = account.id }, label = { Text(account.name) })
                    }
                }
                Text("Categoria:", style = MaterialTheme.typography.labelMedium)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    expenseCategories.forEach { category ->
                        FilterChip(selected = selectedCategoryId == category.id, onClick = { selectedCategoryId = category.id }, label = { Text(category.name) })
                    }
                }
                if (accounts.isEmpty() || expenseCategories.isEmpty()) {
                    Text("Cadastre ao menos uma conta e uma categoria de despesa.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val value = amount.replace(",", ".").toDoubleOrNull() ?: return@Button
                    val totalInstallments = installments.toIntOrNull()?.coerceAtLeast(1) ?: 1
                    val accountId = selectedAccountId ?: return@Button
                    val categoryId = selectedCategoryId ?: return@Button
                    onAdd(
                        TransactionEntity(
                            type = TransactionType.EXPENSE,
                            description = description,
                            amount = value,
                            categoryId = categoryId,
                            accountId = accountId,
                            creditCardId = cardId,
                            date = DateUtils.today(),
                            status = TransactionStatus.PENDING,
                            installmentNumber = 1,
                            totalInstallments = totalInstallments,
                            createdAt = DateUtils.today(),
                            updatedAt = DateUtils.today()
                        )
                    )
                },
                enabled = description.isNotBlank() && amount.isNotBlank() && selectedAccountId != null && selectedCategoryId != null && accounts.isNotEmpty() && expenseCategories.isNotEmpty()
            ) { Text("Adicionar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
