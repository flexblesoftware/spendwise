package com.shahid.spendwise.domain.usecase

import com.shahid.spendwise.data.local.entity.CategoryEntity
import com.shahid.spendwise.domain.model.Money
import com.shahid.spendwise.domain.model.TransactionType
import java.util.Calendar
import java.util.regex.Pattern

data class ParsedVoiceDraft(
    val amountMinor: Long,
    val suggestedCategoryId: Long,
    val type: TransactionType,
    val timestampMillis: Long,
    val note: String,
    val rawText: String
)

object VoiceParser {

    private val AMOUNT_PATTERN = Pattern.compile("(?i)(?:[$₹€£]\\s*)?(\\d+(?:\\.\\d{1,2})?)\\s*(?:rs|rupees|dollars|bucks|usd|inr)?")

    fun parse(
        spokenText: String,
        categories: List<CategoryEntity>,
        defaultCurrency: String = "USD"
    ): ParsedVoiceDraft {
        val lower = spokenText.trim().lowercase()

        var amountMinor = 0L
        val matcher = AMOUNT_PATTERN.matcher(lower)
        var noteWithoutAmount = lower

        if (matcher.find()) {
            val amountStr = matcher.group(1) ?: "0"
            amountMinor = Money.parseMajor(amountStr, defaultCurrency).amountMinor
            noteWithoutAmount = lower.removeRange(matcher.start(), matcher.end()).trim()
        }

        val calendar = Calendar.getInstance()
        if (lower.contains("yesterday")) {
            calendar.add(Calendar.DAY_OF_YEAR, -1)
            noteWithoutAmount = noteWithoutAmount.replace("yesterday", "").trim()
        } else if (lower.contains("today")) {
            noteWithoutAmount = noteWithoutAmount.replace("today", "").trim()
        }

        val cleanNote = noteWithoutAmount
            .replace(Regex("^(for|on|in|spent|paid|received)\\s+"), "")
            .replace(Regex("\\s+(for|on|in)$"), "")
            .trim()
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

        var suggestedCategoryId = 1L
        var type = TransactionType.EXPENSE

        val keywordCategoryMap = mapOf(
            "salary" to (11L to TransactionType.INCOME),
            "freelance" to (12L to TransactionType.INCOME),
            "dividend" to (13L to TransactionType.INCOME),
            "income" to (11L to TransactionType.INCOME),
            "chai" to (1L to TransactionType.EXPENSE),
            "tea" to (1L to TransactionType.EXPENSE),
            "coffee" to (1L to TransactionType.EXPENSE),
            "dinner" to (1L to TransactionType.EXPENSE),
            "lunch" to (1L to TransactionType.EXPENSE),
            "food" to (1L to TransactionType.EXPENSE),
            "grocery" to (2L to TransactionType.EXPENSE),
            "cab" to (3L to TransactionType.EXPENSE),
            "uber" to (3L to TransactionType.EXPENSE),
            "auto" to (3L to TransactionType.EXPENSE),
            "rent" to (4L to TransactionType.EXPENSE),
            "bill" to (5L to TransactionType.EXPENSE),
            "shopping" to (6L to TransactionType.EXPENSE),
            "medicine" to (7L to TransactionType.EXPENSE),
            "movie" to (8L to TransactionType.EXPENSE)
        )

        for ((keyword, target) in keywordCategoryMap) {
            if (lower.contains(keyword)) {
                val matchedCat = categories.find { it.id == target.first }
                if (matchedCat != null) {
                    suggestedCategoryId = matchedCat.id
                    type = target.second
                    break
                }
            }
        }

        return ParsedVoiceDraft(
            amountMinor = amountMinor,
            suggestedCategoryId = suggestedCategoryId,
            type = type,
            timestampMillis = calendar.timeInMillis,
            note = cleanNote.ifBlank { spokenText },
            rawText = spokenText
        )
    }
}
