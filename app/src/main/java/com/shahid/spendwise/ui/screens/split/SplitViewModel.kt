package com.shahid.spendwise.ui.screens.split

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shahid.spendwise.data.local.entity.PersonEntity
import com.shahid.spendwise.data.local.entity.SplitExpenseEntity
import com.shahid.spendwise.data.local.entity.SplitParticipantEntity
import com.shahid.spendwise.data.repository.PreferencesRepository
import com.shahid.spendwise.data.repository.SpendWiseRepository
import com.shahid.spendwise.domain.model.Money
import com.shahid.spendwise.domain.model.SplitType
import com.shahid.spendwise.domain.usecase.DebtTransfer
import com.shahid.spendwise.domain.usecase.SplitCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SplitUiState(
    val people: List<PersonEntity> = emptyList(),
    val splits: List<SplitExpenseEntity> = emptyList(),
    val participants: List<SplitParticipantEntity> = emptyList(),
    val whoOwesWhom: List<DebtTransfer> = emptyList(),
    val baseCurrency: String = "USD",
    val isLoading: Boolean = true
)

@HiltViewModel
class SplitViewModel @Inject constructor(
    private val repository: SpendWiseRepository,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    val uiState: StateFlow<SplitUiState> = combine(
        repository.getAllPeople(),
        repository.getAllSplits(),
        repository.getAllParticipants(),
        preferencesRepository.baseCurrency
    ) { people, splits, participants, baseCurrency ->
        val splitMap = splits.associateBy { it.id }
        val netBalances = mutableMapOf<Long, Long>()

        for (person in people) {
            netBalances[person.id] = 0L
        }

        for (p in participants) {
            if (!p.isSettled) {
                val split = splitMap[p.splitExpenseId]
                if (split != null) {
                    val payerId = split.paidByPersonId
                    val owed = p.owedAmountMinor
                    if (p.personId != payerId) {
                        netBalances[payerId] = (netBalances[payerId] ?: 0L) + owed
                        netBalances[p.personId] = (netBalances[p.personId] ?: 0L) - owed
                    }
                }
            }
        }

        val transfers = SplitCalculator.simplifyDebts(netBalances)

        SplitUiState(
            people = people,
            splits = splits,
            participants = participants,
            whoOwesWhom = transfers,
            baseCurrency = baseCurrency,
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SplitUiState())

    fun addPerson(name: String) {
        viewModelScope.launch {
            repository.insertPerson(PersonEntity(name = name.trim()))
        }
    }

    fun createEqualSplit(
        title: String,
        totalAmountMinor: Long,
        paidByPersonId: Long,
        participantIds: List<Long>
    ) {
        viewModelScope.launch {
            val splitExpense = SplitExpenseEntity(
                title = title,
                totalAmountMinor = totalAmountMinor,
                currency = preferencesRepository.baseCurrency.value,
                paidByPersonId = paidByPersonId,
                splitType = SplitType.EQUAL,
                createdAtMillis = System.currentTimeMillis()
            )

            val splitResults = SplitCalculator.splitEqually(totalAmountMinor, participantIds)
            val participants = splitResults.map { res ->
                SplitParticipantEntity(
                    splitExpenseId = 0L,
                    personId = res.personId,
                    owedAmountMinor = res.owedAmountMinor,
                    isSettled = res.personId == paidByPersonId
                )
            }
            repository.insertSplit(splitExpense, participants)
        }
    }

    fun markSettled(participantId: Long, settled: Boolean) {
        viewModelScope.launch {
            repository.markParticipantSettled(participantId, settled)
        }
    }
}
