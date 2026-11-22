package com.shahid.spendwise.ui.screens.debt

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shahid.spendwise.data.local.entity.DebtEntity
import com.shahid.spendwise.data.local.entity.PersonEntity
import com.shahid.spendwise.data.local.entity.RepaymentEntity
import com.shahid.spendwise.data.repository.PreferencesRepository
import com.shahid.spendwise.data.repository.SpendWiseRepository
import com.shahid.spendwise.domain.model.DebtStatus
import com.shahid.spendwise.domain.model.DebtType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DebtsUiState(
    val debts: List<DebtEntity> = emptyList(),
    val people: List<PersonEntity> = emptyList(),
    val baseCurrency: String = "USD",
    val isLoading: Boolean = true
)

@HiltViewModel
class DebtsViewModel @Inject constructor(
    private val repository: SpendWiseRepository,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    val uiState: StateFlow<DebtsUiState> = combine(
        repository.getAllDebts(),
        repository.getAllPeople(),
        preferencesRepository.baseCurrency
    ) { debts, people, baseCurrency ->
        DebtsUiState(
            debts = debts,
            people = people,
            baseCurrency = baseCurrency,
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DebtsUiState())

    fun addDebt(
        personId: Long,
        type: DebtType,
        amountMinor: Long,
        dueDateMillis: Long?,
        note: String
    ) {
        viewModelScope.launch {
            val debt = DebtEntity(
                personId = personId,
                type = type,
                amountMinor = amountMinor,
                currency = preferencesRepository.baseCurrency.value,
                dueDateMillis = dueDateMillis,
                status = DebtStatus.OPEN,
                note = note.trim(),
                createdAtMillis = System.currentTimeMillis()
            )
            repository.insertDebt(debt)
        }
    }

    fun addRepayment(debt: DebtEntity, repaymentMinor: Long, note: String) {
        viewModelScope.launch {
            val repayment = RepaymentEntity(
                debtId = debt.id,
                amountMinor = repaymentMinor,
                dateMillis = System.currentTimeMillis(),
                note = note.trim()
            )
            repository.insertRepayment(repayment)

            val repayments = repository.getRepaymentsForDebt(debt.id).firstOrNull() ?: emptyList()
            val totalRepaid = repayments.sumOf { it.amountMinor } + repaymentMinor

            val newStatus = if (totalRepaid >= debt.amountMinor) {
                DebtStatus.SETTLED
            } else {
                DebtStatus.PARTIALLY_PAID
            }
            repository.updateDebt(debt.copy(status = newStatus))
        }
    }

    fun markSettled(debt: DebtEntity) {
        viewModelScope.launch {
            repository.updateDebt(debt.copy(status = DebtStatus.SETTLED))
        }
    }

    fun deleteDebt(debt: DebtEntity) {
        viewModelScope.launch {
            repository.deleteDebt(debt)
        }
    }
}
