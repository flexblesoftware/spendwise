package com.shahid.spendwise.ui.screens.backup

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shahid.spendwise.BuildConfig
import com.shahid.spendwise.data.local.entity.CategoryEntity
import com.shahid.spendwise.data.repository.PreferencesRepository
import com.shahid.spendwise.data.repository.SpendWiseRepository
import com.shahid.spendwise.domain.usecase.ExportImportHelper
import com.shahid.spendwise.domain.usecase.SpendWiseBackupData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

data class CsvPreviewData(
    val headers: List<String>,
    val sampleRow: List<String>,
    val rawContent: String,
    val initialAmountCol: Int = 0,
    val initialCategoryCol: Int = 1,
    val initialDateCol: Int = 2,
    val initialNoteCol: Int = 3,
    val initialTypeCol: Int = 4
)

data class BackupUiState(
    val isExporting: Boolean = false,
    val isImporting: Boolean = false,
    val statusMessage: String? = null,
    val isSuccess: Boolean = false,
    val baseCurrency: String = "USD",
    val csvPreview: CsvPreviewData? = null
)

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val repository: SpendWiseRepository,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BackupUiState())
    val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            preferencesRepository.baseCurrency.collect { curr ->
                _uiState.update { it.copy(baseCurrency = curr) }
            }
        }
    }

    fun exportZipBackup(context: Context, destinationUri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true, statusMessage = "Creating backup archive...") }
            withContext(Dispatchers.IO) {
                try {
                    val txs = repository.getTransactions(includeLocked = true).firstOrNull() ?: emptyList()
                    val categories = repository.getAllCategories().firstOrNull() ?: emptyList()
                    val budgets = repository.getBudgetsForMonth(SimpleDateFormat("yyyy-MM", Locale.US).format(Date())).firstOrNull() ?: emptyList()
                    val recurring = repository.getAllRecurring().firstOrNull() ?: emptyList()
                    val people = repository.getAllPeople().firstOrNull() ?: emptyList()
                    val splits = repository.getAllSplits().firstOrNull() ?: emptyList()
                    val participants = repository.getAllParticipants().firstOrNull() ?: emptyList()
                    val debts = repository.getAllDebts().firstOrNull() ?: emptyList()
                    val goals = repository.getAllGoals().firstOrNull() ?: emptyList()
                    val currencies = repository.getAllRates().firstOrNull() ?: emptyList()
                    val filters = repository.getAllFilters().firstOrNull() ?: emptyList()

                    val backupData = SpendWiseBackupData(
                        exportDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date()),
                        appVersion = BuildConfig.VERSION_NAME,
                        transactions = txs,
                        categories = categories,
                        budgets = budgets,
                        recurring = recurring,
                        people = people,
                        splits = splits,
                        splitParticipants = participants,
                        debts = debts,
                        repayments = emptyList(),
                        goals = goals,
                        currencies = currencies,
                        filters = filters
                    )

                    context.contentResolver.openOutputStream(destinationUri)?.use { out ->
                        ExportImportHelper.createZipBackup(backupData, out)
                    }

                    _uiState.update { it.copy(isExporting = false, statusMessage = "Backup ZIP saved successfully!", isSuccess = true) }
                } catch (e: Exception) {
                    _uiState.update { it.copy(isExporting = false, statusMessage = "Export failed: ${e.message}", isSuccess = false) }
                }
            }
        }
    }

    fun restoreZipBackup(context: Context, sourceUri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isImporting = true, statusMessage = "Restoring backup archive...") }
            withContext(Dispatchers.IO) {
                try {
                    val backupData = context.contentResolver.openInputStream(sourceUri)?.use { input ->
                        ExportImportHelper.parseBackup(input)
                    } ?: throw IllegalStateException("Unable to read backup file")

                    // Restore transactions and categories
                    repository.insertAllTransactions(backupData.transactions)
                    for (cat in backupData.categories) {
                        repository.insertCategory(cat)
                    }
                    for (b in backupData.budgets) {
                        repository.saveBudget(b)
                    }
                    for (g in backupData.goals) {
                        repository.insertGoal(g)
                    }

                    _uiState.update { it.copy(isImporting = false, statusMessage = "Data restored successfully!", isSuccess = true) }
                } catch (e: Exception) {
                    _uiState.update { it.copy(isImporting = false, statusMessage = "Restore failed: ${e.message}", isSuccess = false) }
                }
            }
        }
    }

    fun exportCsv(context: Context, destinationUri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val txs = repository.getTransactions(includeLocked = false).firstOrNull() ?: emptyList()
                val categories = repository.getAllCategories().firstOrNull() ?: emptyList()
                val catMap = categories.associate { it.id to it.name }
                val csv = ExportImportHelper.exportToCsv(txs, catMap)

                context.contentResolver.openOutputStream(destinationUri)?.use { out ->
                    out.write(csv.toByteArray(Charsets.UTF_8))
                }
                _uiState.update { it.copy(statusMessage = "CSV exported successfully!", isSuccess = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(statusMessage = "CSV export failed: ${e.message}", isSuccess = false) }
            }
        }
    }

    fun exportPdfReport(context: Context, destinationUri: Uri, isTaxOnly: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val txs = if (isTaxOnly) {
                    repository.getTaxRelevantTransactions().firstOrNull() ?: emptyList()
                } else {
                    repository.getTransactions(includeLocked = false).firstOrNull() ?: emptyList()
                }
                val categories = repository.getAllCategories().firstOrNull() ?: emptyList()
                val catMap = categories.associate { it.id to it.name }
                val title = if (isTaxOnly) "Tax Deductible Expenses Report" else "Monthly Financial Summary"

                context.contentResolver.openOutputStream(destinationUri)?.use { out ->
                    ExportImportHelper.generatePdfReport(
                        title = title,
                        transactions = txs,
                        categoryMap = catMap,
                        baseCurrency = _uiState.value.baseCurrency,
                        outputStream = out
                    )
                }
                _uiState.update { it.copy(statusMessage = "PDF report generated successfully!", isSuccess = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(statusMessage = "PDF generation failed: ${e.message}", isSuccess = false) }
            }
        }
    }

    fun onCsvFileSelected(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val content = context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText()
                    ?: throw IllegalStateException("Empty CSV file")

                val lines = content.lines().filter { it.isNotBlank() }
                if (lines.isEmpty()) {
                    _uiState.update { it.copy(statusMessage = "CSV file is empty", isSuccess = false) }
                    return@launch
                }

                val headerParts = lines[0].split(",").map { it.trim().trim('"') }
                val sampleParts = if (lines.size > 1) lines[1].split(",").map { it.trim().trim('"') } else emptyList()

                // Smart auto-detect column indices by header name
                var amtCol = 0
                var catCol = 1.coerceAtMost(headerParts.size - 1)
                var dateCol = 2.coerceAtMost(headerParts.size - 1)
                var noteCol = 3.coerceAtMost(headerParts.size - 1)
                var typeCol = 4.coerceAtMost(headerParts.size - 1)

                headerParts.forEachIndexed { idx, h ->
                    val lower = h.lowercase()
                    when {
                        lower.contains("amount") -> amtCol = idx
                        lower.contains("category") -> catCol = idx
                        lower.contains("date") || lower.contains("time") -> dateCol = idx
                        lower.contains("note") || lower.contains("desc") -> noteCol = idx
                        lower.contains("type") -> typeCol = idx
                    }
                }

                val preview = CsvPreviewData(
                    headers = headerParts,
                    sampleRow = sampleParts,
                    rawContent = content,
                    initialAmountCol = amtCol,
                    initialCategoryCol = catCol,
                    initialDateCol = dateCol,
                    initialNoteCol = noteCol,
                    initialTypeCol = typeCol
                )
                _uiState.update { it.copy(csvPreview = preview) }
            } catch (e: Exception) {
                _uiState.update { it.copy(statusMessage = "Failed to open CSV: ${e.message}", isSuccess = false) }
            }
        }
    }

    fun dismissCsvDialog() {
        _uiState.update { it.copy(csvPreview = null) }
    }

    fun executeCsvImport(
        amountCol: Int,
        categoryCol: Int,
        dateCol: Int,
        noteCol: Int,
        typeCol: Int
    ) {
        val preview = _uiState.value.csvPreview ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _uiState.update { it.copy(isImporting = true, statusMessage = "Importing CSV transactions...") }

                val categories = repository.getAllCategories().firstOrNull() ?: emptyList()
                val existingTxs = repository.getTransactions(includeLocked = true).firstOrNull() ?: emptyList()
                val baseCurr = _uiState.value.baseCurrency

                val result = ExportImportHelper.parseCsvWithDuplicateDetection(
                    csvContent = preview.rawContent,
                    amountCol = amountCol,
                    categoryCol = categoryCol,
                    dateCol = dateCol,
                    noteCol = noteCol,
                    typeCol = typeCol,
                    categories = categories,
                    existingTransactions = existingTxs,
                    currency = baseCurr
                )

                if (result.newTransactions.isNotEmpty()) {
                    repository.insertAllTransactions(result.newTransactions)
                }

                val summary = if (result.duplicatesSkippedCount > 0) {
                    "Imported ${result.importedCount} transactions successfully (${result.duplicatesSkippedCount} duplicates skipped)!"
                } else {
                    "Imported all ${result.importedCount} transactions successfully!"
                }

                _uiState.update {
                    it.copy(
                        isImporting = false,
                        csvPreview = null,
                        statusMessage = summary,
                        isSuccess = true
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isImporting = false,
                        csvPreview = null,
                        statusMessage = "CSV Import failed: ${e.message}",
                        isSuccess = false
                    )
                }
            }
        }
    }

    fun clearStatus() {
        _uiState.update { it.copy(statusMessage = null) }
    }
}
