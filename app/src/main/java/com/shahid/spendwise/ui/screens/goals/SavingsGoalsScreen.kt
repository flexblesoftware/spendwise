package com.shahid.spendwise.ui.screens.goals

import android.app.DatePickerDialog
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shahid.spendwise.data.local.entity.SavingsGoalEntity
import com.shahid.spendwise.domain.model.Money
import com.shahid.spendwise.ui.components.EmptyStateView
import com.shahid.spendwise.ui.theme.PrimaryEmerald
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavingsGoalsScreen(
    viewModel: GoalsViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }
    var activeGoalForContribution by remember { mutableStateOf<SavingsGoalEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Savings Goals") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showCreateDialog = true }, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Default.Add, contentDescription = "Add Goal")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Savings, contentDescription = "New Savings Goal")
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
                    text = "Your Financial Targets",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (uiState.goals.isEmpty()) {
                item {
                    EmptyStateView(
                        icon = Icons.Default.Savings,
                        title = "No Savings Goals Set",
                        description = "Set targets for an emergency fund, new car, dream vacation, or home down payment.",
                        actionButtonText = "Create First Goal",
                        onActionClick = { showCreateDialog = true }
                    )
                }
            } else {
                items(uiState.goals, key = { it.id }) { goal ->
                    val ratio = if (goal.targetAmountMinor > 0) goal.currentSavedMinor.toFloat() / goal.targetAmountMinor else 0f
                    val progressPercent = (ratio * 100).toInt().coerceIn(0, 100)

                    val savedFormatted = Money(goal.currentSavedMinor, goal.currency).format()
                    val targetFormatted = Money(goal.targetAmountMinor, goal.currency).format()
                    val deadlineStr = SimpleDateFormat("MMM dd, yyyy", Locale.US).format(Date(goal.deadlineMillis))

                    val daysRemaining = maxOf(0, ((goal.deadlineMillis - System.currentTimeMillis()) / (1000 * 60 * 60 * 24)).toInt())

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 2.dp
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                GoalProgressRing(
                                    progress = ratio.coerceIn(0f, 1f),
                                    percent = progressPercent
                                )

                                Spacer(modifier = Modifier.width(16.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(goal.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Saved: $savedFormatted", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = PrimaryEmerald)
                                    Text("Target: $targetFormatted", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text("Target Date: $deadlineStr ($daysRemaining days left)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { viewModel.deleteGoal(goal) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete Goal", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                }

                                Button(
                                    onClick = { activeGoalForContribution = goal },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Contribute")
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(64.dp)) }
        }

        if (showCreateDialog) {
            val context = LocalContext.current
            var goalTitle by remember { mutableStateOf("") }
            var targetAmountStr by remember { mutableStateOf("") }
            val cal = remember { Calendar.getInstance().apply { add(Calendar.MONTH, 6) } }
            var deadlineMillis by remember { mutableStateOf(cal.timeInMillis) }

            AlertDialog(
                onDismissRequest = { showCreateDialog = false },
                title = { Text("New Savings Goal") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = goalTitle,
                            onValueChange = { goalTitle = it },
                            label = { Text("Goal Title (e.g. Emergency Fund)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = targetAmountStr,
                            onValueChange = { targetAmountStr = it },
                            label = { Text("Target Amount (${uiState.baseCurrency})") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        val dateStr = SimpleDateFormat("MMM dd, yyyy", Locale.US).format(Date(deadlineMillis))
                        OutlinedCard(
                            onClick = {
                                DatePickerDialog(
                                    context,
                                    { _, y, m, d ->
                                        val c = Calendar.getInstance().apply { set(y, m, d) }
                                        deadlineMillis = c.timeInMillis
                                    },
                                    cal.get(Calendar.YEAR),
                                    cal.get(Calendar.MONTH),
                                    cal.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Target Deadline: $dateStr")
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val minor = Money.parseMajor(targetAmountStr, uiState.baseCurrency).amountMinor
                            if (goalTitle.isNotBlank() && minor > 0) {
                                viewModel.createGoal(goalTitle, minor, deadlineMillis, "#10B981")
                                showCreateDialog = false
                            }
                        }
                    ) {
                        Text("Create")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCreateDialog = false }) { Text("Cancel") }
                }
            )
        }

        activeGoalForContribution?.let { goal ->
            var contribAmountStr by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { activeGoalForContribution = null },
                title = { Text("Add Contribution to ${goal.title}") },
                text = {
                    OutlinedTextField(
                        value = contribAmountStr,
                        onValueChange = { contribAmountStr = it },
                        label = { Text("Amount (${goal.currency})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val minor = Money.parseMajor(contribAmountStr, goal.currency).amountMinor
                            if (minor > 0) {
                                viewModel.addContribution(goal, minor)
                                activeGoalForContribution = null
                            }
                        }
                    ) {
                        Text("Add")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { activeGoalForContribution = null }) { Text("Cancel") }
                }
            )
        }
    }
}

@Composable
fun GoalProgressRing(progress: Float, percent: Int) {
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val progressColor = PrimaryEmerald

    Box(
        modifier = Modifier.size(70.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(60.dp)) {
            val strokeWidth = 7.dp.toPx()
            val radius = (size.minDimension - strokeWidth) / 2
            val topLeft = Offset((size.width - radius * 2) / 2, (size.height - radius * 2) / 2)
            val arcSize = Size(radius * 2, radius * 2)

            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            drawArc(
                color = progressColor,
                startAngle = -90f,
                sweepAngle = progress * 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }

        Text(
            text = "$percent%",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
