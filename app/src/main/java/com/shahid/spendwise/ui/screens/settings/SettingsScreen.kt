package com.shahid.spendwise.ui.screens.settings

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.shahid.spendwise.BuildConfig
import com.shahid.spendwise.security.BiometricHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToBackup: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val activity = context as? FragmentActivity

    var showCurrencyDialog by remember { mutableStateOf(false) }
    var editingCurrencyCode by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings & Preferences") },
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
            item {
                Text("Security & Privacy", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Biometric App Lock", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                                Text("Require Fingerprint / Device PIN on app launch", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = uiState.isBiometricEnabled,
                                onCheckedChange = { enabled ->
                                    if (enabled && activity != null) {
                                        BiometricHelper.showBiometricPrompt(
                                            activity = activity,
                                            title = "Enable Biometric Lock",
                                            subtitle = "Authenticate to confirm biometric security",
                                            onSuccess = { viewModel.setBiometricEnabled(true) },
                                            onError = { }
                                        )
                                    } else {
                                        viewModel.setBiometricEnabled(false)
                                    }
                                }
                            )
                        }

                        if (uiState.isBiometricEnabled) {
                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(12.dp))

                            Text("Lock Timeout:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(0 to "Immediate", 1 to "1 Min", 5 to "5 Min", 15 to "15 Min").forEach { (min, label) ->
                                    FilterChip(
                                        selected = uiState.biometricTimeoutMinutes == min,
                                        onClick = { viewModel.setBiometricTimeoutMinutes(min) },
                                        label = { Text(label, fontSize = 12.sp) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Text("Currency & Rates", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Base Currency", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                                Text("Totals and budgets are displayed in this currency", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            AssistChip(
                                onClick = { showCurrencyDialog = true },
                                label = { Text(uiState.baseCurrency, fontWeight = FontWeight.Bold) },
                                leadingIcon = { Icon(Icons.Default.CurrencyExchange, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Offline Exchange Rates (1 [Currency] = [Rate] ${uiState.baseCurrency}):", style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.height(6.dp))

                        uiState.currencyRates.filter { it.currencyCode != uiState.baseCurrency }.forEach { rate ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("1 ${rate.currencyCode} = ${rate.rateToBase} ${uiState.baseCurrency}", style = MaterialTheme.typography.bodyMedium)
                                TextButton(onClick = { editingCurrencyCode = rate.currencyCode }) {
                                    Text("Edit Rate")
                                }
                            }
                        }
                    }
                }
            }

            item {
                Text("Data Management", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            item {
                OutlinedButton(
                    onClick = onNavigateToBackup,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Backup, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Backup, CSV & PDF Export")
                }
            }

            item {
                Text("App Updates & Information", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("SpendWise v${BuildConfig.VERSION_NAME}", fontWeight = FontWeight.Bold)
                                Text("Offline-First Android Architecture", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Button(
                                onClick = { viewModel.checkForUpdates() },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.SystemUpdate, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Check for Updates")
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(64.dp)) }
        }

        if (showCurrencyDialog) {
            val currencies = listOf("USD", "EUR", "GBP", "INR", "JPY", "CAD", "AUD")
            AlertDialog(
                onDismissRequest = { showCurrencyDialog = false },
                title = { Text("Select Base Currency") },
                text = {
                    LazyColumn {
                        items(currencies) { curr ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                RadioButton(selected = uiState.baseCurrency == curr, onClick = {
                                    viewModel.setBaseCurrency(curr)
                                    showCurrencyDialog = false
                                })
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(curr, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showCurrencyDialog = false }) { Text("Close") }
                }
            )
        }

        editingCurrencyCode?.let { code ->
            var rateStr by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { editingCurrencyCode = null },
                title = { Text("Set Exchange Rate for $code") },
                text = {
                    OutlinedTextField(
                        value = rateStr,
                        onValueChange = { rateStr = it },
                        label = { Text("1 $code in ${uiState.baseCurrency}") },
                        placeholder = { Text("e.g. 1.08") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val r = rateStr.toDoubleOrNull()
                            if (r != null && r > 0) {
                                viewModel.updateExchangeRate(code, r)
                                editingCurrencyCode = null
                            }
                        }
                    ) {
                        Text("Save Rate")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { editingCurrencyCode = null }) { Text("Cancel") }
                }
            )
        }
    }
}
