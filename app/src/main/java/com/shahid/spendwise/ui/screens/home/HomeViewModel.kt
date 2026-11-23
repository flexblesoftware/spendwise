package com.shahid.spendwise.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shahid.spendwise.data.local.entity.BudgetEntity
import com.shahid.spendwise.data.local.entity.CategoryEntity
import com.shahid.spendwise.data.local.entity.TransactionEntity
import com.shahid.spendwise.data.repository.PreferencesRepository
import com.shahid.spendwise.data.repository.SpendWiseRepository
import com.shahid.spendwise.domain.model.Money
import com.shahid.spendwise.domain.model.TransactionType
import com.shahid.spendwise.update.UpdateManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

data class HomeUiState(
    val netBalanceFormatted: String = "$ 0.00",
    val monthIncomeFormatted: String = "$ 0.00",
    val monthExpenseFormatted: String = "$ 0.00",
    val overallBudget: BudgetEntity? = null,
    val budgetSpentPercentage: Float = 0f,
    val recentTransactions: List<TransactionEntity> = emptyList(),
    val categoryMap: Map<Long, CategoryEntity> = emptyMap(),
    val isPrivacyUnlocked: Boolean = false,
    val baseCurrency: String = "USD",
    val isLoading: Boolean = true
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: SpendWiseRepository,
    private val preferencesRepository: PreferencesRepository,
    val updateManager: UpdateManager
) : ViewModel() {

    private val _isPrivacyUnlocked = MutableStateFlow(false)

    val uiState: StateFlow<HomeUiState> = combine(
        _isPrivacyUnlocked.flatMapLatest { unlocked -> repository.getTransactions(includeLocked = unlocked) },
        repository.getAllCategories(),
        preferencesRepository.baseCurrency,
        repository.getAllRates(),
        _isPrivacyUnlocked
    ) { transactions, categories, baseCurrency, rates, isUnlocked ->
        val catMap = categories.associateBy { it.id }
        val rateMap = rates.associateBy { it.currencyCode }

        val cal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfMonthMillis = cal.timeInMillis

        var monthIncome = 0L
        var monthExpense = 0L
        var totalBalance = 0L

        for (tx in transactions) {
            val rate = if (tx.currency.equals(baseCurrency, ignoreCase = true)) {
                1.0
            } else {
                rateMap[tx.currency]?.rateToBase ?: 1.0
            }
            val convertedAmount = Money.convertMinor(tx.amountMinor, rate)

            if (tx.type == TransactionType.INCOME) {
                totalBalance += convertedAmount
                if (tx.timestampMillis >= startOfMonthMillis) {
                    monthIncome += convertedAmount
                }
            } else {
                totalBalance -= convertedAmount
                if (tx.timestampMillis >= startOfMonthMillis) {
                    monthExpense += convertedAmount
                }
            }
        }

        HomeUiState(
            netBalanceFormatted = Money(totalBalance, baseCurrency).format(),
            monthIncomeFormatted = Money(monthIncome, baseCurrency).format(),
            monthExpenseFormatted = Money(monthExpense, baseCurrency).format(),
            recentTransactions = transactions.take(15),
            categoryMap = catMap,
            isPrivacyUnlocked = isUnlocked,
            baseCurrency = baseCurrency,
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    init {
        viewModelScope.launch {
            updateManager.checkForUpdates(forceCheck = false)
        }
    }

    fun setPrivacyUnlocked(unlocked: Boolean) {
        _isPrivacyUnlocked.value = unlocked
    }

    fun quickAddExpense(amountMinor: Long, categoryId: Long, note: String) {
        viewModelScope.launch {
            val tx = TransactionEntity(
                type = TransactionType.EXPENSE,
                amountMinor = amountMinor,
                currency = preferencesRepository.baseCurrency.value,
                categoryId = categoryId,
                timestampMillis = System.currentTimeMillis(),
                note = note,
                tags = listOf("quick-add")
            )
            repository.insertTransaction(tx)
        }
    }
}
