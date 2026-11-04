@file:OptIn(
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.meugestor.app.ui.screens.creditcards

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.meugestor.app.MeuGestorApp
import com.meugestor.app.data.database.entity.*
import com.meugestor.app.domain.CreditCardInvoice
import com.meugestor.app.ui.common.CollectMessages
import com.meugestor.app.ui.common.TransactionDialogsHost
import com.meugestor.app.ui.common.rememberTransactionDialogState
import com.meugestor.app.ui.common.rememberTransactionLookups
import com.meugestor.app.ui.components.AppDatePickerDialog
import com.meugestor.app.ui.components.ChipSelector
import com.meugestor.app.ui.components.ConfirmDialog
import com.meugestor.app.ui.components.DateFieldButton
import com.meugestor.app.ui.components.EmptyState
import com.meugestor.app.ui.components.TransactionRow
import com.meugestor.app.ui.theme.*
import com.meugestor.app.util.CurrencyUtils
import com.meugestor.app.util.DateUtils

private enum class CardTransactionFilter {
    ALL, OPEN, PAID
}

@Composable
fun CreditCardsScreen(app: MeuGestorApp) {
    val viewModel: CreditCardsViewModel = viewModel(
        factory = CreditCardsViewModel.Factory(app.creditCardRepository, app.transactionRepository)
    )
    val state by viewModel.uiState.collectAsState()
    val accountsFlow = remember(app) { app.accountRepository.getAllAccounts() }
    val expenseFlow = remember(app) { app.categoryRepository.getByType("EXPENSE") }
    val accounts by accountsFlow.collectAsState(initial = emptyList())
    val expenseCategories by expenseFlow.collectAsState(initial = emptyList())
    val dialogs = rememberTransactionDialogState()
    val lookups = rememberTransactionLookups(app)

    val pagerState = rememberPagerState { state.cards.size.coerceAtLeast(1) }
    var transactionFilter by rememberSaveable { mutableStateOf(CardTransactionFilter.ALL) }
    var showAddCard by remember { mutableStateOf(false) }
    var showPurchase by remember { mutableStateOf(false) }
    var showPayInvoice by remember { mutableStateOf(false) }
    var cardToDelete by remember { mutableStateOf<CreditCardEntity?>(null) }

    CollectMessages(viewModel.messages)

    LaunchedEffect(pagerState.currentPage, state.cards.size) {
        if (state.cards.isNotEmpty()) {
            viewModel.selectCard(pagerState.currentPage.coerceAtMost(state.cards.lastIndex))
        }
    }

    val selected = state.selectedCard
    val filteredTransactions = remember(state.selectedCardTransactions, transactionFilter) {
        when (transactionFilter) {
            CardTransactionFilter.ALL -> state.selectedCardTransactions
            CardTransactionFilter.OPEN -> state.selectedCardTransactions.filter {
                it.status == TransactionStatus.PENDING || it.status == TransactionStatus.OVERDUE
            }
            CardTransactionFilter.PAID -> state.selectedCardTransactions.filter {
                it.status == TransactionStatus.PAID
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            if (state.cards.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = { showPurchase = true },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Nova compra") }
                )
            }
        }
    ) { padding ->
        if (state.cards.isEmpty() && !state.isLoading) {
            EmptyState(
                icon = Icons.Default.CreditCard,
                title = "Nenhum cartão cadastrado",
                description = "Adicione seu primeiro cartão de crédito",
                actionLabel = "Adicionar Cartão",
                onAction = { showAddCard = true },
                modifier = Modifier.padding(padding)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Seus cartões",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        OutlinedButton(onClick = { showAddCard = true }) {
                            Text("Novo cartão")
                        }
                    }
                }

                item {
                    HorizontalPager(
                        state = pagerState,
                        contentPadding = PaddingValues(horizontal = 32.dp),
                        pageSpacing = 16.dp
                    ) { page ->
                        if (page < state.cards.size) {
                            CreditCardVisual(
                                cardDetails = state.cards[page],
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                if (state.cards.size > 1) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
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
                }

                if (selected != null) {
                    item {
                        InvoiceSummaryCard(
                            details = selected,
                            onPay = { showPayInvoice = true },
                            onDeleteCard = { cardToDelete = selected.card }
                        )
                    }

                    item {
                        Text(
                            "Lançamentos do Cartão",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = transactionFilter == CardTransactionFilter.ALL,
                                onClick = { transactionFilter = CardTransactionFilter.ALL },
                                label = { Text("Todas") }
                            )
                            FilterChip(
                                selected = transactionFilter == CardTransactionFilter.OPEN,
                                onClick = { transactionFilter = CardTransactionFilter.OPEN },
                                label = { Text("Em aberto") }
                            )
                            FilterChip(
                                selected = transactionFilter == CardTransactionFilter.PAID,
                                onClick = { transactionFilter = CardTransactionFilter.PAID },
                                label = { Text("Pagas") }
                            )
                        }
                    }

                    if (filteredTransactions.isEmpty()) {
                        item {
                            Text(
                                "Nenhum lançamento para este filtro",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    } else {
                        items(filteredTransactions, key = { it.id }) { tx ->
                            TransactionRow(
                                transaction = tx,
                                categoryName = lookups.categoryName(tx),
                                showDueInfo = true,
                                onClick = { dialogs.openDetails(tx) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddCard) {
        AddCardDialog(
            onDismiss = { showAddCard = false },
            onAdd = {
                viewModel.addCard(it)
                showAddCard = false
            }
        )
    }

    if (showPurchase && selected != null) {
        AddCardPurchaseDialog(
            card = selected.card,
            accounts = accounts,
            expenseCategories = expenseCategories,
            onDismiss = { showPurchase = false },
            onAdd = { input ->
                viewModel.addPurchase(selected.card, input)
                showPurchase = false
            }
        )
    }

    if (showPayInvoice && selected != null) {
        PayInvoiceDialog(
            details = selected,
            accounts = accounts,
            onDismiss = { showPayInvoice = false },
            onConfirm = { accountId ->
                viewModel.payNextInvoice(selected, accountId)
                showPayInvoice = false
            }
        )
    }

    cardToDelete?.let { card ->
        ConfirmDialog(
            title = "Excluir cartão?",
            message = "\"${card.name}\" será excluído junto com as compras em aberto. " +
                "Compras já pagas continuam no histórico da conta.",
            confirmLabel = "Excluir",
            onConfirm = {
                viewModel.deleteCard(card)
                cardToDelete = null
            },
            onDismiss = { cardToDelete = null }
        )
    }

    TransactionDialogsHost(app = app, viewModel = viewModel, state = dialogs, lookups = lookups)
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
                    Column(modifier = Modifier.weight(1f)) {
                        Text(card.bankName, style = MaterialTheme.typography.titleSmall, color = White.copy(alpha = 0.9f), maxLines = 1)
                        Text(card.name, style = MaterialTheme.typography.bodySmall, color = White.copy(alpha = 0.7f), maxLines = 1)
                    }
                    Text(brandLabel(card.brand), style = MaterialTheme.typography.labelLarge, color = GoldAccent, fontWeight = FontWeight.Bold)
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
private fun InvoiceSummaryCard(
    details: CreditCardWithDetails,
    onPay: () -> Unit,
    onDeleteCard: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        if (details.nextInvoiceOverdue) "Fatura em atraso" else "Próxima fatura",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (details.nextInvoiceOverdue) ExpenseRed else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        CurrencyUtils.formatBRL(details.nextInvoiceAmount),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (details.nextInvoiceOverdue) ExpenseRed else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        details.nextInvoiceDueDate?.let { "Vence em ${DateUtils.formatDate(it)}" }
                            ?: "Nenhuma fatura em aberto",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(onClick = onPay, enabled = details.nextInvoiceItems.isNotEmpty()) {
                    Text("Pagar fatura")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Limite em uso: ${CurrencyUtils.formatBRL(details.usedLimit)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(onClick = onDeleteCard) {
                    Text("Excluir cartão", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

private fun brandLabel(brand: CreditCardBrand): String = when (brand) {
    CreditCardBrand.VISA -> "VISA"
    CreditCardBrand.MASTERCARD -> "MASTERCARD"
    CreditCardBrand.ELO -> "ELO"
    CreditCardBrand.AMEX -> "AMEX"
    CreditCardBrand.HIPERCARD -> "HIPERCARD"
    CreditCardBrand.OTHER -> "OUTRA"
}

@Composable
private fun AddCardDialog(onDismiss: () -> Unit, onAdd: (CreditCardEntity) -> Unit) {
    var name by remember { mutableStateOf("") }
    var bank by remember { mutableStateOf("") }
    var limit by remember { mutableStateOf("") }
    var closingDay by remember { mutableStateOf("10") }
    var dueDay by remember { mutableStateOf("20") }
    var selectedBrand by remember { mutableStateOf(CreditCardBrand.VISA) }

    val limitValue = CurrencyUtils.parseAmount(limit)
    val closing = closingDay.toIntOrNull()
    val due = dueDay.toIntOrNull()
    val daysValid = closing != null && closing in 1..31 && due != null && due in 1..31
    val canSave = name.isNotBlank() && bank.isNotBlank() && limitValue != null && limitValue > 0 && daysValid

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Novo Cartão") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nome do cartão") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = bank, onValueChange = { bank = it }, label = { Text("Banco") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    value = limit,
                    onValueChange = { limit = CurrencyUtils.filterAmountInput(it) },
                    label = { Text("Limite (R$)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = closingDay,
                        onValueChange = { closingDay = it.filter { c -> c.isDigit() }.take(2) },
                        label = { Text("Fechamento") },
                        singleLine = true,
                        isError = closing == null || closing !in 1..31,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = dueDay,
                        onValueChange = { dueDay = it.filter { c -> c.isDigit() }.take(2) },
                        label = { Text("Vencimento") },
                        singleLine = true,
                        isError = due == null || due !in 1..31,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
                Text("Bandeira:", style = MaterialTheme.typography.labelMedium)
                ChipSelector(
                    options = CreditCardBrand.values().toList(),
                    isSelected = { it == selectedBrand },
                    label = { brandLabel(it) },
                    onSelect = { selectedBrand = it }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onAdd(
                        CreditCardEntity(
                            name = name.trim(),
                            bankName = bank.trim(),
                            brand = selectedBrand,
                            totalLimit = limitValue ?: 0.0,
                            closingDay = closing ?: 10,
                            dueDay = due ?: 20,
                            color = "#2E7D52",
                            createdAt = DateUtils.today()
                        )
                    )
                },
                enabled = canSave
            ) { Text("Adicionar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
private fun AddCardPurchaseDialog(
    card: CreditCardEntity,
    accounts: List<AccountEntity>,
    expenseCategories: List<CategoryEntity>,
    onDismiss: () -> Unit,
    onAdd: (CardPurchaseInput) -> Unit
) {
    var description by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var installments by remember { mutableStateOf("1") }
    var purchaseDate by remember { mutableStateOf(DateUtils.today()) }
    var alreadyPaid by remember { mutableStateOf(false) }
    var selectedAccountId by remember { mutableStateOf(accounts.firstOrNull()?.id) }
    var selectedCategoryId by remember { mutableStateOf(expenseCategories.firstOrNull()?.id) }
    var showDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(accounts) {
        if (accounts.isNotEmpty() && accounts.none { it.id == selectedAccountId }) selectedAccountId = accounts.first().id
    }
    LaunchedEffect(expenseCategories) {
        if (expenseCategories.isNotEmpty() && expenseCategories.none { it.id == selectedCategoryId }) {
            selectedCategoryId = expenseCategories.first().id
        }
    }

    val value = CurrencyUtils.parseAmount(amount)
    val amountValid = value != null && value > 0
    val count = installments.toIntOrNull()?.coerceIn(1, CreditCardInvoice.MAX_INSTALLMENTS) ?: 1
    val firstDue = DateUtils.parse(purchaseDate)?.let { CreditCardInvoice.dueDateForPurchase(it, card) }
    val canSave = description.isNotBlank() && amountValid && selectedAccountId != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nova compra • ${card.name}") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descrição") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = CurrencyUtils.filterAmountInput(it) },
                    label = { Text("Valor total (R$)") },
                    singleLine = true,
                    isError = amount.isNotBlank() && !amountValid,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = installments,
                    onValueChange = { installments = it.filter(Char::isDigit).take(2) },
                    label = { Text("Parcelas") },
                    singleLine = true,
                    supportingText = {
                        if (amountValid && count > 1) {
                            val parts = CreditCardInvoice.splitAmount(value ?: 0.0, count)
                            Text("${count}x de ${CurrencyUtils.formatBRL(parts.last())}")
                        } else {
                            Text("1 = à vista")
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                DateFieldButton(
                    label = "Data da compra",
                    isoDate = purchaseDate,
                    onClick = { showDatePicker = true }
                )
                firstDue?.let {
                    Text(
                        "Primeira parcela na fatura que vence em ${DateUtils.formatDate(it)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text("Status:", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = !alreadyPaid,
                        onClick = { alreadyPaid = false },
                        label = { Text("Em aberto") }
                    )
                    FilterChip(
                        selected = alreadyPaid,
                        onClick = { alreadyPaid = true },
                        label = { Text("Já paga") }
                    )
                }

                Text("Conta que paga a fatura:", style = MaterialTheme.typography.labelMedium)
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

                if (expenseCategories.isNotEmpty()) {
                    Text("Categoria:", style = MaterialTheme.typography.labelMedium)
                    ChipSelector(
                        options = expenseCategories,
                        isSelected = { it.id == selectedCategoryId },
                        label = { it.name },
                        onSelect = { selectedCategoryId = it.id }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val total = value ?: return@Button
                    val accountId = selectedAccountId ?: return@Button
                    onAdd(
                        CardPurchaseInput(
                            description = description,
                            totalAmount = total,
                            installments = count,
                            purchaseDate = purchaseDate,
                            accountId = accountId,
                            categoryId = selectedCategoryId,
                            alreadyPaid = alreadyPaid
                        )
                    )
                },
                enabled = canSave
            ) {
                Text("Adicionar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )

    if (showDatePicker) {
        AppDatePickerDialog(
            initialDate = purchaseDate,
            onDismiss = { showDatePicker = false },
            onDateSelected = { purchaseDate = it }
        )
    }
}

@Composable
private fun PayInvoiceDialog(
    details: CreditCardWithDetails,
    accounts: List<AccountEntity>,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit
) {
    val defaultAccountId = details.nextInvoiceItems.firstOrNull()?.accountId
    var selectedAccountId by remember {
        mutableStateOf(defaultAccountId?.takeIf { id -> accounts.any { it.id == id } } ?: accounts.firstOrNull()?.id)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pagar fatura") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "${details.card.name}: ${CurrencyUtils.formatBRL(details.nextInvoiceAmount)}" +
                        (details.nextInvoiceDueDate?.let { " • vence em ${DateUtils.formatDate(it)}" } ?: ""),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "${details.nextInvoiceItems.size} lançamento(s) serão marcados como pagos e o valor será debitado da conta escolhida.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text("Pagar com:", style = MaterialTheme.typography.labelMedium)
                if (accounts.isEmpty()) {
                    Text(
                        "Nenhuma conta cadastrada.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                } else {
                    ChipSelector(
                        options = accounts,
                        isSelected = { it.id == selectedAccountId },
                        label = { "${it.name} (${CurrencyUtils.formatBRL(it.balance)})" },
                        onSelect = { selectedAccountId = it.id }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { selectedAccountId?.let(onConfirm) },
                enabled = selectedAccountId != null
            ) { Text("Confirmar pagamento") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
