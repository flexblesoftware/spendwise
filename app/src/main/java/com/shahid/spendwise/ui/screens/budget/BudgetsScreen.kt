package com.shahid.spendwise.ui.screens.budget

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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shahid.spendwise.domain.model.Money
import com.shahid.spendwise.ui.components.CategoryIconHelper
import com.shahid.spendwise.ui.components.EmptyStateView
import com.shahid.spendwise.ui.theme.ExpenseRed
import com.shahid.spendwise.ui.theme.PrimaryEmerald
import com.shahid.spendwise.ui.theme.WarningAmber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetsScreen(
    viewModel: BudgetsViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Monthly Budgets") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Default.Add, contentDescription = "Add Budget")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Set Budget")
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
                Text(
                    text = "Budgets for ${uiState.monthYear}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (uiState.overallBudget != null) {
                item {
                    val overall = uiState.overallBudget!!
                    val barColor = if (overall.isExceeded) ExpenseRed else if (overall.isOver80Percent) WarningAmber else PrimaryEmerald

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Overall Monthly Budget", fontWeight = FontWeight.Bold)
                                if (overall.isExceeded) {
                                    AssistChip(
                                        onClick = {},
                                        label = { Text("Exceeded!", color = ExpenseRed, fontWeight = FontWeight.Bold) },
                                        leadingIcon = { Icon(Icons.Default.Warning, contentDescription = null, tint = ExpenseRed, modifier = Modifier.size(16.dp)) }
                                    )
                                } else if (overall.isOver80Percent) {
                                    AssistChip(
                                        onClick = {},
                                        label = { Text("80%+ Used", color = WarningAmber, fontWeight = FontWeight.Bold) },
                                        leadingIcon = { Icon(Icons.Default.Warning, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(16.dp)) }
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = overall.progress,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                                    .clip(RoundedCornerShape(5.dp)),
                                color = barColor
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Spent: ${overall.spentFormatted}", style = MaterialTheme.typography.bodySmall)
                                Text("Limit: ${overall.budgetFormatted}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            item {
                Text("Category Budgets", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            }

            if (uiState.categoryBudgets.isEmpty() && uiState.overallBudget == null) {
                item {
                    EmptyStateView(
                        icon = Icons.Default.PieChart,
                        title = "No Budgets Set",
                        description = "Set category or overall spending limits to monitor your finances and avoid overspending.",
                        actionButtonText = "Set First Budget",
                        onActionClick = { showAddDialog = true }
                    )
                }
            } else {
                items(uiState.categoryBudgets, key = { it.budget.id }) { item ->
                    val barColor = if (item.isExceeded) ExpenseRed else if (item.isOver80Percent) WarningAmber else PrimaryEmerald

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
                                Icon(
                                    imageVector = CategoryIconHelper.getIcon(item.category?.iconName ?: "Category"),
                                    contentDescription = null,
                                    tint = barColor,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = item.category?.name ?: "Category",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { viewModel.deleteBudget(item.budget) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Delete Budget", modifier = Modifier.size(18.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = item.progress,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = barColor
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(item.spentFormatted, style = MaterialTheme.typography.bodySmall)
                                Text("Limit: ${item.budgetFormatted}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
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
            AddBudgetDialog(
                categories = uiState.categories,
                baseCurrency = uiState.baseCurrency,
                onDismiss = { showAddDialog = false },
                onConfirm = { catId, amountMinor ->
                    viewModel.saveBudget(catId, amountMinor)
                    showAddDialog = false
                }
            )
        }
    }
}

@Composable
fun AddBudgetDialog(
    categories: List<com.shahid.spendwise.data.local.entity.CategoryEntity>,
    baseCurrency: String,
    onDismiss: () -> Unit,
    onConfirm: (Long?, Long) -> Unit
) {
    var isOverall by remember { mutableStateOf(false) }
    var selectedCatId by remember { mutableStateOf(categories.firstOrNull()?.id ?: 1L) }
    var amountStr by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Monthly Budget") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isOverall, onCheckedChange = { isOverall = it })
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Overall Monthly Budget")
                }

                if (!isOverall) {
                    Text("Select Category:", style = MaterialTheme.typography.bodySmall)
                    LazyColumn(modifier = Modifier.heightIn(max = 140.dp)) {
                        items(categories) { cat ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                RadioButton(
                                    selected = selectedCatId == cat.id,
                                    onClick = { selectedCatId = cat.id }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(cat.name)
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("Budget Limit ($baseCurrency)") },
                    placeholder = { Text("500.00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val minor = Money.parseMajor(amountStr, baseCurrency).amountMinor
                    if (minor > 0) {
                        onConfirm(if (isOverall) null else selectedCatId, minor)
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
