package com.shahid.spendwise.ui.screens.split

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import com.shahid.spendwise.domain.model.Money
import com.shahid.spendwise.ui.components.EmptyStateView
import com.shahid.spendwise.ui.theme.PrimaryEmerald

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SplitBillsScreen(
    viewModel: SplitViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val personMap = remember(uiState.people) { uiState.people.associateBy { it.id } }

    var showAddPersonDialog by remember { mutableStateOf(false) }
    var showCreateSplitDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Split Bills & Shared Costs") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddPersonDialog = true }, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Default.PersonAdd, contentDescription = "Add Person")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (uiState.people.size >= 2) {
                        showCreateSplitDialog = true
                    } else {
                        showAddPersonDialog = true
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.GroupAdd, contentDescription = "Split Bill")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("People", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    TextButton(onClick = { showAddPersonDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Person")
                    }
                }
            }

            if (uiState.people.isEmpty()) {
                item {
                    Text("No friends or roommates added yet. Add at least two people to start splitting expenses.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(uiState.people, key = { it.id }) { person ->
                            InputChip(
                                selected = false,
                                onClick = {},
                                label = { Text(person.name) },
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            )
                        }
                    }
                }
            }

            item {
                Text("Who Owes Whom (Settle Up)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            if (uiState.whoOwesWhom.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = PrimaryEmerald)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("All balances are settled up! Nobody owes any money.")
                        }
                    }
                }
            } else {
                items(uiState.whoOwesWhom) { transfer ->
                    val fromName = personMap[transfer.fromPersonId]?.name ?: "Someone"
                    val toName = personMap[transfer.toPersonId]?.name ?: "Someone"
                    val amountStr = Money(transfer.amountMinor, uiState.baseCurrency).format()

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 2.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("$fromName owes $toName", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(amountStr, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }

            item {
                Text("Split Bills History", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            if (uiState.splits.isEmpty()) {
                item {
                    EmptyStateView(
                        icon = Icons.Default.Receipt,
                        title = "No Split Bills Yet",
                        description = "Split dinner, travel, or rent with friends. SpendWise calculates exact portions and who owes whom.",
                        actionButtonText = if (uiState.people.size >= 2) "Create Split Bill" else "Add People First",
                        onActionClick = {
                            if (uiState.people.size >= 2) showCreateSplitDialog = true else showAddPersonDialog = true
                        }
                    )
                }
            } else {
                items(uiState.splits, key = { it.id }) { split ->
                    val payerName = personMap[split.paidByPersonId]?.name ?: "Unknown"
                    val totalStr = Money(split.totalAmountMinor, split.currency).format()

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 2.dp
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(split.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(totalStr, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Paid by $payerName • Split ${split.splitType.name}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(64.dp))
            }
        }

        if (showAddPersonDialog) {
            var personName by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { showAddPersonDialog = false },
                title = { Text("Add Person") },
                text = {
                    OutlinedTextField(
                        value = personName,
                        onValueChange = { personName = it },
                        label = { Text("Name") },
                        placeholder = { Text("Friend's Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (personName.isNotBlank()) {
                                viewModel.addPerson(personName)
                                showAddPersonDialog = false
                            }
                        }
                    ) {
                        Text("Add")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddPersonDialog = false }) { Text("Cancel") }
                }
            )
        }

        if (showCreateSplitDialog) {
            var splitTitle by remember { mutableStateOf("") }
            var totalAmountStr by remember { mutableStateOf("") }
            var payerId by remember { mutableStateOf(uiState.people.firstOrNull()?.id ?: 0L) }
            val selectedParticipants = remember { mutableStateListOf<Long>().apply { addAll(uiState.people.map { it.id }) } }

            AlertDialog(
                onDismissRequest = { showCreateSplitDialog = false },
                title = { Text("Split Expense Equally") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = splitTitle,
                            onValueChange = { splitTitle = it },
                            label = { Text("Expense Title (e.g. Dinner, Airbnb)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = totalAmountStr,
                            onValueChange = { totalAmountStr = it },
                            label = { Text("Total Amount (${uiState.baseCurrency})") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text("Paid By:", style = MaterialTheme.typography.bodySmall)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(uiState.people) { person ->
                                FilterChip(
                                    selected = payerId == person.id,
                                    onClick = { payerId = person.id },
                                    label = { Text(person.name) }
                                )
                            }
                        }

                        Text("Split Among:", style = MaterialTheme.typography.bodySmall)
                        LazyColumn(modifier = Modifier.heightIn(max = 120.dp)) {
                            items(uiState.people) { person ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(
                                        checked = selectedParticipants.contains(person.id),
                                        onCheckedChange = { checked ->
                                            if (checked) selectedParticipants.add(person.id) else selectedParticipants.remove(person.id)
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(person.name)
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val minor = Money.parseMajor(totalAmountStr, uiState.baseCurrency).amountMinor
                            if (splitTitle.isNotBlank() && minor > 0 && selectedParticipants.isNotEmpty()) {
                                viewModel.createEqualSplit(
                                    title = splitTitle.trim(),
                                    totalAmountMinor = minor,
                                    paidByPersonId = payerId,
                                    participantIds = selectedParticipants.toList()
                                )
                                showCreateSplitDialog = false
                            }
                        }
                    ) {
                        Text("Create Split")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCreateSplitDialog = false }) { Text("Cancel") }
                }
            )
        }
    }
}
