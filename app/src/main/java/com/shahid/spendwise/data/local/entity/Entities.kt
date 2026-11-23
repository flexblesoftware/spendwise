package com.shahid.spendwise.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.shahid.spendwise.domain.model.DebtStatus
import com.shahid.spendwise.domain.model.DebtType
import com.shahid.spendwise.domain.model.RecurrenceFrequency
import com.shahid.spendwise.domain.model.SplitType
import com.shahid.spendwise.domain.model.TransactionType
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "transactions",
    indices = [
        Index("timestampMillis"),
        Index("categoryId"),
        Index("isPrivacyLocked")
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: TransactionType,
    val amountMinor: Long,
    val currency: String,
    val categoryId: Long,
    val timestampMillis: Long,
    val note: String,
    val tags: List<String> = emptyList(),
    val receiptPhotoPath: String? = null,
    val isTaxRelevant: Boolean = false,
    val isPrivacyLocked: Boolean = false,
    val recurringId: Long? = null
)

@Serializable
@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val iconName: String,
    val colorHex: String,
    val type: TransactionType,
    val isTaxDeductible: Boolean = false
)

@Serializable
@Entity(
    tableName = "budgets",
    indices = [Index(value = ["categoryId", "monthYear"], unique = true)]
)
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val categoryId: Long? = null, // null represents overall monthly budget
    val amountMinor: Long,
    val monthYear: String // Format: "YYYY-MM"
)

@Serializable
@Entity(tableName = "recurring_expenses")
data class RecurringEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val amountMinor: Long,
    val currency: String,
    val categoryId: Long,
    val frequency: RecurrenceFrequency,
    val startDateMillis: Long,
    val nextDueDateMillis: Long,
    val autoLog: Boolean = true,
    val reminderEnabled: Boolean = true
)

@Serializable
@Entity(tableName = "people")
data class PersonEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val emailOrPhone: String? = null
)

@Serializable
@Entity(tableName = "split_expenses")
data class SplitExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val totalAmountMinor: Long,
    val currency: String,
    val paidByPersonId: Long,
    val splitType: SplitType,
    val createdAtMillis: Long
)

@Serializable
@Entity(
    tableName = "split_participants",
    indices = [Index("splitExpenseId"), Index("personId")]
)
data class SplitParticipantEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val splitExpenseId: Long,
    val personId: Long,
    val owedAmountMinor: Long,
    val isSettled: Boolean = false
)

@Serializable
@Entity(
    tableName = "debts",
    indices = [Index("personId"), Index("status")]
)
data class DebtEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val personId: Long,
    val type: DebtType,
    val amountMinor: Long,
    val currency: String,
    val dueDateMillis: Long? = null,
    val status: DebtStatus = DebtStatus.OPEN,
    val note: String = "",
    val createdAtMillis: Long
)

@Serializable
@Entity(
    tableName = "repayments",
    indices = [Index("debtId")]
)
data class RepaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val debtId: Long,
    val amountMinor: Long,
    val dateMillis: Long,
    val note: String = ""
)

@Serializable
@Entity(tableName = "savings_goals")
data class SavingsGoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val targetAmountMinor: Long,
    val currentSavedMinor: Long = 0,
    val currency: String,
    val deadlineMillis: Long,
    val colorHex: String = "#10B981"
)

@Serializable
@Entity(tableName = "currency_rates")
data class CurrencyRateEntity(
    @PrimaryKey val currencyCode: String,
    val rateToBase: Double,
    val lastUpdatedMillis: Long
)

@Serializable
@Entity(tableName = "saved_filters")
data class SavedFilterEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val categoryId: Long? = null,
    val tag: String? = null,
    val minAmountMinor: Long? = null,
    val maxAmountMinor: Long? = null,
    val startDateMillis: Long? = null,
    val endDateMillis: Long? = null
)
