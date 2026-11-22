package com.shahid.spendwise.ui.screens.transaction

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.shahid.spendwise.domain.model.Money
import com.shahid.spendwise.domain.model.TransactionType
import com.shahid.spendwise.ui.components.CategoryIconHelper
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTransactionScreen(
    viewModel: AddEditTransactionViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToCamera: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var showReceiptFullScreen by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isSaved, uiState.isDeleted) {
        if (uiState.isSaved || uiState.isDeleted) {
            onNavigateBack()
        }
    }

    val calendar = Calendar.getInstance().apply { timeInMillis = uiState.timestampMillis }
    val dateFormatted = SimpleDateFormat("EEE, MMM dd, yyyy", Locale.US).format(Date(uiState.timestampMillis))

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.id != null) "Edit Transaction" else "Add Transaction") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (uiState.id != null) {
                        IconButton(onClick = { showDeleteConfirmDialog = true }, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Transaction", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = uiState.type == TransactionType.EXPENSE,
                    onClick = { viewModel.setType(TransactionType.EXPENSE) },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                ) { Text("Expense", fontWeight = FontWeight.SemiBold) }
                SegmentedButton(
                    selected = uiState.type == TransactionType.INCOME,
                    onClick = { viewModel.setType(TransactionType.INCOME) },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                ) { Text("Income", fontWeight = FontWeight.SemiBold) }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AssistChip(
                    onClick = {
                        val nextIdx = (uiState.availableCurrencies.indexOf(uiState.currency) + 1) % uiState.availableCurrencies.size
                        viewModel.setCurrency(uiState.availableCurrencies[nextIdx])
                    },
                    label = { Text(uiState.currency, fontWeight = FontWeight.Bold) },
                    leadingIcon = { Text(Money.getCurrencySymbol(uiState.currency), fontWeight = FontWeight.Bold) },
                    modifier = Modifier.height(56.dp).padding(end = 8.dp)
                )

                OutlinedTextField(
                    value = uiState.amountMajorStr,
                    onValueChange = { viewModel.setAmount(it) },
                    label = { Text("Amount") },
                    placeholder = { Text("0.00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold)
                )
            }

            Text("Category", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            val filteredCategories = uiState.categories.filter { it.type == uiState.type }

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                items(filteredCategories, key = { it.id }) { cat ->
                    val isSelected = cat.id == uiState.selectedCategoryId
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.setCategory(cat.id) },
                        label = { Text(cat.name) },
                        leadingIcon = {
                            Icon(imageVector = CategoryIconHelper.getIcon(cat.iconName), contentDescription = null, modifier = Modifier.size(18.dp))
                        },
                        modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                    )
                }
            }

            OutlinedCard(
                onClick = {
                    DatePickerDialog(
                        context,
                        { _, year, month, day ->
                            val c = Calendar.getInstance().apply { set(year, month, day) }
                            viewModel.setDateMillis(c.timeInMillis)
                        },
                        calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH),
                        calendar.get(Calendar.DAY_OF_MONTH)
                    ).show()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CalendarToday, contentDescription = "Date")
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(text = dateFormatted, style = MaterialTheme.typography.bodyLarge)
                }
            }

            OutlinedTextField(
                value = uiState.note,
                onValueChange = { viewModel.setNote(it) },
                label = { Text("Note / Description") },
                placeholder = { Text("What was this for?") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3
            )

            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Receipt Photo", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))

                    if (!uiState.receiptPhotoPath.isNullOrBlank()) {
                        val file = File(uiState.receiptPhotoPath!!)
                        if (file.exists()) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                Box(
                                    modifier = Modifier.size(72.dp).clip(RoundedCornerShape(8.dp)).clickable { showReceiptFullScreen = true }
                                ) {
                                    AsyncImage(model = file, contentDescription = "Receipt photo preview", modifier = Modifier.fillMaxSize())
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    TextButton(onClick = { showReceiptFullScreen = true }) { Text("View Fullscreen") }
                                    TextButton(
                                        onClick = { viewModel.setReceiptPhotoPath(null) },
                                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                    ) { Text("Remove Receipt") }
                                }
                            }
                        }
                    } else {
                        Button(
                            onClick = onNavigateToCamera,
                            modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Capture Receipt with Camera")
                        }
                    }
                }
            }

            Button(
                onClick = { viewModel.save() },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = !uiState.isSaving
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Save Transaction", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        if (showReceiptFullScreen && !uiState.receiptPhotoPath.isNullOrBlank()) {
            Dialog(onDismissRequest = { showReceiptFullScreen = false }) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.9f)), contentAlignment = Alignment.Center) {
                    AsyncImage(model = File(uiState.receiptPhotoPath!!), contentDescription = "Full Receipt", modifier = Modifier.fillMaxWidth())
                    IconButton(
                        onClick = { showReceiptFullScreen = false },
                        modifier = Modifier.align(Alignment.TopEnd).padding(16.dp).size(48.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }
            }
        }

        if (showDeleteConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmDialog = false },
                title = { Text("Delete Transaction") },
                text = { Text("Are you sure you want to permanently delete this transaction?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showDeleteConfirmDialog = false
                            viewModel.delete()
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) { Text("Delete") }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirmDialog = false }) { Text("Cancel") }
                }
            )
        }
    }
}
