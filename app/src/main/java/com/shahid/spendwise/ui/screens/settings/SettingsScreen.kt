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
import com.shahid.spendwise.ui.theme.PrimaryEmerald
import com.shahid.spendwise.update.UpdateUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToBackup: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val updateState by viewModel.updateState.collectAsState()
    val userSession by viewModel.userSession.collectAsState()
    val context = LocalContext.current
    val activity = context as? FragmentActivity

    var showCurrencyDialog by remember { mutableStateOf(false) }
    var editingCurrencyCode by remember { mutableStateOf<String?>(null) }
    var showAccountDialog by remember { mutableStateOf(false) }

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
            // Account & Encrypted Keystore Session Section
            item {
                Text("Account & Security Session", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
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
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(24.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.AccountCircle,
                                        contentDescription = "Profile",
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = userSession?.displayName ?: "MD Shahid",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = userSession?.email ?: "shahiidkhan108@gmail.com",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            SuggestionChip(
                                onClick = { showAccountDialog = true },
                                label = { Text("Manage") }
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.VerifiedUser,
                                contentDescription = null,
                                tint = PrimaryEmerald,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Keystore Encrypted • Survives App Updates & Restarts",
                                style = MaterialTheme.typography.labelSmall,
                                color = PrimaryEmerald,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // Theme Section
            item {
                Text("Appearance & Theme", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("App Theme", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                        Text("Choose between system default, light, or dark mode", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(12.dp))
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            listOf("SYSTEM" to "System", "LIGHT" to "Light", "DARK" to "Dark").forEachIndexed { index, (mode, label) ->
                                SegmentedButton(
                                    selected = uiState.themeMode == mode,
                                    onClick = { viewModel.setThemeMode(mode) },
                                    shape = SegmentedButtonDefaults.itemShape(index = index, count = 3)
                                ) {
                                    Text(label, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }
                }
            }

            // Security & Biometric Lock Section
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
                                            onError = { /* failed */ }
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

            // Currency & Conversion Section
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
                        Text("Offline Exchange Rates (1 [Currency] = [Rate] ${uiState.baseCurrency}):", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
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

            // Backup & Data
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

            // Updates & App Info
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
                            Column(modifier = Modifier.weight(1f)) {
                                Text("SpendWise v${BuildConfig.VERSION_NAME}", fontWeight = FontWeight.Bold)
                                Text("Build Code: ${BuildConfig.VERSION_CODE} • Offline First", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Button(
                                onClick = { viewModel.checkForUpdates() },
                                shape = RoundedCornerShape(8.dp),
                                enabled = updateState !is UpdateUiState.Checking
                            ) {
                                if (updateState is UpdateUiState.Checking) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Checking...")
                                } else {
                                    Icon(Icons.Default.SystemUpdate, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Check for Updates")
                                }
                            }
                        }

                        // Live Update Feedback Section
                        when (val state = updateState) {
                            is UpdateUiState.Checking -> {
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Connecting to GitHub Releases...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                }
                            }

                            is UpdateUiState.UpToDate -> {
                                Spacer(modifier = Modifier.height(12.dp))
                                Surface(
                                    color = PrimaryEmerald.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = PrimaryEmerald, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text("SpendWise is up to date!", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = PrimaryEmerald)
                                            Text("Latest public release: ${state.latestTag}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }

                            is UpdateUiState.UpdateRequired -> {
                                Spacer(modifier = Modifier.height(12.dp))
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Update Available: ${state.release.tagName}!", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Text("New release ready to download and install", fontSize = 11.sp)
                                        }
                                        Button(
                                            onClick = { viewModel.updateManager.startDownload() },
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                        ) {
                                            Text("Update Now", fontSize = 12.sp)
                                        }
                                    }
                                }
                            }

                            is UpdateUiState.Error -> {
                                Spacer(modifier = Modifier.height(12.dp))
                                Surface(
                                    color = MaterialTheme.colorScheme.errorContainer,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Update Check Failed", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = MaterialTheme.colorScheme.error)
                                            Text(state.message, fontSize = 11.sp, color = MaterialTheme.colorScheme.onErrorContainer)
                                        }
                                        TextButton(onClick = { viewModel.checkForUpdates() }) {
                                            Text("Retry", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }

                            else -> Unit
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(64.dp)) }
        }

        // Base Currency Picker Dialog
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

        // Edit Exchange Rate Dialog
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

        if (showAccountDialog) {
            var inputName by remember { mutableStateOf(userSession?.displayName ?: "MD Shahid") }
            var inputEmail by remember { mutableStateOf(userSession?.email ?: "shahiidkhan108@gmail.com") }

            AlertDialog(
                onDismissRequest = { showAccountDialog = false },
                title = { Text("Manage Account & Session") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Your account session is protected by Android Keystore hardware-backed encryption. Tokens and data persist seamlessly across app restarts and in-app APK updates.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedTextField(
                            value = inputName,
                            onValueChange = { inputName = it },
                            label = { Text("Display Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = inputEmail,
                            onValueChange = { inputEmail = it },
                            label = { Text("Email Address") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    viewModel.loginWithGoogle("MD Shahid", "shahiidkhan108@gmail.com")
                                    showAccountDialog = false
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Link Google", fontSize = 12.sp)
                            }
                            OutlinedButton(
                                onClick = {
                                    viewModel.logout()
                                    showAccountDialog = false
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Sign Out", fontSize = 12.sp)
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.loginWithEmail(inputEmail, inputName)
                            showAccountDialog = false
                        }
                    ) {
                        Text("Save & Persist")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAccountDialog = false }) { Text("Cancel") }
                }
            )
        }
    }
}
