package com.meugestor.app.ui.screens.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.meugestor.app.data.database.entity.GoalEntity
import com.meugestor.app.data.database.entity.GoalPeriod
import com.meugestor.app.data.repository.GoalRepository
import com.meugestor.app.data.repository.GoalProgress
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class GoalsUiState(
    val goals: List<GoalProgress> = emptyList(),
    val isLoading: Boolean = true,
    val showAddDialog: Boolean = false
)

class GoalsViewModel(
    private val goalRepository: GoalRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GoalsUiState())
    val uiState: StateFlow<GoalsUiState> = _uiState.asStateFlow()

    init { loadGoals() }

    private fun loadGoals() {
        viewModelScope.launch {
            goalRepository.getActive().collect { goals ->
                val progresses = goals.map { goal ->
                    goalRepository.checkGoalProgress(goal)
                }
                _uiState.update { it.copy(goals = progresses, isLoading = false) }
            }
        }
    }

    fun addGoal(goal: GoalEntity) {
        viewModelScope.launch {
            goalRepository.insert(goal)
            _uiState.update { it.copy(showAddDialog = false) }
        }
    }

    fun deleteGoal(goal: GoalEntity) {
        viewModelScope.launch { goalRepository.delete(goal) }
    }

    fun toggleAddDialog() {
        _uiState.update { it.copy(showAddDialog = !it.showAddDialog) }
    }

    class Factory(
        private val goalRepository: GoalRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            GoalsViewModel(goalRepository) as T
    }
}
