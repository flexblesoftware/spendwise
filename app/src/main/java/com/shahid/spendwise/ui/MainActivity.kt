package com.shahid.spendwise.ui

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.navigation.compose.rememberNavController
import com.shahid.spendwise.auth.repository.AuthRepository
import com.shahid.spendwise.data.repository.PreferencesRepository
import com.shahid.spendwise.security.BiometricHelper
import com.shahid.spendwise.ui.navigation.SpendWiseNavGraph
import com.shahid.spendwise.ui.theme.SpendWiseTheme
import com.shahid.spendwise.update.UpdateManager
import com.shahid.spendwise.update.UpdateUiState
import com.shahid.spendwise.update.ui.UpdateScreen
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject
    lateinit var updateManager: UpdateManager

    @Inject
    lateinit var authRepository: AuthRepository

    @Inject
    lateinit var preferencesRepository: PreferencesRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val themeMode by preferencesRepository.themeMode.collectAsState()
            val isDark = when (themeMode) {
                "DARK" -> true
                "LIGHT" -> false
                else -> isSystemInDarkTheme()
            }

            SpendWiseTheme(darkTheme = isDark) {
                val updateState by updateManager.updateState.collectAsState()
                var isAppUnlocked by remember { mutableStateOf(!preferencesRepository.isUnlockRequired()) }

                // Silent session restore & background update check on app launch
                LaunchedEffect(Unit) {
                    authRepository.checkAndRefreshSession()
                    updateManager.checkForUpdates(forceCheck = false)
                }

                // Check update blocking condition:
                // Mandatory update blocks app if update is required, downloading, ready to install, or download errored
                val isBlockingUpdate = updateState is UpdateUiState.UpdateRequired ||
                        updateState is UpdateUiState.Downloading ||
                        updateState is UpdateUiState.ReadyToInstall ||
                        (updateState is UpdateUiState.Error && (updateState as UpdateUiState.Error).isForcedUpdateError)

                if (isBlockingUpdate) {
                    // Full Screen Blocking Update UI - rest of app is unreachable
                    UpdateScreen(
                        state = updateState,
                        onStartDownload = { updateManager.startDownload() },
                        onRetry = { updateManager.retry() }
                    )
                } else if (!isAppUnlocked) {
                    // Biometric Lock Screen on App Open
                    LockScreen(
                        onUnlockRequested = {
                            if (BiometricHelper.isBiometricAvailable(this)) {
                                BiometricHelper.showBiometricPrompt(
                                    activity = this,
                                    title = "Unlock SpendWise",
                                    subtitle = "Confirm biometric or device PIN",
                                    onSuccess = {
                                        preferencesRepository.updateLastUnlockTimestamp()
                                        isAppUnlocked = true
                                    },
                                    onError = { /* Keep locked */ }
                                )
                            } else {
                                preferencesRepository.updateLastUnlockTimestamp()
                                isAppUnlocked = true
                            }
                        }
                    )
                } else {
                    // Main App Content
                    val navController = rememberNavController()
                    SpendWiseNavGraph(navController = navController)
                }
            }
        }
    }
}

@Composable
fun LockScreen(onUnlockRequested: () -> Unit) {
    LaunchedEffect(Unit) {
        onUnlockRequested()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Fingerprint,
                contentDescription = "Unlock",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(80.dp)
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "SpendWise is Locked",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Biometric authentication is required to access your financial data.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = onUnlockRequested,
                modifier = Modifier.height(50.dp)
            ) {
                Text("Tap to Unlock")
            }
        }
    }
}
