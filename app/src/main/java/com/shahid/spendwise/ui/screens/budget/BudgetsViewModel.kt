package com.shahid.spendwise.ui.screens.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shahid.spendwise.data.local.entity.BudgetEntity
import com.shahid.spendwise.data.local.entity.CategoryEntity
import com.shahid.spendwise.data.repository.PreferencesRepository
import com.shahid.spendwise.data.repository.SpendWiseRepository
import com.shahid.spendwise.domain.model.Money
import com.shahid.spendwise.domain.model.TransactionType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

data class BudgetProgressItem(
    val budget: BudgetEntity,
    val category: CategoryEntity?,
    val spentMinor: Long,
    val budgetMinor: Long,
    val spentFormatted: String,
    val budgetFormatted: String,
    val progress: Float,
    val isOver80Percent: Boolean,
    val isExceeded: Boolean
)

data class BudgetsUiState(
    val monthYear: String = SimpleDateFormat("yyyy-MM", Locale.US).format(Date()),
    val overallBudget: BudgetProgressItem? = null,
    val categoryBudgets: List<BudgetProgressItem> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val baseCurrency: String = "USD",
    val isLoading: Boolean = true
)

@HiltViewModel
class BudgetsViewModel @Inject constructor(
    private val repository: SpendWiseRepository,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    private val currentMonthYear = SimpleDateFormat("yyyy-MM", Locale.US).format(Date())

    val uiState: StateFlow<BudgetsUiState> = combine(
        repository.getBudgetsForMonth(currentMonthYear),
        repository.getTransactions(includeLocked = false),
        repository.getAllCategories(),
        preferencesRepository.baseCurrency,
        repository.getAllRates()
    ) { budgets, transactions, categories, baseCurrency, rates ->
        val catMap = categories.associateBy { it.id }
        val rateMap = rates.associateBy { it.currencyCode }

        val cal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfMonth = cal.timeInMillis

        val monthExpenses = transactions.filter {
            it.type == TransactionType.EXPENSE && it.timestampMillis >= startOfMonth
        }

        // Convert each expense to base currency using exchange rate
        var totalMonthExpense = 0L
        val categoryExpenseMap = mutableMapOf<Long, Long>()

        for (tx in monthExpenses) {
            val rate = if (tx.currency.equals(baseCurrency, ignoreCase = true)) {
                1.0
            } else {
                rateMap[tx.currency]?.rateToBase ?: 1.0
            }
            val converted = Money.convertMinor(tx.amountMinor, rate)
            totalMonthExpense += converted
            categoryExpenseMap[tx.categoryId] = (categoryExpenseMap[tx.categoryId] ?: 0L) + converted
        }

        var overallItem: BudgetProgressItem? = null
        val catItems = mutableListOf<BudgetProgressItem>()

        for (b in budgets) {
            if (b.categoryId == null) {
                val ratio = if (b.amountMinor > 0) totalMonthExpense.toFloat() / b.amountMinor else 0f
                overallItem = BudgetProgressItem(
                    budget = b,
                    category = null,
                    spentMinor = totalMonthExpense,
                    budgetMinor = b.amountMinor,
                    spentFormatted = Money(totalMonthExpense, baseCurrency).format(),
                    budgetFormatted = Money(b.amountMinor, baseCurrency).format(),
                    progress = ratio.coerceIn(0f, 1f),
                    isOver80Percent = ratio >= 0.8f,
                    isExceeded = ratio >= 1.0f
                )
            } else {
                val spent = categoryExpenseMap[b.categoryId] ?: 0L
                val ratio = if (b.amountMinor > 0) spent.toFloat() / b.amountMinor else 0f
                catItems.add(
                    BudgetProgressItem(
                        budget = b,
                        category = catMap[b.categoryId],
                        spentMinor = spent,
                        budgetMinor = b.amountMinor,
                        spentFormatted = Money(spent, baseCurrency).format(),
                        budgetFormatted = Money(b.amountMinor, baseCurrency).format(),
                        progress = ratio.coerceIn(0f, 1f),
                        isOver80Percent = ratio >= 0.8f,
                        isExceeded = ratio >= 1.0f
                    )
                )
            }
        }

        BudgetsUiState(
            monthYear = currentMonthYear,
            overallBudget = overallItem,
            categoryBudgets = catItems,
            categories = categories.filter { it.type == TransactionType.EXPENSE },
            baseCurrency = baseCurrency,
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BudgetsUiState())

    fun saveBudget(categoryId: Long?, amountMinor: Long) {
        viewModelScope.launch {
            val budget = BudgetEntity(
                categoryId = categoryId,
                amountMinor = amountMinor,
                monthYear = currentMonthYear
            )
            repository.saveBudget(budget)
        }
    }

    fun deleteBudget(budget: BudgetEntity) {
        viewModelScope.launch {
            repository.deleteBudget(budget)
        }
    }
}
