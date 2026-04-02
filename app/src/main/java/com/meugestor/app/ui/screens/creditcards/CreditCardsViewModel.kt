package com.meugestor.app.ui.screens.creditcards

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.meugestor.app.data.database.entity.CreditCardEntity
import com.meugestor.app.data.database.entity.TransactionEntity
import com.meugestor.app.data.repository.CreditCardRepository
import com.meugestor.app.data.repository.TransactionRepository
import kotlinx.coroutines.flow.*
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
    val showAddDialog: Boolean = false
)

class CreditCardsViewModel(
    private val creditCardRepository: CreditCardRepository,
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreditCardsUiState())
    val uiState: StateFlow<CreditCardsUiState> = _uiState.asStateFlow()

    init { loadCards() }

    private fun loadCards() {
        viewModelScope.launch {
            creditCardRepository.getAllCards().collect { cards ->
                val details = cards.map { card ->
                    CreditCardWithDetails(
                        card = card,
                        usedLimit = 0.0,
                        availableLimit = card.totalLimit,
                        currentInvoice = 0.0
                    )
                }
                _uiState.update { it.copy(cards = details, isLoading = false) }
                // Load transactions for selected card
                if (details.isNotEmpty()) {
                    loadTransactionsForCard(details[_uiState.value.selectedCardIndex].card.id)
                }
            }
        }
    }

    private fun loadTransactionsForCard(cardId: Long) {
        viewModelScope.launch {
            transactionRepository.getByCreditCard(cardId).collect { txList ->
                _uiState.update { it.copy(selectedCardTransactions = txList) }
            }
        }
    }

    fun selectCard(index: Int) {
        _uiState.update { it.copy(selectedCardIndex = index) }
        val cards = _uiState.value.cards
        if (index < cards.size) {
            loadTransactionsForCard(cards[index].card.id)
        }
    }

    fun addCard(card: CreditCardEntity) {
        viewModelScope.launch {
            creditCardRepository.insert(card)
            _uiState.update { it.copy(showAddDialog = false) }
        }
    }

    fun deleteCard(card: CreditCardEntity) {
        viewModelScope.launch { creditCardRepository.delete(card) }
    }

    fun toggleAddDialog() {
        _uiState.update { it.copy(showAddDialog = !it.showAddDialog) }
    }

    class Factory(
        private val creditCardRepository: CreditCardRepository,
        private val transactionRepository: TransactionRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            CreditCardsViewModel(creditCardRepository, transactionRepository) as T
    }
}
