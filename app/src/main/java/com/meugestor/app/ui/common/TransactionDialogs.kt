package com.meugestor.app.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.meugestor.app.MeuGestorApp
import com.meugestor.app.data.database.entity.TransactionEntity
import com.meugestor.app.data.database.entity.TransactionType
import com.meugestor.app.ui.components.TransactionActionsDialog
import com.meugestor.app.ui.components.TransactionFormDialog

/** Controla quais diálogos de lançamento estão abertos em uma tela. */
@Stable
class TransactionDialogState {
    var selected by mutableStateOf<TransactionEntity?>(null)
    var editing by mutableStateOf<TransactionEntity?>(null)
    var creatingType by mutableStateOf<TransactionType?>(null)

    fun openDetails(transaction: TransactionEntity) {
        selected = transaction
    }

    fun create(type: TransactionType) {
        creatingType = type
    }
}

@Composable
fun rememberTransactionDialogState(): TransactionDialogState = remember { TransactionDialogState() }

/** Dados auxiliares (contas, categorias, cartões) para exibir nomes e preencher formulários. */
class TransactionLookups(
    val accountNames: Map<Long, String>,
    val categoryNames: Map<Long, String>,
    val cardNames: Map<Long, String>
) {
    fun categoryName(transaction: TransactionEntity): String? =
        transaction.categoryId?.let { categoryNames[it] }
}

@Composable
fun rememberTransactionLookups(app: MeuGestorApp): TransactionLookups {
    val accountsFlow = remember(app) { app.accountRepository.getAllAccounts() }
    val categoriesFlow = remember(app) { app.categoryRepository.getAllCategories() }
    val cardsFlow = remember(app) { app.creditCardRepository.getAllCards() }
    val accounts by accountsFlow.collectAsState(initial = emptyList())
    val categories by categoriesFlow.collectAsState(initial = emptyList())
    val cards by cardsFlow.collectAsState(initial = emptyList())
    return remember(accounts, categories, cards) {
        TransactionLookups(
            accountNames = accounts.associate { it.id to it.name },
            categoryNames = categories.associate { it.id to it.name },
            cardNames = cards.associate { it.id to it.name }
        )
    }
}

/**
 * Exibe os diálogos de detalhes/ações, criação e edição de lançamentos
 * conforme o [state], delegando as operações ao [viewModel].
 */
@Composable
fun TransactionDialogsHost(
    app: MeuGestorApp,
    viewModel: TransactionActionsViewModel,
    state: TransactionDialogState,
    lookups: TransactionLookups
) {
    val accountsFlow = remember(app) { app.accountRepository.getAllAccounts() }
    val incomeFlow = remember(app) { app.categoryRepository.getByType("INCOME") }
    val expenseFlow = remember(app) { app.categoryRepository.getByType("EXPENSE") }
    val accounts by accountsFlow.collectAsState(initial = emptyList())
    val incomeCategories by incomeFlow.collectAsState(initial = emptyList())
    val expenseCategories by expenseFlow.collectAsState(initial = emptyList())

    val selected = state.selected
    if (selected != null) {
        val editable = selected.type == TransactionType.INCOME || selected.type == TransactionType.EXPENSE
        TransactionActionsDialog(
            transaction = selected,
            categoryName = lookups.categoryName(selected),
            accountName = lookups.accountNames[selected.accountId],
            cardName = selected.creditCardId?.let { lookups.cardNames[it] },
            onDismiss = { state.selected = null },
            onToggleStatus = { viewModel.toggleStatus(selected) },
            onDelete = { viewModel.deleteTransaction(selected) },
            onEdit = if (editable) {
                {
                    state.selected = null
                    state.editing = selected
                }
            } else {
                null
            },
            onDeleteOpenSeries = { viewModel.deleteOpenSeries(selected) }
        )
    }

    val editing = state.editing
    val creatingType = state.creatingType
    if (editing != null || creatingType != null) {
        TransactionFormDialog(
            accounts = accounts,
            incomeCategories = incomeCategories,
            expenseCategories = expenseCategories,
            initialType = creatingType ?: TransactionType.EXPENSE,
            editing = editing,
            onDismiss = {
                state.editing = null
                state.creatingType = null
            },
            onSave = { draft ->
                viewModel.saveDraft(draft, editing)
                state.editing = null
                state.creatingType = null
            }
        )
    }
}
