package com.shahid.spendwise.data.local.converter

import androidx.room.TypeConverter
import com.shahid.spendwise.domain.model.DebtStatus
import com.shahid.spendwise.domain.model.DebtType
import com.shahid.spendwise.domain.model.RecurrenceFrequency
import com.shahid.spendwise.domain.model.SplitType
import com.shahid.spendwise.domain.model.TransactionType
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class Converters {

    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun fromTransactionType(value: TransactionType): String = value.name

    @TypeConverter
    fun toTransactionType(value: String): TransactionType =
        runCatching { TransactionType.valueOf(value) }.getOrDefault(TransactionType.EXPENSE)

    @TypeConverter
    fun fromRecurrenceFrequency(value: RecurrenceFrequency): String = value.name

    @TypeConverter
    fun toRecurrenceFrequency(value: String): RecurrenceFrequency =
        runCatching { RecurrenceFrequency.valueOf(value) }.getOrDefault(RecurrenceFrequency.MONTHLY)

    @TypeConverter
    fun fromDebtType(value: DebtType): String = value.name

    @TypeConverter
    fun toDebtType(value: String): DebtType =
        runCatching { DebtType.valueOf(value) }.getOrDefault(DebtType.LENT)

    @TypeConverter
    fun fromDebtStatus(value: DebtStatus): String = value.name

    @TypeConverter
    fun toDebtStatus(value: String): DebtStatus =
        runCatching { DebtStatus.valueOf(value) }.getOrDefault(DebtStatus.OPEN)

    @TypeConverter
    fun fromSplitType(value: SplitType): String = value.name

    @TypeConverter
    fun toSplitType(value: String): SplitType =
        runCatching { SplitType.valueOf(value) }.getOrDefault(SplitType.EQUAL)

    @TypeConverter
    fun fromStringList(value: List<String>): String =
        json.encodeToString(value)

    @TypeConverter
    fun toStringList(value: String): List<String> =
        if (value.isBlank()) emptyList() else runCatching { json.decodeFromString<List<String>>(value) }.getOrDefault(emptyList())
}
