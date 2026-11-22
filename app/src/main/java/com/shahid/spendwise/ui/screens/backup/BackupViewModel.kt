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

data class BackupUiState(
    val isExporting: Boolean = false,
    val isImporting: Boolean = false,
    val statusMessage: String? = null,
    val isSuccess: Boolean = false,
    val baseCurrency: String = "USD"
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

    fun clearStatus() {
        _uiState.update { it.copy(statusMessage = null) }
    }
}
