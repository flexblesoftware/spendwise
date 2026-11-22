package com.shahid.spendwise.ui.screens.recurring

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shahid.spendwise.data.local.entity.RecurringEntity
import com.shahid.spendwise.domain.model.Money
import com.shahid.spendwise.domain.model.RecurrenceFrequency
import com.shahid.spendwise.ui.components.EmptyStateView
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringScreen(
    viewModel: RecurringViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Subscriptions & Recurring") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Default.Add, contentDescription = "Add Recurring")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Recurring Expense")
            }
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
                Text(
                    text = "Recurring Expenses & Bills",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (uiState.recurringList.isEmpty()) {
                item {
                    EmptyStateView(
                        icon = Icons.Default.Repeat,
                        title = "No Recurring Expenses",
                        description = "Add recurring subscriptions, rent, or EMI payments. SpendWise can auto-log them and notify you before due dates.",
                        actionButtonText = "Add Recurring Bill",
                        onActionClick = { showAddDialog = true }
                    )
                }
            } else {
                items(uiState.recurringList, key = { it.id }) { item ->
                    val dueDateFormatted = SimpleDateFormat("MMM dd, yyyy", Locale.US).format(Date(item.nextDueDateMillis))
                    val amountFormatted = Money(item.amountMinor, item.currency).format()

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 2.dp
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text("Next due: $dueDateFormatted", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text(amountFormatted, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AssistChip(
                                    onClick = {},
                                    label = { Text(item.frequency.name) },
                                    leadingIcon = { Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Auto-Log", style = MaterialTheme.typography.bodySmall)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Switch(
                                        checked = item.autoLog,
                                        onCheckedChange = { viewModel.toggleAutoLog(item, it) }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    IconButton(
                                        onClick = { viewModel.deleteRecurring(item) },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(64.dp))
            }
        }

        if (showAddDialog) {
            AddRecurringDialog(
                categories = uiState.categories,
                baseCurrency = uiState.baseCurrency,
                onDismiss = { showAddDialog = false },
                onConfirm = { title, amountMinor, catId, freq, autoLog, reminder ->
                    viewModel.addRecurring(
                        title = title,
                        amountMinor = amountMinor,
                        categoryId = catId,
                        frequency = freq,
                        startDateMillis = System.currentTimeMillis(),
                        autoLog = autoLog,
                        reminderEnabled = reminder
                    )
                    showAddDialog = false
                }
            )
        }
    }
}

@Composable
fun AddRecurringDialog(
    categories: List<com.shahid.spendwise.data.local.entity.CategoryEntity>,
    baseCurrency: String,
    onDismiss: () -> Unit,
    onConfirm: (String, Long, Long, RecurrenceFrequency, Boolean, Boolean) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var amountStr by remember { mutableStateOf("") }
    var selectedCatId by remember { mutableStateOf(categories.firstOrNull()?.id ?: 1L) }
    var selectedFreq by remember { mutableStateOf(RecurrenceFrequency.MONTHLY) }
    var autoLog by remember { mutableStateOf(true) }
    var reminder by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Recurring Payment") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title (e.g. Netflix, Rent, Gym)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("Amount ($baseCurrency)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Frequency:", style = MaterialTheme.typography.bodySmall)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    RecurrenceFrequency.values().forEach { freq ->
                        FilterChip(
                            selected = selectedFreq == freq,
                            onClick = { selectedFreq = freq },
                            label = { Text(freq.name.take(3), fontSize = 11.sp) }
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = autoLog, onCheckedChange = { autoLog = it })
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Auto-log to expenses on due date")
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = reminder, onCheckedChange = { reminder = it })
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Send reminder notification")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val minor = Money.parseMajor(amountStr, baseCurrency).amountMinor
                    if (title.isNotBlank() && minor > 0) {
                        onConfirm(title.trim(), minor, selectedCatId, selectedFreq, autoLog, reminder)
                    }
                }
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
