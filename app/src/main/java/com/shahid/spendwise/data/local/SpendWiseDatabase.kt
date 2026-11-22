package com.shahid.spendwise.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.shahid.spendwise.data.local.converter.Converters
import com.shahid.spendwise.data.local.dao.*
import com.shahid.spendwise.data.local.entity.*
import com.shahid.spendwise.domain.model.TransactionType

@Database(
    entities = [
        TransactionEntity::class,
        CategoryEntity::class,
        BudgetEntity::class,
        RecurringEntity::class,
        PersonEntity::class,
        SplitExpenseEntity::class,
        SplitParticipantEntity::class,
        DebtEntity::class,
        RepaymentEntity::class,
        SavingsGoalEntity::class,
        CurrencyRateEntity::class,
        SavedFilterEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class SpendWiseDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun budgetDao(): BudgetDao
    abstract fun recurringDao(): RecurringDao
    abstract fun personDao(): PersonDao
    abstract fun splitDao(): SplitDao
    abstract fun debtDao(): DebtDao
    abstract fun savingsGoalDao(): SavingsGoalDao
    abstract fun currencyDao(): CurrencyDao
    abstract fun filterDao(): SavedFilterDao

    companion object {
        const val DATABASE_NAME = "spendwise_db"

        val DEFAULT_CATEGORIES = listOf(
            CategoryEntity(1, "Food & Dining", "Restaurant", "#EF4444", TransactionType.EXPENSE, false),
            CategoryEntity(2, "Groceries", "ShoppingCart", "#F97316", TransactionType.EXPENSE, false),
            CategoryEntity(3, "Transport", "DirectionsCar", "#F59E0B", TransactionType.EXPENSE, false),
            CategoryEntity(4, "Housing & Rent", "Home", "#10B981", TransactionType.EXPENSE, true),
            CategoryEntity(5, "Utilities & Bills", "Bolt", "#06B6D4", TransactionType.EXPENSE, true),
            CategoryEntity(6, "Shopping", "ShoppingBag", "#3B82F6", TransactionType.EXPENSE, false),
            CategoryEntity(7, "Health & Medical", "LocalHospital", "#EC4899", TransactionType.EXPENSE, true),
            CategoryEntity(8, "Entertainment", "Movie", "#8B5CF6", TransactionType.EXPENSE, false),
            CategoryEntity(9, "Education", "School", "#6366F1", TransactionType.EXPENSE, true),
            CategoryEntity(10, "Travel", "Flight", "#14B8A6", TransactionType.EXPENSE, false),
            CategoryEntity(11, "Salary", "AccountBalanceWallet", "#10B981", TransactionType.INCOME, true),
            CategoryEntity(12, "Freelance & Business", "Work", "#3B82F6", TransactionType.INCOME, true),
            CategoryEntity(13, "Investments & Dividends", "TrendingUp", "#8B5CF6", TransactionType.INCOME, true),
            CategoryEntity(14, "Gifts & Grants", "CardGiftcard", "#F59E0B", TransactionType.INCOME, false)
        )

        val DEFAULT_CURRENCIES = listOf(
            CurrencyRateEntity("USD", 1.0, System.currentTimeMillis()),
            CurrencyRateEntity("EUR", 0.95, System.currentTimeMillis()),
            CurrencyRateEntity("GBP", 0.79, System.currentTimeMillis()),
            CurrencyRateEntity("INR", 84.50, System.currentTimeMillis()),
            CurrencyRateEntity("JPY", 154.0, System.currentTimeMillis()),
            CurrencyRateEntity("CAD", 1.40, System.currentTimeMillis()),
            CurrencyRateEntity("AUD", 1.55, System.currentTimeMillis())
        )
    }
}
