package com.shahid.spendwise.ui.screens.backup

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shahid.spendwise.ui.theme.PrimaryEmerald

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupExportScreen(
    viewModel: BackupViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // SAF Launchers
    val exportZipLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        uri?.let { viewModel.exportZipBackup(context, it) }
    }

    val restoreZipLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.restoreZipBackup(context, it) }
    }

    val exportCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let { viewModel.exportCsv(context, it) }
    }

    val importCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.onCsvFileSelected(context, it) }
    }

    val exportPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        uri?.let { viewModel.exportPdfReport(context, it, isTaxOnly = false) }
    }

    val exportTaxPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        uri?.let { viewModel.exportPdfReport(context, it, isTaxOnly = true) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Backup, Export & Tax") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Status Feedback Card
            if (uiState.statusMessage != null) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (uiState.isSuccess) PrimaryEmerald.copy(alpha = 0.15f) else MaterialTheme.colorScheme.errorContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (uiState.isSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                                contentDescription = null,
                                tint = if (uiState.isSuccess) PrimaryEmerald else MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = uiState.statusMessage!!,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { viewModel.clearStatus() }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Dismiss", modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            // Local Full Backup (SAF ZIP)
            item {
                Text("Full Database Backup & Restore", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Encrypted local JSON ZIP archive via Android Storage Access Framework (SAF). Protects all transactions, budgets, splits, and categories offline.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { exportZipLauncher.launch("SpendWise_Backup_${System.currentTimeMillis()}.zip") },
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Upload, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Export ZIP")
                    }

                    OutlinedButton(
                        onClick = { restoreZipLauncher.launch(arrayOf("application/zip", "application/octet-stream", "*/*")) },
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Restore ZIP")
                    }
                }
            }

            item { HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp)) }

            // CSV & Spreadsheet Export / Import
            item {
                Text("Spreadsheet & CSV Management", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Export transactions to spreadsheet or import external bank CSV files with custom column mapping and automatic duplicate detection.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { exportCsvLauncher.launch("SpendWise_Transactions_${System.currentTimeMillis()}.csv") },
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.TableChart, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Export CSV")
                    }

                    OutlinedButton(
                        onClick = { importCsvLauncher.launch(arrayOf("text/comma-separated-values", "text/csv", "application/csv", "*/*")) },
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.FileUpload, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Import CSV")
                    }
                }
            }

            item { HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp)) }

            // PDF Reports & Tax Summary
            item {
                Text("PDF Reports & Tax Deductions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Generate professional, printable PDF documents directly from your device using native Android PdfDocument.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = { exportPdfLauncher.launch("SpendWise_Monthly_Report_${System.currentTimeMillis()}.pdf") },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Export Monthly Statement (PDF)")
                    }

                    Button(
                        onClick = { exportTaxPdfLauncher.launch("SpendWise_Tax_Deductions_${System.currentTimeMillis()}.pdf") },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.ReceiptLong, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Export Tax-Deductible Summary (PDF)")
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(64.dp)) }
        }

        // CSV Column Mapping Dialog
        uiState.csvPreview?.let { preview ->
            var amountCol by remember { mutableStateOf(preview.initialAmountCol) }
            var categoryCol by remember { mutableStateOf(preview.initialCategoryCol) }
            var dateCol by remember { mutableStateOf(preview.initialDateCol) }
            var noteCol by remember { mutableStateOf(preview.initialNoteCol) }
            var typeCol by remember { mutableStateOf(preview.initialTypeCol) }

            AlertDialog(
                onDismissRequest = { viewModel.dismissCsvDialog() },
                title = { Text("CSV Column Mapping", fontWeight = FontWeight.Bold) },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Map the CSV columns to match transaction fields. Duplicate transactions will be detected and skipped automatically.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        ColumnMappingRow("Amount Column", amountCol, preview.headers) { amountCol = it }
                        ColumnMappingRow("Category Column", categoryCol, preview.headers) { categoryCol = it }
                        ColumnMappingRow("Date Column", dateCol, preview.headers) { dateCol = it }
                        ColumnMappingRow("Note Column", noteCol, preview.headers) { noteCol = it }
                        ColumnMappingRow("Type Column", typeCol, preview.headers) { typeCol = it }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.executeCsvImport(
                                amountCol = amountCol,
                                categoryCol = categoryCol,
                                dateCol = dateCol,
                                noteCol = noteCol,
                                typeCol = typeCol
                            )
                        }
                    ) {
                        Text("Import & Skip Duplicates")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.dismissCsvDialog() }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun ColumnMappingRow(
    label: String,
    selectedIndex: Int,
    headers: List<String>,
    onSelect: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val currentHeader = headers.getOrNull(selectedIndex) ?: "Column $selectedIndex"

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        Box {
            AssistChip(
                onClick = { expanded = true },
                label = { Text(currentHeader, fontSize = 12.sp) },
                trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp)) }
            )
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                headers.forEachIndexed { index, header ->
                    DropdownMenuItem(
                        text = { Text("[$index] $header") },
                        onClick = {
                            onSelect(index)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
