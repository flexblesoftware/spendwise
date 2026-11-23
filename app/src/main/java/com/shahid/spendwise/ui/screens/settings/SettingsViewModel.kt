package com.shahid.spendwise.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shahid.spendwise.data.local.entity.CurrencyRateEntity
import com.shahid.spendwise.data.repository.PreferencesRepository
import com.shahid.spendwise.data.repository.SpendWiseRepository
import com.shahid.spendwise.update.UpdateManager
import com.shahid.spendwise.update.UpdateUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val baseCurrency: String = "USD",
    val isBiometricEnabled: Boolean = false,
    val biometricTimeoutMinutes: Int = 0,
    val themeMode: String = "SYSTEM",
    val currencyRates: List<CurrencyRateEntity> = emptyList()
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    private val repository: SpendWiseRepository,
    val updateManager: UpdateManager
) : ViewModel() {

    val updateState: StateFlow<UpdateUiState> = updateManager.updateState

    val uiState: StateFlow<SettingsUiState> = combine(
        preferencesRepository.baseCurrency,
        preferencesRepository.isBiometricEnabled,
        preferencesRepository.biometricTimeoutMinutes,
        preferencesRepository.themeMode,
        repository.getAllRates()
    ) { baseCurr, bioEnabled, timeout, theme, rates ->
        SettingsUiState(
            baseCurrency = baseCurr,
            isBiometricEnabled = bioEnabled,
            biometricTimeoutMinutes = timeout,
            themeMode = theme,
            currencyRates = rates
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    fun setBaseCurrency(currency: String) {
        preferencesRepository.setBaseCurrency(currency)
    }

    fun setBiometricEnabled(enabled: Boolean) {
        preferencesRepository.setBiometricEnabled(enabled)
    }

    fun setBiometricTimeoutMinutes(minutes: Int) {
        preferencesRepository.setBiometricTimeoutMinutes(minutes)
    }

    fun setThemeMode(mode: String) {
        preferencesRepository.setThemeMode(mode)
    }

    fun updateExchangeRate(currencyCode: String, rate: Double) {
        viewModelScope.launch {
            repository.updateRate(
                CurrencyRateEntity(
                    currencyCode = currencyCode,
                    rateToBase = rate,
                    lastUpdatedMillis = System.currentTimeMillis()
                )
            )
        }
    }

    fun checkForUpdates() {
        viewModelScope.launch {
            updateManager.checkForUpdates(forceCheck = true)
        }
    }
}
