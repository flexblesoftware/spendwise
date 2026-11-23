package com.shahid.spendwise.domain.usecase

import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.shahid.spendwise.data.local.entity.*
import com.shahid.spendwise.domain.model.Money
import com.shahid.spendwise.domain.model.TransactionType
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

@Serializable
data class SpendWiseBackupData(
    val exportDate: String,
    val appVersion: String,
    val transactions: List<TransactionEntity>,
    val categories: List<CategoryEntity>,
    val budgets: List<BudgetEntity>,
    val recurring: List<RecurringEntity>,
    val people: List<PersonEntity>,
    val splits: List<SplitExpenseEntity>,
    val splitParticipants: List<SplitParticipantEntity>,
    val debts: List<DebtEntity>,
    val repayments: List<RepaymentEntity>,
    val goals: List<SavingsGoalEntity>,
    val currencies: List<CurrencyRateEntity>,
    val filters: List<SavedFilterEntity>
)

data class CsvImportResult(
    val importedCount: Int,
    val duplicatesSkippedCount: Int,
    val newTransactions: List<TransactionEntity>
)

object ExportImportHelper {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
    private val fileDateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)

    /**
     * Creates a ZIP archive containing the JSON backup.
     */
    fun createZipBackup(backupData: SpendWiseBackupData, outputStream: OutputStream) {
        val jsonString = json.encodeToString(backupData)
        ZipOutputStream(BufferedOutputStream(outputStream)).use { zipOut ->
            val zipEntry = ZipEntry("spendwise_backup.json")
            zipOut.putNextEntry(zipEntry)
            zipOut.write(jsonString.toByteArray(Charsets.UTF_8))
            zipOut.closeEntry()
            zipOut.flush()
        }
    }

    /**
     * Reads a ZIP backup or raw JSON and parses SpendWiseBackupData.
     */
    fun parseBackup(inputStream: InputStream): SpendWiseBackupData {
        val pushbackStream = PushbackInputStream(inputStream, 2)
        val header = ByteArray(2)
        val read = pushbackStream.read(header)
        pushbackStream.unread(header, 0, read)

        // Check if ZIP (PK..)
        val isZip = header[0] == 0x50.toByte() && header[1] == 0x4B.toByte()

        val jsonContent = if (isZip) {
            ZipInputStream(pushbackStream).use { zipIn ->
                var entry = zipIn.nextEntry
                while (entry != null && !entry.name.endsWith(".json")) {
                    entry = zipIn.nextEntry
                }
                if (entry == null) throw IllegalArgumentException("No JSON file found in ZIP archive")
                zipIn.bufferedReader(Charsets.UTF_8).readText()
            }
        } else {
            pushbackStream.bufferedReader(Charsets.UTF_8).readText()
        }

        return json.decodeFromString(jsonContent)
    }

    /**
     * Exports transactions to CSV string.
     */
    fun exportToCsv(
        transactions: List<TransactionEntity>,
        categoryMap: Map<Long, String>
    ): String {
        val sb = StringBuilder()
        sb.append("ID,Type,AmountMajor,Currency,Category,Date,Note,Tags,TaxRelevant\n")

        for (tx in transactions) {
            val typeStr = tx.type.name
            val amountMajor = tx.amountMinor / 100.0
            val catName = (categoryMap[tx.categoryId] ?: "Unknown").replace(",", " ")
            val dateStr = dateFormat.format(Date(tx.timestampMillis))
            val noteClean = tx.note.replace(",", " ").replace("\n", " ")
            val tagsClean = tx.tags.joinToString(";").replace(",", " ")

            sb.append("${tx.id},$typeStr,$amountMajor,${tx.currency},$catName,$dateStr,$noteClean,$tagsClean,${tx.isTaxRelevant}\n")
        }
        return sb.toString()
    }

    /**
     * Parses CSV rows with mapped column indices and detects duplicates.
     */
    fun parseCsvWithDuplicateDetection(
        csvContent: String,
        amountCol: Int,
        categoryCol: Int,
        dateCol: Int,
        noteCol: Int,
        typeCol: Int,
        categories: List<CategoryEntity>,
        existingTransactions: List<TransactionEntity>,
        currency: String = "USD"
    ): CsvImportResult {
        val lines = csvContent.lines().filter { it.isNotBlank() }
        if (lines.size <= 1) return CsvImportResult(0, 0, emptyList())

        val dataLines = lines.drop(1)
        val newTransactions = mutableListOf<TransactionEntity>()
        var duplicatesCount = 0

        val catMapByName = categories.associateBy { it.name.lowercase().trim() }
        val defaultCatId = categories.firstOrNull()?.id ?: 1L

        for (line in dataLines) {
            val parts = line.split(",").map { it.trim().trim('"') }
            val maxCol = maxOf(amountCol, categoryCol, dateCol, noteCol, typeCol)
            if (parts.size <= maxCol) continue

            val amountStr = parts.getOrNull(amountCol) ?: "0"
            val amountMinor = Money.parseMajor(amountStr, currency).amountMinor
            if (amountMinor <= 0L) continue

            val catName = (parts.getOrNull(categoryCol) ?: "General").lowercase().trim()
            val categoryId = catMapByName[catName]?.id ?: defaultCatId

            val dateStr = parts.getOrNull(dateCol) ?: ""
            val note = parts.getOrNull(noteCol) ?: ""
            val typeStr = (parts.getOrNull(typeCol) ?: "EXPENSE").uppercase()

            val parsedDate = runCatching {
                dateFormat.parse(dateStr)?.time
            }.getOrNull() ?: runCatching {
                SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(dateStr)?.time
            }.getOrNull() ?: System.currentTimeMillis()

            val txType = if (typeStr.contains("INCOME")) TransactionType.INCOME else TransactionType.EXPENSE

            // Duplicate detection: same amount, type, note and within 24 hours of timestamp
            val isDuplicate = existingTransactions.any { existing ->
                existing.amountMinor == amountMinor &&
                existing.type == txType &&
                existing.note.equals(note, ignoreCase = true) &&
                kotlin.math.abs(existing.timestampMillis - parsedDate) < 86400000L
            } || newTransactions.any { existing ->
                existing.amountMinor == amountMinor &&
                existing.type == txType &&
                existing.note.equals(note, ignoreCase = true) &&
                kotlin.math.abs(existing.timestampMillis - parsedDate) < 86400000L
            }

            if (isDuplicate) {
                duplicatesCount++
            } else {
                newTransactions.add(
                    TransactionEntity(
                        type = txType,
                        amountMinor = amountMinor,
                        currency = currency,
                        categoryId = categoryId,
                        timestampMillis = parsedDate,
                        note = note,
                        tags = listOf("csv-import"),
                        isTaxRelevant = false
                    )
                )
            }
        }

        return CsvImportResult(
            importedCount = newTransactions.size,
            duplicatesSkippedCount = duplicatesCount,
            newTransactions = newTransactions
        )
    }

    /**
     * Parses CSV rows with mapped column indices.
     */
    fun parseCsv(
        csvContent: String,
        amountCol: Int,
        categoryCol: Int,
        dateCol: Int,
        noteCol: Int,
        typeCol: Int,
        categories: List<CategoryEntity>,
        currency: String = "USD"
    ): List<TransactionEntity> {
        val lines = csvContent.lines().filter { it.isNotBlank() }
        if (lines.size <= 1) return emptyList()

        val dataLines = lines.drop(1)
        val results = mutableListOf<TransactionEntity>()

        for (line in dataLines) {
            val parts = line.split(",").map { it.trim().trim('"') }
            if (parts.size <= maxOf(amountCol, categoryCol, dateCol, noteCol, typeCol)) continue

            val amountStr = parts.getOrNull(amountCol) ?: "0"
            val amountMinor = Money.parseMajor(amountStr, currency).amountMinor
            val catName = parts.getOrNull(categoryCol) ?: "General"
            val dateStr = parts.getOrNull(dateCol) ?: ""
            val note = parts.getOrNull(noteCol) ?: ""
            val typeStr = parts.getOrNull(typeCol)?.uppercase() ?: "EXPENSE"

            val matchedCat = categories.find { it.name.equals(catName, ignoreCase = true) } ?: categories.firstOrNull()
            val categoryId = matchedCat?.id ?: 1L

            val parsedDate = runCatching { dateFormat.parse(dateStr)?.time }.getOrNull()
                ?: System.currentTimeMillis()

            val txType = if (typeStr.contains("INCOME")) TransactionType.INCOME else TransactionType.EXPENSE

            results.add(
                TransactionEntity(
                    type = txType,
                    amountMinor = amountMinor,
                    currency = currency,
                    categoryId = categoryId,
                    timestampMillis = parsedDate,
                    note = note,
                    tags = emptyList(),
                    isTaxRelevant = false
                )
            )
        }
        return results
    }

    /**
     * Generates a standard PDF document for Expense / Tax Reports.
     */
    fun generatePdfReport(
        title: String,
        transactions: List<TransactionEntity>,
        categoryMap: Map<Long, String>,
        baseCurrency: String,
        outputStream: OutputStream
    ) {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 standard size
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        val paintTitle = Paint().apply {
            color = Color.DKGRAY
            textSize = 18f
            isFakeBoldText = true
        }

        val paintHeader = Paint().apply {
            color = Color.BLACK
            textSize = 11f
            isFakeBoldText = true
        }

        val paintBody = Paint().apply {
            color = Color.DKGRAY
            textSize = 10f
        }

        val paintLine = Paint().apply {
            color = Color.LTGRAY
            strokeWidth = 1f
        }

        var y = 50f
        canvas.drawText("SpendWise Financial Report", 40f, y, paintTitle)
        y += 24f
        canvas.drawText(title, 40f, y, paintHeader)
        y += 18f
        canvas.drawText("Generated on: ${dateFormat.format(Date())} | Base Currency: $baseCurrency", 40f, y, paintBody)
        y += 24f

        canvas.drawLine(40f, y, 555f, y, paintLine)
        y += 18f

        // Table Header
        canvas.drawText("Date", 40f, y, paintHeader)
        canvas.drawText("Category", 130f, y, paintHeader)
        canvas.drawText("Note", 250f, y, paintHeader)
        canvas.drawText("Amount", 480f, y, paintHeader)
        y += 10f
        canvas.drawLine(40f, y, 555f, y, paintLine)
        y += 18f

        var totalMinor = 0L

        for (tx in transactions.take(35)) { // First page rows
            val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(tx.timestampMillis))
            val catName = categoryMap[tx.categoryId] ?: "Unknown"
            val noteClean = if (tx.note.length > 28) tx.note.take(25) + "..." else tx.note
            val amountStr = Money(tx.amountMinor, tx.currency).format()

            if (tx.type == TransactionType.EXPENSE) {
                totalMinor += tx.amountMinor
            } else {
                totalMinor -= tx.amountMinor
            }

            canvas.drawText(dateStr, 40f, y, paintBody)
            canvas.drawText(catName, 130f, y, paintBody)
            canvas.drawText(noteClean, 250f, y, paintBody)
            canvas.drawText(amountStr, 480f, y, paintBody)

            y += 16f
        }

        y += 10f
        canvas.drawLine(40f, y, 555f, y, paintLine)
        y += 20f

        val totalMoney = Money(totalMinor, baseCurrency)
        canvas.drawText("Net Total: ${totalMoney.format()}", 380f, y, paintHeader)

        document.finishPage(page)
        document.writeTo(outputStream)
        document.close()
    }
}
