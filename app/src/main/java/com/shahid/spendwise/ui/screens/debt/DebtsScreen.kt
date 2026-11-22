package com.shahid.spendwise.ui.screens.debt

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
import com.shahid.spendwise.data.local.entity.DebtEntity
import com.shahid.spendwise.domain.model.DebtStatus
import com.shahid.spendwise.domain.model.DebtType
import com.shahid.spendwise.domain.model.Money
import com.shahid.spendwise.ui.components.EmptyStateView
import com.shahid.spendwise.ui.theme.ExpenseRed
import com.shahid.spendwise.ui.theme.IncomeGreen
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtsScreen(
    viewModel: DebtsViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val personMap = remember(uiState.people) { uiState.people.associateBy { it.id } }

    var selectedTab by remember { mutableStateOf(0) }
    var showAddDialog by remember { mutableStateOf(false) }
    var activeDebtForRepayment by remember { mutableStateOf<DebtEntity?>(null) }

    val filteredDebts = uiState.debts.filter {
        if (selectedTab == 0) it.type == DebtType.LENT else it.type == DebtType.BORROWED
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lent & Borrowed Money") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Default.Add, contentDescription = "Add Debt Entry")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Record Loan")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Money Lent (Owed to You)", fontWeight = FontWeight.SemiBold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Money Borrowed (You Owe)", fontWeight = FontWeight.SemiBold) }
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { Spacer(modifier = Modifier.height(4.dp)) }

                if (filteredDebts.isEmpty()) {
                    item {
                        EmptyStateView(
                            icon = Icons.Default.Handshake,
                            title = if (selectedTab == 0) "No Money Lent" else "No Money Borrowed",
                            description = if (selectedTab == 0) "Record money you lent to friends with due dates and repayments." else "Track money you borrowed so you never miss a repayment.",
                            actionButtonText = "Record Entry",
                            onActionClick = { showAddDialog = true }
                        )
                    }
                } else {
                    items(filteredDebts, key = { it.id }) { debt ->
                        val personName = personMap[debt.personId]?.name ?: "Someone"
                        val amountFormatted = Money(debt.amountMinor, debt.currency).format()
                        val isLent = debt.type == DebtType.LENT
                        val statusColor = when (debt.status) {
                            DebtStatus.OPEN -> if (isLent) IncomeGreen else ExpenseRed
                            DebtStatus.PARTIALLY_PAID -> MaterialTheme.colorScheme.primary
                            DebtStatus.SETTLED -> MaterialTheme.colorScheme.outline
                        }

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
                                        Text(personName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                        if (debt.note.isNotBlank()) {
                                            Text(debt.note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        debt.dueDateMillis?.let { due ->
                                            val dueStr = SimpleDateFormat("MMM dd, yyyy", Locale.US).format(Date(due))
                                            Text("Due by: $dueStr", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                        }
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(amountFormatted, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = statusColor)
                                        AssistChip(
                                            onClick = {},
                                            label = { Text(debt.status.name.replace("_", " "), fontSize = 11.sp) }
                                        )
                                    }
                                }

                                if (debt.status != DebtStatus.SETTLED) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    HorizontalDivider()
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        TextButton(onClick = { activeDebtForRepayment = debt }) {
                                            Text("Add Repayment")
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Button(onClick = { viewModel.markSettled(debt) }) {
                                            Text("Mark Settled")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(64.dp)) }
            }
        }

        if (showAddDialog) {
            AddDebtDialog(
                people = uiState.people,
                initialType = if (selectedTab == 0) DebtType.LENT else DebtType.BORROWED,
                baseCurrency = uiState.baseCurrency,
                onDismiss = { showAddDialog = false },
                onConfirm = { pId, type, amountMinor, dueDate, note ->
                    viewModel.addDebt(pId, type, amountMinor, dueDate, note)
                    showAddDialog = false
                }
            )
        }

        activeDebtForRepayment?.let { debt ->
            var repayAmountStr by remember { mutableStateOf("") }
            var repayNote by remember { mutableStateOf("") }

            AlertDialog(
                onDismissRequest = { activeDebtForRepayment = null },
                title = { Text("Record Partial / Full Repayment") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Original Total: ${Money(debt.amountMinor, debt.currency).format()}")
                        OutlinedTextField(
                            value = repayAmountStr,
                            onValueChange = { repayAmountStr = it },
                            label = { Text("Repayment Amount (${debt.currency})") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = repayNote,
                            onValueChange = { repayNote = it },
                            label = { Text("Note (optional)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val minor = Money.parseMajor(repayAmountStr, debt.currency).amountMinor
                            if (minor > 0) {
                                viewModel.addRepayment(debt, minor, repayNote)
                                activeDebtForRepayment = null
                            }
                        }
                    ) {
                        Text("Save Repayment")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { activeDebtForRepayment = null }) { Text("Cancel") }
                }
            )
        }
    }
}

@Composable
fun AddDebtDialog(
    people: List<com.shahid.spendwise.data.local.entity.PersonEntity>,
    initialType: DebtType,
    baseCurrency: String,
    onDismiss: () -> Unit,
    onConfirm: (Long, DebtType, Long, Long?, String) -> Unit
) {
    var selectedPersonId by remember { mutableStateOf(people.firstOrNull()?.id ?: 0L) }
    var debtType by remember { mutableStateOf(initialType) }
    var amountStr by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record Loan / Borrow") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = debtType == DebtType.LENT,
                        onClick = { debtType = DebtType.LENT },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                    ) { Text("I Lent Money") }
                    SegmentedButton(
                        selected = debtType == DebtType.BORROWED,
                        onClick = { debtType = DebtType.BORROWED },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                    ) { Text("I Borrowed") }
                }

                if (people.isEmpty()) {
                    Text("Please add a person first from the Split Bills screen.", color = MaterialTheme.colorScheme.error)
                } else {
                    Text("Select Person:", style = MaterialTheme.typography.bodySmall)
                    LazyColumn(modifier = Modifier.heightIn(max = 120.dp)) {
                        items(people) { person ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = selectedPersonId == person.id, onClick = { selectedPersonId = person.id })
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(person.name)
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("Amount ($baseCurrency)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Reason / Note") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val minor = Money.parseMajor(amountStr, baseCurrency).amountMinor
                    if (selectedPersonId > 0 && minor > 0) {
                        onConfirm(selectedPersonId, debtType, minor, null, note)
                    }
                },
                enabled = people.isNotEmpty()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
