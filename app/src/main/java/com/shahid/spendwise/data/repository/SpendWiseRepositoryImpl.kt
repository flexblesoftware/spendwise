package com.shahid.spendwise.data.repository

import com.shahid.spendwise.data.local.SpendWiseDatabase
import com.shahid.spendwise.data.local.entity.*
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SpendWiseRepositoryImpl @Inject constructor(
    private val db: SpendWiseDatabase
) : SpendWiseRepository {

    override fun getTransactions(includeLocked: Boolean): Flow<List<TransactionEntity>> {
        return if (includeLocked) {
            db.transactionDao().getAllTransactions()
        } else {
            db.transactionDao().getPublicTransactions()
        }
    }

    override fun getTransactionById(id: Long): Flow<TransactionEntity?> =
        db.transactionDao().getTransactionById(id)

    override fun getTransactionsBetween(startMillis: Long, endMillis: Long): Flow<List<TransactionEntity>> =
        db.transactionDao().getTransactionsBetween(startMillis, endMillis)

    override fun getTaxRelevantTransactions(): Flow<List<TransactionEntity>> =
        db.transactionDao().getTaxRelevantTransactions()

    override suspend fun insertTransaction(transaction: TransactionEntity): Long =
        db.transactionDao().insertTransaction(transaction)

    override suspend fun insertAllTransactions(transactions: List<TransactionEntity>) =
        db.transactionDao().insertAll(transactions)

    override suspend fun updateTransaction(transaction: TransactionEntity) =
        db.transactionDao().updateTransaction(transaction)

    override suspend fun deleteTransaction(transaction: TransactionEntity) =
        db.transactionDao().deleteTransaction(transaction)

    override suspend fun deleteTransactionById(id: Long) =
        db.transactionDao().deleteById(id)

    override fun getAllCategories(): Flow<List<CategoryEntity>> =
        db.categoryDao().getAllCategories()

    override suspend fun getCategoryById(id: Long): CategoryEntity? =
        db.categoryDao().getCategoryById(id)

    override suspend fun insertCategory(category: CategoryEntity): Long =
        db.categoryDao().insertCategory(category)

    override suspend fun updateCategory(category: CategoryEntity) =
        db.categoryDao().updateCategory(category)

    override suspend fun deleteCategory(category: CategoryEntity) =
        db.categoryDao().deleteCategory(category)

    override fun getBudgetsForMonth(monthYear: String): Flow<List<BudgetEntity>> =
        db.budgetDao().getBudgetsForMonth(monthYear)

    override suspend fun saveBudget(budget: BudgetEntity): Long =
        db.budgetDao().insertOrUpdate(budget)

    override suspend fun deleteBudget(budget: BudgetEntity) =
        db.budgetDao().deleteBudget(budget)

    override fun getAllRecurring(): Flow<List<RecurringEntity>> =
        db.recurringDao().getAllRecurring()

    override suspend fun getDueRecurring(currentMillis: Long): List<RecurringEntity> =
        db.recurringDao().getDueRecurring(currentMillis)

    override suspend fun insertRecurring(recurring: RecurringEntity): Long =
        db.recurringDao().insert(recurring)

    override suspend fun updateRecurring(recurring: RecurringEntity) =
        db.recurringDao().update(recurring)

    override suspend fun deleteRecurring(recurring: RecurringEntity) =
        db.recurringDao().delete(recurring)

    override fun getAllPeople(): Flow<List<PersonEntity>> =
        db.personDao().getAllPeople()

    override suspend fun insertPerson(person: PersonEntity): Long =
        db.personDao().insert(person)

    override suspend fun deletePerson(person: PersonEntity) =
        db.personDao().delete(person)

    override fun getAllSplits(): Flow<List<SplitExpenseEntity>> =
        db.splitDao().getAllSplits()

    override fun getParticipantsForSplit(splitId: Long): Flow<List<SplitParticipantEntity>> =
        db.splitDao().getParticipantsForSplit(splitId)

    override fun getAllParticipants(): Flow<List<SplitParticipantEntity>> =
        db.splitDao().getAllParticipants()

    override suspend fun insertSplit(
        split: SplitExpenseEntity,
        participants: List<SplitParticipantEntity>
    ): Long {
        val splitId = db.splitDao().insertSplit(split)
        val linkedParticipants = participants.map { it.copy(splitExpenseId = splitId) }
        db.splitDao().insertParticipants(linkedParticipants)
        return splitId
    }

    override suspend fun markParticipantSettled(participantId: Long, settled: Boolean) =
        db.splitDao().markSettled(participantId, settled)

    override suspend fun deleteSplit(split: SplitExpenseEntity) =
        db.splitDao().deleteSplit(split)

    override fun getAllDebts(): Flow<List<DebtEntity>> =
        db.debtDao().getAllDebts()

    override fun getDebtById(id: Long): Flow<DebtEntity?> =
        db.debtDao().getDebtById(id)

    override suspend fun insertDebt(debt: DebtEntity): Long =
        db.debtDao().insertDebt(debt)

    override suspend fun updateDebt(debt: DebtEntity) =
        db.debtDao().updateDebt(debt)

    override suspend fun deleteDebt(debt: DebtEntity) =
        db.debtDao().deleteDebt(debt)

    override fun getRepaymentsForDebt(debtId: Long): Flow<List<RepaymentEntity>> =
        db.debtDao().getRepaymentsForDebt(debtId)

    override suspend fun insertRepayment(repayment: RepaymentEntity): Long =
        db.debtDao().insertRepayment(repayment)

    override suspend fun deleteRepayment(repayment: RepaymentEntity) =
        db.debtDao().deleteRepayment(repayment)

    override fun getAllGoals(): Flow<List<SavingsGoalEntity>> =
        db.savingsGoalDao().getAllGoals()

    override suspend fun insertGoal(goal: SavingsGoalEntity): Long =
        db.savingsGoalDao().insertGoal(goal)

    override suspend fun updateGoal(goal: SavingsGoalEntity) =
        db.savingsGoalDao().updateGoal(goal)

    override suspend fun deleteGoal(goal: SavingsGoalEntity) =
        db.savingsGoalDao().deleteGoal(goal)

    override fun getAllRates(): Flow<List<CurrencyRateEntity>> =
        db.currencyDao().getAllRates()

    override suspend fun getRate(currencyCode: String): CurrencyRateEntity? =
        db.currencyDao().getRate(currencyCode)

    override suspend fun updateRate(rate: CurrencyRateEntity) =
        db.currencyDao().insertOrUpdate(rate)

    override fun getAllFilters(): Flow<List<SavedFilterEntity>> =
        db.filterDao().getAllFilters()

    override suspend fun insertFilter(filter: SavedFilterEntity): Long =
        db.filterDao().insertFilter(filter)

    override suspend fun deleteFilter(filter: SavedFilterEntity) =
        db.filterDao().deleteFilter(filter)
}
