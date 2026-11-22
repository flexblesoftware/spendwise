package com.shahid.spendwise.ui.screens.transaction

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shahid.spendwise.data.local.entity.CategoryEntity
import com.shahid.spendwise.data.local.entity.TransactionEntity
import com.shahid.spendwise.data.repository.PreferencesRepository
import com.shahid.spendwise.data.repository.SpendWiseRepository
import com.shahid.spendwise.domain.model.Money
import com.shahid.spendwise.domain.model.TransactionType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class AddEditTransactionUiState(
    val id: Long? = null,
    val type: TransactionType = TransactionType.EXPENSE,
    val amountMajorStr: String = "",
    val currency: String = "USD",
    val selectedCategoryId: Long = 1L,
    val timestampMillis: Long = System.currentTimeMillis(),
    val note: String = "",
    val tags: List<String> = emptyList(),
    val tagInput: String = "",
    val receiptPhotoPath: String? = null,
    val isTaxRelevant: Boolean = false,
    val isPrivacyLocked: Boolean = false,
    val categories: List<CategoryEntity> = emptyList(),
    val availableCurrencies: List<String> = listOf("USD", "EUR", "GBP", "INR", "JPY", "CAD", "AUD"),
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val isDeleted: Boolean = false
)

@HiltViewModel
class AddEditTransactionViewModel @Inject constructor(
    private val repository: SpendWiseRepository,
    private val preferencesRepository: PreferencesRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val transactionId: Long? = savedStateHandle.get<String>("transactionId")?.toLongOrNull()

    private val _uiState = MutableStateFlow(AddEditTransactionUiState())
    val uiState: StateFlow<AddEditTransactionUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val baseCurrency = preferencesRepository.baseCurrency.value
            _uiState.update { it.copy(currency = baseCurrency) }

            repository.getAllCategories().collect { categories ->
                _uiState.update { current ->
                    current.copy(
                        categories = categories,
                        selectedCategoryId = if (categories.any { it.id == current.selectedCategoryId }) current.selectedCategoryId else categories.firstOrNull()?.id ?: 1L
                    )
                }
            }
        }

        if (transactionId != null && transactionId > 0) {
            viewModelScope.launch {
                repository.getTransactionById(transactionId).firstOrNull()?.let { tx ->
                    _uiState.update {
                        it.copy(
                            id = tx.id,
                            type = tx.type,
                            amountMajorStr = String.format(java.util.Locale.US, "%.2f", tx.amountMinor / 100.0),
                            currency = tx.currency,
                            selectedCategoryId = tx.categoryId,
                            timestampMillis = tx.timestampMillis,
                            note = tx.note,
                            tags = tx.tags,
                            receiptPhotoPath = tx.receiptPhotoPath,
                            isTaxRelevant = tx.isTaxRelevant,
                            isPrivacyLocked = tx.isPrivacyLocked
                        )
                    }
                }
            }
        }
    }

    fun setType(type: TransactionType) { _uiState.update { it.copy(type = type) } }
    fun setAmount(amountStr: String) { _uiState.update { it.copy(amountMajorStr = amountStr) } }
    fun setCurrency(currency: String) { _uiState.update { it.copy(currency = currency) } }
    fun setCategory(categoryId: Long) { _uiState.update { it.copy(selectedCategoryId = categoryId) } }
    fun setDateMillis(millis: Long) { _uiState.update { it.copy(timestampMillis = millis) } }
    fun setNote(note: String) { _uiState.update { it.copy(note = note) } }
    fun setTaxRelevant(tax: Boolean) { _uiState.update { it.copy(isTaxRelevant = tax) } }
    fun setPrivacyLocked(locked: Boolean) { _uiState.update { it.copy(isPrivacyLocked = locked) } }
    fun setReceiptPhotoPath(path: String?) { _uiState.update { it.copy(receiptPhotoPath = path) } }
    fun setTagInput(input: String) { _uiState.update { it.copy(tagInput = input) } }

    fun addTag() {
        val tag = _uiState.value.tagInput.trim().removePrefix("#")
        if (tag.isNotBlank() && !uiState.value.tags.contains(tag)) {
            _uiState.update { it.copy(tags = it.tags + tag, tagInput = "") }
        }
    }

    fun removeTag(tag: String) {
        _uiState.update { it.copy(tags = it.tags - tag) }
    }

    fun save() {
        val state = _uiState.value
        val amountMinor = Money.parseMajor(state.amountMajorStr, state.currency).amountMinor
        if (amountMinor <= 0L) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val tx = TransactionEntity(
                id = state.id ?: 0L,
                type = state.type,
                amountMinor = amountMinor,
                currency = state.currency,
                categoryId = state.selectedCategoryId,
                timestampMillis = state.timestampMillis,
                note = state.note.trim(),
                tags = state.tags,
                receiptPhotoPath = state.receiptPhotoPath,
                isTaxRelevant = state.isTaxRelevant,
                isPrivacyLocked = state.isPrivacyLocked
            )

            if (state.id != null && state.id > 0) {
                repository.updateTransaction(tx)
            } else {
                repository.insertTransaction(tx)
            }
            _uiState.update { it.copy(isSaving = false, isSaved = true) }
        }
    }

    fun delete() {
        val id = _uiState.value.id ?: return
        viewModelScope.launch {
            _uiState.value.receiptPhotoPath?.let { runCatching { File(it).delete() } }
            repository.deleteTransactionById(id)
            _uiState.update { it.copy(isDeleted = true) }
        }
    }
}
