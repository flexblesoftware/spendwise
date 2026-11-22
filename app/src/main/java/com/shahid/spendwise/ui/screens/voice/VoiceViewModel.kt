package com.shahid.spendwise.ui.screens.voice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shahid.spendwise.data.local.entity.CategoryEntity
import com.shahid.spendwise.data.local.entity.TransactionEntity
import com.shahid.spendwise.data.repository.PreferencesRepository
import com.shahid.spendwise.data.repository.SpendWiseRepository
import com.shahid.spendwise.domain.model.Money
import com.shahid.spendwise.domain.usecase.ParsedVoiceDraft
import com.shahid.spendwise.domain.usecase.VoiceParser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class VoiceUiState(
    val isListening: Boolean = false,
    val spokenText: String = "",
    val draft: ParsedVoiceDraft? = null,
    val categories: List<CategoryEntity> = emptyList(),
    val baseCurrency: String = "USD",
    val isSaved: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class VoiceViewModel @Inject constructor(
    private val repository: SpendWiseRepository,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(VoiceUiState())
    val uiState: StateFlow<VoiceUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val baseCurrency = preferencesRepository.baseCurrency.value
            _uiState.update { it.copy(baseCurrency = baseCurrency) }

            repository.getAllCategories().collect { categories ->
                _uiState.update { it.copy(categories = categories) }
            }
        }
    }

    fun onSpeechRecognized(text: String) {
        val categories = _uiState.value.categories
        val baseCurrency = _uiState.value.baseCurrency
        val parsedDraft = VoiceParser.parse(text, categories, baseCurrency)

        _uiState.update {
            it.copy(
                isListening = false,
                spokenText = text,
                draft = parsedDraft,
                errorMessage = null
            )
        }
    }

    fun updateDraftAmount(amountStr: String) {
        val minor = Money.parseMajor(amountStr, _uiState.value.baseCurrency).amountMinor
        _uiState.value.draft?.let { d ->
            _uiState.update { it.copy(draft = d.copy(amountMinor = minor)) }
        }
    }

    fun updateDraftNote(note: String) {
        _uiState.value.draft?.let { d ->
            _uiState.update { it.copy(draft = d.copy(note = note)) }
        }
    }

    fun confirmAndSaveDraft() {
        val draft = _uiState.value.draft ?: return
        if (draft.amountMinor <= 0L) return

        viewModelScope.launch {
            val tx = TransactionEntity(
                type = draft.type,
                amountMinor = draft.amountMinor,
                currency = _uiState.value.baseCurrency,
                categoryId = draft.suggestedCategoryId,
                timestampMillis = draft.timestampMillis,
                note = draft.note,
                tags = listOf("voice-entry")
            )
            repository.insertTransaction(tx)
            _uiState.update { it.copy(isSaved = true) }
        }
    }

    fun setListening(listening: Boolean) {
        _uiState.update { it.copy(isListening = listening) }
    }

    fun setError(message: String) {
        _uiState.update { it.copy(isListening = false, errorMessage = message) }
    }
}
