package com.meugestor.app.ui.screens.creditcards

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.meugestor.app.data.database.entity.CreditCardEntity
import com.meugestor.app.data.database.entity.TransactionEntity
import com.meugestor.app.data.database.entity.TransactionStatus
import com.meugestor.app.data.repository.CreditCardRepository
import com.meugestor.app.data.repository.TransactionRepository
import com.meugestor.app.domain.CreditCardInvoice
import com.meugestor.app.domain.withStatus
import com.meugestor.app.ui.common.TransactionActionsViewModel
import com.meugestor.app.util.DateUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

data class CreditCardWithDetails(
    val card: CreditCardEntity,
    val usedLimit: Double = 0.0,
    val availableLimit: Double = 0.0,
    /** Total da próxima fatura em aberto (a mais antiga ainda não paga). */
    val nextInvoiceAmount: Double = 0.0,
    val nextInvoiceDueDate: String? = null,
    val nextInvoiceOverdue: Boolean = false,
    val nextInvoiceItems: List<TransactionEntity> = emptyList(),
    val transactions: List<TransactionEntity> = emptyList()
)

data class CreditCardsUiState(
    val cards: List<CreditCardWithDetails> = emptyList(),
    val selectedCardIndex: Int = 0,
    val selectedCardTransactions: List<TransactionEntity> = emptyList(),
    val isLoading: Boolean = true
) {
    val selectedCard: CreditCardWithDetails? get() = cards.getOrNull(selectedCardIndex)
}

/** Dados do formulário de compra no cartão. */
data class CardPurchaseInput(
    val description: String,
    val totalAmount: Double,
    val installments: Int,
    val purchaseDate: String,
    val accountId: Long,
    val categoryId: Long?,
    val alreadyPaid: Boolean
)

class CreditCardsViewModel(
    private val creditCardRepository: CreditCardRepository,
    transactionRepository: TransactionRepository
) : TransactionActionsViewModel(transactionRepository) {

    private val selectedIndex = MutableStateFlow(0)

    // Antes selectCard() só mudava o índice e a lista de lançamentos continuava a do cartão anterior.
    val uiState: StateFlow<CreditCardsUiState> = combine(
        creditCardRepository.getAllCards(),
        transactionRepository.getCreditCardTransactions(),
        selectedIndex
    ) { cards, cardTransactions, index ->
        val today = DateUtils.todayDate()
        val byCard = cardTransactions.groupBy { it.creditCardId }
        val details = cards.map { card -> buildDetails(card, byCard[card.id].orEmpty(), today) }
        val safeIndex = index.coerceIn(0, (details.size - 1).coerceAtLeast(0))
        CreditCardsUiState(
            cards = details,
            selectedCardIndex = safeIndex,
            selectedCardTransactions = details.getOrNull(safeIndex)?.transactions.orEmpty(),
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CreditCardsUiState())

    private fun buildDetails(
        card: CreditCardEntity,
        transactions: List<TransactionEntity>,
        today: LocalDate
    ): CreditCardWithDetails {
        val open = transactions.filter {
            it.status != TransactionStatus.PAID && it.status != TransactionStatus.CANCELLED
        }
        val used = open.sumOf { it.amount }
        val openByDue = open.groupBy { CreditCardInvoice.effectiveDue(it, card) }
        val nextDue: LocalDate? = openByDue.keys.minByOrNull { it.toEpochDay() }
        val nextItems = nextDue?.let { openByDue[it] }.orEmpty()
        return CreditCardWithDetails(
            card = card,
            usedLimit = used,
            availableLimit = (card.totalLimit - used).coerceAtLeast(0.0),
            nextInvoiceAmount = nextItems.sumOf { it.amount },
            nextInvoiceDueDate = nextDue?.let { DateUtils.toIso(it) },
            nextInvoiceOverdue = nextDue?.isBefore(today) == true,
            nextInvoiceItems = nextItems,
            transactions = transactions.sortedWith(
                compareByDescending<TransactionEntity> { it.date }.thenByDescending { it.id }
            )
        )
    }

    fun selectCard(index: Int) {
        selectedIndex.value = index
    }

    fun addCard(card: CreditCardEntity) {
        launchSafely(successMessage = "Cartão adicionado") {
            creditCardRepository.insert(card)
        }
    }

    fun deleteCard(card: CreditCardEntity) {
        launchSafely(successMessage = "Cartão excluído") {
            creditCardRepository.delete(card)
        }
    }

    /** Registra a compra gerando uma linha por parcela, cada uma na fatura correspondente. */
    fun addPurchase(card: CreditCardEntity, input: CardPurchaseInput) {
        val purchaseDate = DateUtils.parse(input.purchaseDate) ?: DateUtils.todayDate()
        val installments = CreditCardInvoice.buildInstallments(
            card = card,
            description = input.description.trim(),
            totalAmount = input.totalAmount,
            installments = input.installments,
            purchaseDate = purchaseDate,
            accountId = input.accountId,
            categoryId = input.categoryId,
            status = if (input.alreadyPaid) TransactionStatus.PAID else TransactionStatus.PENDING
        )
        val message = if (installments.size > 1) {
            "Compra registrada em ${installments.size} parcelas"
        } else {
            "Compra registrada"
        }
        launchSafely(successMessage = message) {
            transactionRepository.addSeries(installments)
        }
    }

    /**
     * Paga a próxima fatura do cartão: marca os itens como pagos e debita o total
     * da conta escolhida.
     */
    fun payNextInvoice(details: CreditCardWithDetails, accountId: Long) {
        val items = details.nextInvoiceItems
        if (items.isEmpty()) return
        val today = DateUtils.today()
        val updated = items.map { item ->
            item.withStatus(TransactionStatus.PAID, today).copy(
                accountId = accountId,
                dueDate = item.dueDate ?: DateUtils.toIso(CreditCardInvoice.effectiveDue(item, details.card))
            )
        }
        launchSafely(successMessage = "Fatura paga") {
            transactionRepository.updateAll(updated)
        }
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
