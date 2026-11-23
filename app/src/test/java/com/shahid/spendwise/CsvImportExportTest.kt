package com.shahid.spendwise

import com.shahid.spendwise.data.local.entity.CategoryEntity
import com.shahid.spendwise.data.local.entity.TransactionEntity
import com.shahid.spendwise.domain.model.TransactionType
import com.shahid.spendwise.domain.usecase.ExportImportHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CsvImportExportTest {

    private val categories = listOf(
        CategoryEntity(id = 1L, name = "Food & Dining", iconName = "Restaurant", colorHex = "#10B981", type = TransactionType.EXPENSE),
        CategoryEntity(id = 2L, name = "Salary", iconName = "Work", colorHex = "#3B82F6", type = TransactionType.INCOME)
    )

    @Test
    fun testCsvExportFormat() {
        val txs = listOf(
            TransactionEntity(
                id = 101L,
                type = TransactionType.EXPENSE,
                amountMinor = 2550L,
                currency = "USD",
                categoryId = 1L,
                timestampMillis = 1700000000000L,
                note = "Lunch at cafe",
                tags = listOf("food", "work"),
                isTaxRelevant = false
            )
        )
        val catMap = mapOf(1L to "Food & Dining")

        val csv = ExportImportHelper.exportToCsv(txs, catMap)
        assertTrue(csv.contains("ID,Type,AmountMajor,Currency,Category,Date,Note,Tags,TaxRelevant"))
        assertTrue(csv.contains("101,EXPENSE,25.5,USD,Food & Dining"))
        assertTrue(csv.contains("Lunch at cafe"))
    }

    @Test
    fun testCsvImportAndDuplicateDetection() {
        val existingTxs = listOf(
            TransactionEntity(
                id = 1L,
                type = TransactionType.EXPENSE,
                amountMinor = 5000L,
                currency = "USD",
                categoryId = 1L,
                timestampMillis = 1700000000000L,
                note = "Grocery shopping"
            )
        )

        val csvInput = """
            Date,Category,Amount,Note,Type
            2023-11-14 22:13,Food & Dining,50.00,Grocery shopping,EXPENSE
            2023-11-14 22:13,Salary,3500.00,Monthly salary,INCOME
            2023-11-15 10:00,Food & Dining,12.50,Coffee,EXPENSE
        """.trimIndent()

        // Column mapping:
        // Amount = 2, Category = 1, Date = 0, Note = 3, Type = 4
        val result = ExportImportHelper.parseCsvWithDuplicateDetection(
            csvContent = csvInput,
            amountCol = 2,
            categoryCol = 1,
            dateCol = 0,
            noteCol = 3,
            typeCol = 4,
            categories = categories,
            existingTransactions = existingTxs,
            currency = "USD"
        )

        // The first row is identical to existingTx -> duplicate skipped!
        // The other two rows are new -> imported!
        assertEquals(2, result.importedCount)
        assertEquals(1, result.duplicatesSkippedCount)
        assertEquals(2, result.newTransactions.size)

        assertEquals(350000L, result.newTransactions[0].amountMinor)
        assertEquals(TransactionType.INCOME, result.newTransactions[0].type)

        assertEquals(1250L, result.newTransactions[1].amountMinor)
        assertEquals("Coffee", result.newTransactions[1].note)
    }
}
