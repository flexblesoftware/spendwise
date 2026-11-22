package com.shahid.spendwise.ui.screens.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shahid.spendwise.data.local.entity.CategoryEntity
import com.shahid.spendwise.data.repository.PreferencesRepository
import com.shahid.spendwise.data.repository.SpendWiseRepository
import com.shahid.spendwise.domain.model.Money
import com.shahid.spendwise.domain.model.TransactionType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import java.util.Calendar
import javax.inject.Inject

data class CategoryExpenseStat(
    val category: CategoryEntity?,
    val totalAmountMinor: Long,
    val formattedAmount: String,
    val percentageOfTotal: Float
)

data class MonthComparisonItem(
    val categoryName: String,
    val currentMonthMinor: Long,
    val previousMonthMinor: Long,
    val changePercent: Int,
    val isIncrease: Boolean
)

data class AnalyticsUiState(
    val isYearly: Boolean = false,
    val totalExpensesFormatted: String = "$ 0.00",
    val totalIncomeFormatted: String = "$ 0.00",
    val categoryBreakdown: List<CategoryExpenseStat> = emptyList(),
    val monthOverMonthComparisons: List<MonthComparisonItem> = emptyList(),
    val baseCurrency: String = "USD",
    val isLoading: Boolean = true
)

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val repository: SpendWiseRepository,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    private val _isYearly = MutableStateFlow(false)

    val uiState: StateFlow<AnalyticsUiState> = combine(
        repository.getTransactions(includeLocked = false),
        repository.getAllCategories(),
        preferencesRepository.baseCurrency,
        _isYearly
    ) { transactions, categories, baseCurrency, isYearly ->
        val catMap = categories.associateBy { it.id }

        val cal = Calendar.getInstance()
        val currentYear = cal.get(Calendar.YEAR)
        val currentMonth = cal.get(Calendar.MONTH)

        val prevCal = Calendar.getInstance().apply {
            set(Calendar.MONTH, currentMonth - 1)
        }
        val prevYear = prevCal.get(Calendar.YEAR)
        val prevMonth = prevCal.get(Calendar.MONTH)

        val currentPeriodExpenses = mutableListOf<com.shahid.spendwise.data.local.entity.TransactionEntity>()
        val currentPeriodIncomes = mutableListOf<com.shahid.spendwise.data.local.entity.TransactionEntity>()
        val prevMonthExpenses = mutableListOf<com.shahid.spendwise.data.local.entity.TransactionEntity>()

        for (tx in transactions) {
            val txCal = Calendar.getInstance().apply { timeInMillis = tx.timestampMillis }
            val y = txCal.get(Calendar.YEAR)
            val m = txCal.get(Calendar.MONTH)

            val matchesPeriod = if (isYearly) y == currentYear else (y == currentYear && m == currentMonth)
            if (matchesPeriod) {
                if (tx.type == TransactionType.EXPENSE) currentPeriodExpenses.add(tx) else currentPeriodIncomes.add(tx)
            }

            if (!isYearly && y == prevYear && m == prevMonth && tx.type == TransactionType.EXPENSE) {
                prevMonthExpenses.add(tx)
            }
        }

        val totalExpensesMinor = currentPeriodExpenses.sumOf { it.amountMinor }
        val totalIncomeMinor = currentPeriodIncomes.sumOf { it.amountMinor }

        val breakdown = currentPeriodExpenses.groupBy { it.categoryId }
            .map { (catId, txs) ->
                val sum = txs.sumOf { it.amountMinor }
                val ratio = if (totalExpensesMinor > 0) sum.toFloat() / totalExpensesMinor else 0f
                CategoryExpenseStat(
                    category = catMap[catId],
                    totalAmountMinor = sum,
                    formattedAmount = Money(sum, baseCurrency).format(),
                    percentageOfTotal = ratio
                )
            }.sortedByDescending { it.totalAmountMinor }

        val comparisons = mutableListOf<MonthComparisonItem>()
        if (!isYearly) {
            val currCatMap = currentPeriodExpenses.groupBy { it.categoryId }.mapValues { it.value.sumOf { t -> t.amountMinor } }
            val prevCatMap = prevMonthExpenses.groupBy { it.categoryId }.mapValues { it.value.sumOf { t -> t.amountMinor } }

            for ((catId, currAmount) in currCatMap) {
                val prevAmount = prevCatMap[catId] ?: 0L
                val catName = catMap[catId]?.name ?: "General"

                val changePercent = if (prevAmount > 0) {
                    (((currAmount - prevAmount).toDouble() / prevAmount) * 100).toInt()
                } else {
                    100
                }

                comparisons.add(
                    MonthComparisonItem(
                        categoryName = catName,
                        currentMonthMinor = currAmount,
                        previousMonthMinor = prevAmount,
                        changePercent = kotlin.math.abs(changePercent),
                        isIncrease = currAmount >= prevAmount
                    )
                )
            }
        }

        AnalyticsUiState(
            isYearly = isYearly,
            totalExpensesFormatted = Money(totalExpensesMinor, baseCurrency).format(),
            totalIncomeFormatted = Money(totalIncomeMinor, baseCurrency).format(),
            categoryBreakdown = breakdown,
            monthOverMonthComparisons = comparisons.sortedByDescending { it.currentMonthMinor },
            baseCurrency = baseCurrency,
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AnalyticsUiState())

    fun setYearly(yearly: Boolean) {
        _isYearly.value = yearly
    }
}
