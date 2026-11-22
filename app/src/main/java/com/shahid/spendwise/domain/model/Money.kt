package com.shahid.spendwise.domain.model

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.roundToLong

enum class TransactionType {
    EXPENSE,
    INCOME
}

enum class RecurrenceFrequency {
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY
}

enum class DebtType {
    LENT,
    BORROWED
}

enum class DebtStatus {
    OPEN,
    PARTIALLY_PAID,
    SETTLED
}

enum class SplitType {
    EQUAL,
    EXACT_AMOUNT,
    PERCENTAGE
}

data class Money(
    val amountMinor: Long,
    val currency: String = DEFAULT_CURRENCY
) {
    companion object {
        const val DEFAULT_CURRENCY = "USD"

        fun fromMajor(major: Double, currency: String = DEFAULT_CURRENCY): Money {
            val minor = (major * 100.0).roundToLong()
            return Money(minor, currency)
        }

        fun parseMajor(majorStr: String, currency: String = DEFAULT_CURRENCY): Money {
            val cleaned = majorStr.replace(",", "").trim()
            val value = cleaned.toDoubleOrNull() ?: 0.0
            return fromMajor(value, currency)
        }

        fun getCurrencySymbol(currencyCode: String): String {
            return when (currencyCode.uppercase()) {
                "USD" -> "$"
                "INR" -> "₹"
                "EUR" -> "€"
                "GBP" -> "£"
                "JPY" -> "¥"
                "CAD" -> "CA$"
                "AUD" -> "A$"
                else -> currencyCode
            }
        }

        fun convertMinor(amountMinor: Long, rate: Double): Long {
            return (amountMinor * rate).roundToLong()
        }
    }

    fun toMajor(): Double = amountMinor / 100.0

    fun format(includeSymbol: Boolean = true): String {
        val symbols = DecimalFormatSymbols(Locale.US)
        val df = DecimalFormat("#,##0.00", symbols)
        val formattedNumber = df.format(toMajor())
        return if (includeSymbol) {
            "${getCurrencySymbol(currency)} $formattedNumber"
        } else {
            formattedNumber
        }
    }

    operator fun plus(other: Money): Money {
        require(currency == other.currency) { "Currencies must match: $currency vs ${other.currency}" }
        return Money(amountMinor + other.amountMinor, currency)
    }

    operator fun minus(other: Money): Money {
        require(currency == other.currency) { "Currencies must match: $currency vs ${other.currency}" }
        return Money(amountMinor - other.amountMinor, currency)
    }
}
