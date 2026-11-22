package com.shahid.spendwise.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shahid.spendwise.data.local.entity.CategoryEntity
import com.shahid.spendwise.data.local.entity.SavedFilterEntity
import com.shahid.spendwise.data.local.entity.TransactionEntity
import com.shahid.spendwise.data.repository.SpendWiseRepository
import com.shahid.spendwise.domain.model.Money
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchFilterUiState(
    val query: String = "",
    val selectedCategoryId: Long? = null,
    val selectedTag: String? = null,
    val minAmountStr: String = "",
    val maxAmountStr: String = "",
    val results: List<TransactionEntity> = emptyList(),
    val allTags: List<String> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val categoryMap: Map<Long, CategoryEntity> = emptyMap(),
    val savedFilters: List<SavedFilterEntity> = emptyList(),
    val isLoading: Boolean = false
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: SpendWiseRepository
) : ViewModel() {

    private val _query = MutableStateFlow("")
    private val _selectedCategoryId = MutableStateFlow<Long?>(null)
    private val _selectedTag = MutableStateFlow<String?>(null)
    private val _minAmountMinor = MutableStateFlow<Long?>(null)
    private val _maxAmountMinor = MutableStateFlow<Long?>(null)

    val uiState: StateFlow<SearchFilterUiState> = combine(
        repository.getTransactions(includeLocked = false),
        repository.getAllCategories(),
        repository.getAllFilters(),
        _query,
        _selectedCategoryId,
        _selectedTag,
        _minAmountMinor
    ) { txs, categories, savedFilters, query, catId, tag, minAmount ->
        val catMap = categories.associateBy { it.id }
        val maxAmount = _maxAmountMinor.value
        val uniqueTags = txs.flatMap { it.tags }.distinct().sorted()

        val filtered = txs.filter { tx ->
            val matchesQuery = query.isBlank() ||
                    tx.note.contains(query, ignoreCase = true) ||
                    (catMap[tx.categoryId]?.name ?: "").contains(query, ignoreCase = true) ||
                    tx.tags.any { it.contains(query, ignoreCase = true) }

            val matchesCat = catId == null || tx.categoryId == catId
            val matchesTag = tag == null || tx.tags.contains(tag)
            val matchesMin = minAmount == null || tx.amountMinor >= minAmount
            val matchesMax = maxAmount == null || tx.amountMinor <= maxAmount

            matchesQuery && matchesCat && matchesTag && matchesMin && matchesMax
        }

        SearchFilterUiState(
            query = query,
            selectedCategoryId = catId,
            selectedTag = tag,
            results = filtered,
            allTags = uniqueTags,
            categories = categories,
            categoryMap = catMap,
            savedFilters = savedFilters
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SearchFilterUiState())

    fun setQuery(q: String) { _query.value = q }
    fun setCategory(catId: Long?) { _selectedCategoryId.value = catId }
    fun setTag(tag: String?) { _selectedTag.value = tag }

    fun setAmountRange(minStr: String, maxStr: String) {
        val minMinor = if (minStr.isNotBlank()) Money.parseMajor(minStr).amountMinor else null
        val maxMinor = if (maxStr.isNotBlank()) Money.parseMajor(maxStr).amountMinor else null
        _minAmountMinor.value = minMinor
        _maxAmountMinor.value = maxMinor
    }

    fun saveCurrentFilter(name: String) {
        viewModelScope.launch {
            val filter = SavedFilterEntity(
                name = name.trim(),
                categoryId = _selectedCategoryId.value,
                tag = _selectedTag.value,
                minAmountMinor = _minAmountMinor.value,
                maxAmountMinor = _maxAmountMinor.value
            )
            repository.insertFilter(filter)
        }
    }

    fun applySavedFilter(filter: SavedFilterEntity) {
        _selectedCategoryId.value = filter.categoryId
        _selectedTag.value = filter.tag
        _minAmountMinor.value = filter.minAmountMinor
        _maxAmountMinor.value = filter.maxAmountMinor
    }

    fun deleteFilter(filter: SavedFilterEntity) {
        viewModelScope.launch {
            repository.deleteFilter(filter)
        }
    }

    fun clearFilters() {
        _query.value = ""
        _selectedCategoryId.value = null
        _selectedTag.value = null
        _minAmountMinor.value = null
        _maxAmountMinor.value = null
    }
}
