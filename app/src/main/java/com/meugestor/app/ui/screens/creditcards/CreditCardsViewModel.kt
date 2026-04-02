package com.meugestor.app.ui.screens.creditcards

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.meugestor.app.data.database.entity.CreditCardEntity
import com.meugestor.app.data.database.entity.TransactionEntity
import com.meugestor.app.data.database.entity.TransactionStatus
import com.meugestor.app.data.repository.CreditCardRepository
import com.meugestor.app.data.repository.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CreditCardWithDetails(
    val card: CreditCardEntity,
    val usedLimit: Double = 0.0,
    val availableLimit: Double = 0.0,
    val currentInvoice: Double = 0.0
)

data class CreditCardsUiState(
    val cards: List<CreditCardWithDetails> = emptyList(),
    val selectedCardIndex: Int = 0,
    val selectedCardTransactions: List<TransactionEntity> = emptyList(),
    val isLoading: Boolean = true,
    val showAddDialog: Boolean = false,
    val showExpenseDialog: Boolean = false
)

class CreditCardsViewModel(
    private val creditCardRepository: CreditCardRepository,
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreditCardsUiState())
    val uiState: StateFlow<CreditCardsUiState> = _uiState.asStateFlow()

    init {
        loadCards()
    }

    private fun loadCards() {
        viewModelScope.launch {
            combine(
                creditCardRepository.getAllCards(),
                transactionRepository.getAllTransactions()
            ) { cards, allTransactions ->
                val details = cards.map { card ->
                    val cardTransactions = allTransactions.filter { it.creditCardId == card.id }
                    val used = cardTransactions
                        .filter { it.status != TransactionStatus.PAID && it.status != TransactionStatus.CANCELLED }
                        .sumOf { it.amount }
                    val currentInvoice = cardTransactions
                        .filter { it.status != TransactionStatus.CANCELLED }
                        .sumOf { it.amount }

                    CreditCardWithDetails(
                        card = card,
                        usedLimit = used,
                        availableLimit = (card.totalLimit - used).coerceAtLeast(0.0),
                        currentInvoice = currentInvoice
                    )
                }

                val safeIndex = _uiState.value.selectedCardIndex.coerceIn(0, (details.size - 1).coerceAtLeast(0))
                val selectedTransactions = if (details.isNotEmpty()) {
                    allTransactions.filter { it.creditCardId == details[safeIndex].card.id }
                } else {
                    emptyList()
                }

                Triple(details, safeIndex, selectedTransactions)
            }.collect { (details, safeIndex, selectedTransactions) ->
                _uiState.update {
                    it.copy(
                        cards = details,
                        selectedCardIndex = safeIndex,
                        selectedCardTransactions = selectedTransactions,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun selectCard(index: Int) {
        _uiState.update { it.copy(selectedCardIndex = index) }
    }

    fun addCard(card: CreditCardEntity) {
        viewModelScope.launch {
            creditCardRepository.insert(card)
            _uiState.update { it.copy(showAddDialog = false) }
        }
    }

    fun addExpense(transaction: TransactionEntity) {
        viewModelScope.launch {
            transactionRepository.addTransaction(transaction)
            _uiState.update { it.copy(showExpenseDialog = false) }
        }
    }

    fun deleteCard(card: CreditCardEntity) {
        viewModelScope.launch { creditCardRepository.delete(card) }
    }

    fun toggleAddDialog() {
        _uiState.update { it.copy(showAddDialog = !it.showAddDialog) }
    }

    fun toggleExpenseDialog() {
        _uiState.update { it.copy(showExpenseDialog = !it.showExpenseDialog) }
    }

    class Factory(
        private val creditCardRepository: CreditCardRepository,
        private val transactionRepository: TransactionRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CreditCardsViewModel(creditCardRepository, transactionRepository) as T
        }
    }
}
