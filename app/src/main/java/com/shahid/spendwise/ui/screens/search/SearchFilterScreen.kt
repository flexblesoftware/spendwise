package com.shahid.spendwise.ui.screens.search

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shahid.spendwise.ui.components.EmptyStateView
import com.shahid.spendwise.ui.components.TransactionItemCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchFilterScreen(
    viewModel: SearchViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToEditTransaction: (Long) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showSaveFilterDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Smart Search & Filters") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.clearFilters() }, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Default.FilterAltOff, contentDescription = "Clear Filters")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                OutlinedTextField(
                    value = uiState.query,
                    onValueChange = { viewModel.setQuery(it) },
                    placeholder = { Text("Search notes, tags, categories...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (uiState.query.isNotBlank()) {
                            IconButton(onClick = { viewModel.setQuery("") }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear Search")
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
            }

            if (uiState.savedFilters.isNotEmpty()) {
                item {
                    Text("Saved Filters", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                        items(uiState.savedFilters, key = { it.id }) { filter ->
                            FilterChip(
                                selected = false,
                                onClick = { viewModel.applySavedFilter(filter) },
                                label = { Text(filter.name) },
                                trailingIcon = {
                                    IconButton(
                                        onClick = { viewModel.deleteFilter(filter) },
                                        modifier = Modifier.size(16.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Delete Filter")
                                    }
                                }
                            )
                        }
                    }
                }
            }

            if (uiState.allTags.isNotEmpty()) {
                item {
                    Text("Filter by Tag", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp)) {
                        items(uiState.allTags) { tag ->
                            val isSelected = uiState.selectedTag == tag
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.setTag(if (isSelected) null else tag) },
                                label = { Text("#$tag") }
                            )
                        }
                    }
                }
            }

            item {
                Text("Filter by Category", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp)) {
                    items(uiState.categories, key = { it.id }) { cat ->
                        val isSelected = uiState.selectedCategoryId == cat.id
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.setCategory(if (isSelected) null else cat.id) },
                            label = { Text(cat.name) }
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("${uiState.results.size} Results Found", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    if (uiState.query.isNotBlank() || uiState.selectedCategoryId != null || uiState.selectedTag != null) {
                        TextButton(onClick = { showSaveFilterDialog = true }) {
                            Icon(Icons.Default.BookmarkAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Save Filter")
                        }
                    }
                }
            }

            if (uiState.results.isEmpty()) {
                item {
                    EmptyStateView(
                        icon = Icons.Default.SearchOff,
                        title = "No Matches Found",
                        description = "Try adjusting your search terms, removing filters, or clearing the query."
                    )
                }
            } else {
                items(uiState.results, key = { it.id }) { tx ->
                    TransactionItemCard(
                        transaction = tx,
                        category = uiState.categoryMap[tx.categoryId],
                        onClick = { onNavigateToEditTransaction(tx.id) }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(64.dp)) }
        }

        if (showSaveFilterDialog) {
            var filterName by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { showSaveFilterDialog = false },
                title = { Text("Save Current Filter") },
                text = {
                    OutlinedTextField(
                        value = filterName,
                        onValueChange = { filterName = it },
                        label = { Text("Filter Name (e.g. Work Expenses)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (filterName.isNotBlank()) {
                                viewModel.saveCurrentFilter(filterName)
                                showSaveFilterDialog = false
                            }
                        }
                    ) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showSaveFilterDialog = false }) { Text("Cancel") }
                }
            )
        }
    }
}
