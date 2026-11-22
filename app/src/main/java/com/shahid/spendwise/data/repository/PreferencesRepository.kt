package com.shahid.spendwise.data.repository

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("spendwise_prefs", Context.MODE_PRIVATE)

    private val _baseCurrency = MutableStateFlow(prefs.getString(KEY_BASE_CURRENCY, "USD") ?: "USD")
    val baseCurrency: StateFlow<String> = _baseCurrency.asStateFlow()

    private val _isBiometricEnabled = MutableStateFlow(prefs.getBoolean(KEY_BIOMETRIC_ENABLED, false))
    val isBiometricEnabled: StateFlow<Boolean> = _isBiometricEnabled.asStateFlow()

    private val _biometricTimeoutMinutes = MutableStateFlow(prefs.getInt(KEY_BIOMETRIC_TIMEOUT, 0))
    val biometricTimeoutMinutes: StateFlow<Int> = _biometricTimeoutMinutes.asStateFlow()

    private val _themeMode = MutableStateFlow(prefs.getString(KEY_THEME_MODE, "SYSTEM") ?: "SYSTEM")
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    fun setBaseCurrency(currency: String) {
        prefs.edit().putString(KEY_BASE_CURRENCY, currency).apply()
        _baseCurrency.value = currency
    }

    fun setBiometricEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply()
        _isBiometricEnabled.value = enabled
    }

    fun setBiometricTimeoutMinutes(minutes: Int) {
        prefs.edit().putInt(KEY_BIOMETRIC_TIMEOUT, minutes).apply()
        _biometricTimeoutMinutes.value = minutes
    }

    fun setThemeMode(mode: String) {
        prefs.edit().putString(KEY_THEME_MODE, mode).apply()
        _themeMode.value = mode
    }

    fun updateLastUnlockTimestamp() {
        prefs.edit().putLong(KEY_LAST_UNLOCK, System.currentTimeMillis()).apply()
    }

    fun isUnlockRequired(): Boolean {
        if (!_isBiometricEnabled.value) return false
        val timeoutMillis = _biometricTimeoutMinutes.value * 60 * 1000L
        if (timeoutMillis == 0L) return true
        val lastUnlock = prefs.getLong(KEY_LAST_UNLOCK, 0L)
        return (System.currentTimeMillis() - lastUnlock) > timeoutMillis
    }

    companion object {
        private const val KEY_BASE_CURRENCY = "base_currency"
        private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
        private const val KEY_BIOMETRIC_TIMEOUT = "biometric_timeout_minutes"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_LAST_UNLOCK = "last_unlock_timestamp"
    }
}
