package com.shahid.spendwise.ui.screens.recurring

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shahid.spendwise.data.local.entity.CategoryEntity
import com.shahid.spendwise.data.local.entity.RecurringEntity
import com.shahid.spendwise.data.repository.PreferencesRepository
import com.shahid.spendwise.data.repository.SpendWiseRepository
import com.shahid.spendwise.domain.model.Money
import com.shahid.spendwise.domain.model.RecurrenceFrequency
import com.shahid.spendwise.domain.usecase.RecurringScheduleHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RecurringUiState(
    val recurringList: List<RecurringEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val baseCurrency: String = "USD",
    val isLoading: Boolean = true
)

@HiltViewModel
class RecurringViewModel @Inject constructor(
    private val repository: SpendWiseRepository,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    val uiState: StateFlow<RecurringUiState> = combine(
        repository.getAllRecurring(),
        repository.getAllCategories(),
        preferencesRepository.baseCurrency
    ) { recurring, categories, baseCurrency ->
        RecurringUiState(
            recurringList = recurring,
            categories = categories,
            baseCurrency = baseCurrency,
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RecurringUiState())

    fun addRecurring(
        title: String,
        amountMinor: Long,
        categoryId: Long,
        frequency: RecurrenceFrequency,
        startDateMillis: Long,
        autoLog: Boolean,
        reminderEnabled: Boolean
    ) {
        viewModelScope.launch {
            val nextDue = RecurringScheduleHelper.calculateNextDueDate(startDateMillis, frequency)
            val item = RecurringEntity(
                title = title,
                amountMinor = amountMinor,
                currency = preferencesRepository.baseCurrency.value,
                categoryId = categoryId,
                frequency = frequency,
                startDateMillis = startDateMillis,
                nextDueDateMillis = nextDue,
                autoLog = autoLog,
                reminderEnabled = reminderEnabled
            )
            repository.insertRecurring(item)
        }
    }

    fun deleteRecurring(recurring: RecurringEntity) {
        viewModelScope.launch {
            repository.deleteRecurring(recurring)
        }
    }

    fun toggleAutoLog(recurring: RecurringEntity, autoLog: Boolean) {
        viewModelScope.launch {
            repository.updateRecurring(recurring.copy(autoLog = autoLog))
        }
    }
}
