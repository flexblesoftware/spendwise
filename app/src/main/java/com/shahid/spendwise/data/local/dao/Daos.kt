package com.shahid.spendwise.data.local.dao

import androidx.room.*
import com.shahid.spendwise.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY timestampMillis DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE isPrivacyLocked = 0 ORDER BY timestampMillis DESC")
    fun getPublicTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    fun getTransactionById(id: Long): Flow<TransactionEntity?>

    @Query("SELECT * FROM transactions WHERE timestampMillis BETWEEN :startMillis AND :endMillis ORDER BY timestampMillis DESC")
    fun getTransactionsBetween(startMillis: Long, endMillis: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE isTaxRelevant = 1 ORDER BY timestampMillis DESC")
    fun getTaxRelevantTransactions(): Flow<List<TransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transactions: List<TransactionEntity>)

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getCategoryById(id: Long): CategoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(categories: List<CategoryEntity>)

    @Update
    suspend fun updateCategory(category: CategoryEntity)

    @Delete
    suspend fun deleteCategory(category: CategoryEntity)
}

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets WHERE monthYear = :monthYear")
    fun getBudgetsForMonth(monthYear: String): Flow<List<BudgetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(budget: BudgetEntity): Long

    @Delete
    suspend fun deleteBudget(budget: BudgetEntity)
}

@Dao
interface RecurringDao {
    @Query("SELECT * FROM recurring_expenses ORDER BY nextDueDateMillis ASC")
    fun getAllRecurring(): Flow<List<RecurringEntity>>

    @Query("SELECT * FROM recurring_expenses WHERE nextDueDateMillis <= :currentMillis AND autoLog = 1")
    suspend fun getDueRecurring(currentMillis: Long): List<RecurringEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(recurring: RecurringEntity): Long

    @Update
    suspend fun update(recurring: RecurringEntity)

    @Delete
    suspend fun delete(recurring: RecurringEntity)
}

@Dao
interface PersonDao {
    @Query("SELECT * FROM people ORDER BY name ASC")
    fun getAllPeople(): Flow<List<PersonEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(person: PersonEntity): Long

    @Delete
    suspend fun delete(person: PersonEntity)
}

@Dao
interface SplitDao {
    @Query("SELECT * FROM split_expenses ORDER BY createdAtMillis DESC")
    fun getAllSplits(): Flow<List<SplitExpenseEntity>>

    @Query("SELECT * FROM split_participants WHERE splitExpenseId = :splitExpenseId")
    fun getParticipantsForSplit(splitExpenseId: Long): Flow<List<SplitParticipantEntity>>

    @Query("SELECT * FROM split_participants")
    fun getAllParticipants(): Flow<List<SplitParticipantEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSplit(split: SplitExpenseEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertParticipants(participants: List<SplitParticipantEntity>)

    @Query("UPDATE split_participants SET isSettled = :settled WHERE id = :participantId")
    suspend fun markSettled(participantId: Long, settled: Boolean)

    @Delete
    suspend fun deleteSplit(split: SplitExpenseEntity)
}

@Dao
interface DebtDao {
    @Query("SELECT * FROM debts ORDER BY createdAtMillis DESC")
    fun getAllDebts(): Flow<List<DebtEntity>>

    @Query("SELECT * FROM debts WHERE id = :id")
    fun getDebtById(id: Long): Flow<DebtEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDebt(debt: DebtEntity): Long

    @Update
    suspend fun updateDebt(debt: DebtEntity)

    @Delete
    suspend fun deleteDebt(debt: DebtEntity)

    @Query("SELECT * FROM repayments WHERE debtId = :debtId ORDER BY dateMillis DESC")
    fun getRepaymentsForDebt(debtId: Long): Flow<List<RepaymentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRepayment(repayment: RepaymentEntity): Long

    @Delete
    suspend fun deleteRepayment(repayment: RepaymentEntity)
}

@Dao
interface SavingsGoalDao {
    @Query("SELECT * FROM savings_goals ORDER BY deadlineMillis ASC")
    fun getAllGoals(): Flow<List<SavingsGoalEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: SavingsGoalEntity): Long

    @Update
    suspend fun updateGoal(goal: SavingsGoalEntity)

    @Delete
    suspend fun deleteGoal(goal: SavingsGoalEntity)
}

@Dao
interface CurrencyDao {
    @Query("SELECT * FROM currency_rates")
    fun getAllRates(): Flow<List<CurrencyRateEntity>>

    @Query("SELECT * FROM currency_rates WHERE currencyCode = :code")
    suspend fun getRate(code: String): CurrencyRateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(rate: CurrencyRateEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(rates: List<CurrencyRateEntity>)
}

@Dao
interface SavedFilterDao {
    @Query("SELECT * FROM saved_filters ORDER BY name ASC")
    fun getAllFilters(): Flow<List<SavedFilterEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFilter(filter: SavedFilterEntity): Long

    @Delete
    suspend fun deleteFilter(filter: SavedFilterEntity)
}
