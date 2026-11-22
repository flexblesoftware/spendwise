package com.shahid.spendwise.data.repository

import com.shahid.spendwise.data.local.entity.*
import kotlinx.coroutines.flow.Flow

interface SpendWiseRepository {
    fun getTransactions(includeLocked: Boolean): Flow<List<TransactionEntity>>
    fun getTransactionById(id: Long): Flow<TransactionEntity?>
    fun getTransactionsBetween(startMillis: Long, endMillis: Long): Flow<List<TransactionEntity>>
    fun getTaxRelevantTransactions(): Flow<List<TransactionEntity>>
    suspend fun insertTransaction(transaction: TransactionEntity): Long
    suspend fun insertAllTransactions(transactions: List<TransactionEntity>)
    suspend fun updateTransaction(transaction: TransactionEntity)
    suspend fun deleteTransaction(transaction: TransactionEntity)
    suspend fun deleteTransactionById(id: Long)

    fun getAllCategories(): Flow<List<CategoryEntity>>
    suspend fun getCategoryById(id: Long): CategoryEntity?
    suspend fun insertCategory(category: CategoryEntity): Long
    suspend fun updateCategory(category: CategoryEntity)
    suspend fun deleteCategory(category: CategoryEntity)

    fun getBudgetsForMonth(monthYear: String): Flow<List<BudgetEntity>>
    suspend fun saveBudget(budget: BudgetEntity): Long
    suspend fun deleteBudget(budget: BudgetEntity)

    fun getAllRecurring(): Flow<List<RecurringEntity>>
    suspend fun getDueRecurring(currentMillis: Long): List<RecurringEntity>
    suspend fun insertRecurring(recurring: RecurringEntity): Long
    suspend fun updateRecurring(recurring: RecurringEntity)
    suspend fun deleteRecurring(recurring: RecurringEntity)

    fun getAllPeople(): Flow<List<PersonEntity>>
    suspend fun insertPerson(person: PersonEntity): Long
    suspend fun deletePerson(person: PersonEntity)

    fun getAllSplits(): Flow<List<SplitExpenseEntity>>
    fun getParticipantsForSplit(splitId: Long): Flow<List<SplitParticipantEntity>>
    fun getAllParticipants(): Flow<List<SplitParticipantEntity>>
    suspend fun insertSplit(split: SplitExpenseEntity, participants: List<SplitParticipantEntity>): Long
    suspend fun markParticipantSettled(participantId: Long, settled: Boolean)
    suspend fun deleteSplit(split: SplitExpenseEntity)

    fun getAllDebts(): Flow<List<DebtEntity>>
    fun getDebtById(id: Long): Flow<DebtEntity?>
    suspend fun insertDebt(debt: DebtEntity): Long
    suspend fun updateDebt(debt: DebtEntity)
    suspend fun deleteDebt(debt: DebtEntity)
    fun getRepaymentsForDebt(debtId: Long): Flow<List<RepaymentEntity>>
    suspend fun insertRepayment(repayment: RepaymentEntity): Long
    suspend fun deleteRepayment(repayment: RepaymentEntity)

    fun getAllGoals(): Flow<List<SavingsGoalEntity>>
    suspend fun insertGoal(goal: SavingsGoalEntity): Long
    suspend fun updateGoal(goal: SavingsGoalEntity)
    suspend fun deleteGoal(goal: SavingsGoalEntity)

    fun getAllRates(): Flow<List<CurrencyRateEntity>>
    suspend fun getRate(currencyCode: String): CurrencyRateEntity?
    suspend fun updateRate(rate: CurrencyRateEntity)

    fun getAllFilters(): Flow<List<SavedFilterEntity>>
    suspend fun insertFilter(filter: SavedFilterEntity): Long
    suspend fun deleteFilter(filter: SavedFilterEntity)
}
