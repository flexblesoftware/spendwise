package com.shahid.spendwise.ui.screens.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shahid.spendwise.data.local.entity.SavingsGoalEntity
import com.shahid.spendwise.data.repository.PreferencesRepository
import com.shahid.spendwise.data.repository.SpendWiseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GoalsUiState(
    val goals: List<SavingsGoalEntity> = emptyList(),
    val baseCurrency: String = "USD",
    val isLoading: Boolean = true
)

@HiltViewModel
class GoalsViewModel @Inject constructor(
    private val repository: SpendWiseRepository,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    val uiState: StateFlow<GoalsUiState> = combine(
        repository.getAllGoals(),
        preferencesRepository.baseCurrency
    ) { goals, baseCurrency ->
        GoalsUiState(
            goals = goals,
            baseCurrency = baseCurrency,
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), GoalsUiState())

    fun createGoal(
        title: String,
        targetAmountMinor: Long,
        deadlineMillis: Long,
        colorHex: String
    ) {
        viewModelScope.launch {
            val goal = SavingsGoalEntity(
                title = title.trim(),
                targetAmountMinor = targetAmountMinor,
                currentSavedMinor = 0L,
                currency = preferencesRepository.baseCurrency.value,
                deadlineMillis = deadlineMillis,
                colorHex = colorHex
            )
            repository.insertGoal(goal)
        }
    }

    fun addContribution(goal: SavingsGoalEntity, contributionMinor: Long) {
        viewModelScope.launch {
            val updated = goal.copy(
                currentSavedMinor = goal.currentSavedMinor + contributionMinor
            )
            repository.updateGoal(updated)
        }
    }

    fun deleteGoal(goal: SavingsGoalEntity) {
        viewModelScope.launch {
            repository.deleteGoal(goal)
        }
    }
}
