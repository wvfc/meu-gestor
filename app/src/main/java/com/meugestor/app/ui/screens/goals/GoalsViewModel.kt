package com.meugestor.app.ui.screens.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.meugestor.app.data.database.entity.GoalEntity
import com.meugestor.app.data.database.entity.GoalPeriod
import com.meugestor.app.data.repository.CategoryRepository
import com.meugestor.app.data.repository.GoalProgress
import com.meugestor.app.data.repository.GoalRepository
import com.meugestor.app.data.repository.TransactionRepository
import com.meugestor.app.util.DateUtils
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class GoalsUiState(
    val goals: List<GoalProgress> = emptyList(),
    val isLoading: Boolean = true
)

@OptIn(ExperimentalCoroutinesApi::class)
class GoalsViewModel(
    private val goalRepository: GoalRepository,
    transactionRepository: TransactionRepository,
    categoryRepository: CategoryRepository
) : ViewModel() {

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    // O progresso é recalculado sempre que metas, categorias ou lançamentos mudam
    // (antes só era calculado quando a lista de metas mudava).
    val uiState: StateFlow<GoalsUiState> = combine(
        goalRepository.getActive(),
        categoryRepository.getAllCategories(),
        transactionRepository.getAllTransactions()
    ) { goals, categories, _ ->
        goals to categories.associate { it.id to it.name }
    }.mapLatest { (goals, categoryNames) ->
        val progresses = goals.map { goal ->
            goalRepository.checkGoalProgress(goal).copy(categoryName = categoryNames[goal.categoryId])
        }
        GoalsUiState(goals = progresses, isLoading = false)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GoalsUiState())

    fun addGoal(name: String, categoryId: Long, targetAmount: Double, period: GoalPeriod) {
        val (start, end) = GoalRepository.currentPeriod(period)
        val goal = GoalEntity(
            name = name.trim(),
            categoryId = categoryId,
            targetAmount = targetAmount,
            period = period,
            startDate = start,
            endDate = end,
            createdAt = DateUtils.today()
        )
        launchWithFeedback("Meta criada") { goalRepository.insert(goal) }
    }

    fun deleteGoal(goal: GoalEntity) {
        launchWithFeedback("Meta excluída") { goalRepository.delete(goal) }
    }

    private fun launchWithFeedback(success: String, block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
                _messages.tryEmit(success)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _messages.tryEmit("Não foi possível concluir a operação.")
            }
        }
    }

    class Factory(
        private val goalRepository: GoalRepository,
        private val transactionRepository: TransactionRepository,
        private val categoryRepository: CategoryRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            GoalsViewModel(goalRepository, transactionRepository, categoryRepository) as T
    }
}
