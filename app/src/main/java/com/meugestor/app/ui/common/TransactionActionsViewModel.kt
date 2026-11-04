package com.meugestor.app.ui.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meugestor.app.data.database.entity.TransactionEntity
import com.meugestor.app.data.database.entity.TransactionStatus
import com.meugestor.app.data.database.entity.TransactionType
import com.meugestor.app.data.repository.TransactionRepository
import com.meugestor.app.domain.TransactionDraft
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

/**
 * Ações comuns sobre lançamentos (incluir, editar, marcar como pago, excluir) usadas por
 * várias telas. Erros são convertidos em mensagens para o usuário em vez de derrubar o app.
 */
abstract class TransactionActionsViewModel(
    protected val transactionRepository: TransactionRepository
) : ViewModel() {

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    protected fun showMessage(message: String) {
        _messages.tryEmit(message)
    }

    protected fun launchSafely(
        successMessage: String? = null,
        errorMessage: String = "Não foi possível concluir a operação.",
        block: suspend () -> Unit
    ) {
        viewModelScope.launch {
            try {
                block()
                successMessage?.let { showMessage(it) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                showMessage(errorMessage)
            }
        }
    }

    fun saveDraft(draft: TransactionDraft, editing: TransactionEntity?) {
        if (editing != null) {
            launchSafely(successMessage = "Lançamento atualizado") {
                transactionRepository.update(draft.applyTo(editing))
            }
        } else {
            val entities = draft.toEntities()
            val message = if (entities.size > 1) {
                "${entities.size} lançamentos criados"
            } else {
                "Lançamento salvo"
            }
            launchSafely(successMessage = message) {
                transactionRepository.addSeries(entities)
            }
        }
    }

    fun toggleStatus(transaction: TransactionEntity) {
        val newStatus = if (transaction.status == TransactionStatus.PAID) {
            TransactionStatus.PENDING
        } else {
            TransactionStatus.PAID
        }
        val message = when {
            newStatus == TransactionStatus.PENDING -> "Marcado como pendente"
            transaction.type == TransactionType.INCOME -> "Marcado como recebido"
            else -> "Marcado como pago"
        }
        launchSafely(successMessage = message) {
            transactionRepository.setStatus(transaction, newStatus)
        }
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        launchSafely(successMessage = "Lançamento excluído") {
            transactionRepository.delete(transaction)
        }
    }

    fun deleteOpenSeries(transaction: TransactionEntity) {
        viewModelScope.launch {
            try {
                val removed = transactionRepository.deleteOpenSeries(transaction)
                showMessage(if (removed == 1) "1 lançamento excluído" else "$removed lançamentos excluídos")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                showMessage("Não foi possível excluir os lançamentos.")
            }
        }
    }
}
