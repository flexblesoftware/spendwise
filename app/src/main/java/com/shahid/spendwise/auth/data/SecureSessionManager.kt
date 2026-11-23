package com.shahid.spendwise.auth.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.shahid.spendwise.auth.model.AuthProvider
import com.shahid.spendwise.auth.model.UserSession
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecureSessionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val securePrefs: SharedPreferences by lazy {
        initSecurePreferences()
    }

    private fun initSecurePreferences(): SharedPreferences {
        return try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                SECURE_PREFS_FILE_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            // Fallback for hardware keystore issues in certain emulator / modified environments
            context.getSharedPreferences(FALLBACK_PREFS_FILE_NAME, Context.MODE_PRIVATE)
        }
    }

    fun saveSession(session: UserSession) {
        val serialized = json.encodeToString(session)
        securePrefs.edit()
            .putString(KEY_USER_SESSION, serialized)
            .putLong(KEY_SESSION_EXPIRES_AT, session.expiresAtMillis)
            .putString(KEY_USER_ID, session.userId)
            .apply()
    }

    fun getSession(): UserSession? {
        val serialized = securePrefs.getString(KEY_USER_SESSION, null) ?: return null
        return try {
            json.decodeFromString<UserSession>(serialized)
        } catch (e: Exception) {
            null
        }
    }

    fun updateTokens(accessToken: String, refreshToken: String, expiresAtMillis: Long) {
        val current = getSession() ?: return
        val updated = current.copy(
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresAtMillis = expiresAtMillis,
            lastActiveMillis = System.currentTimeMillis()
        )
        saveSession(updated)
    }

    fun clearSession() {
        securePrefs.edit()
            .remove(KEY_USER_SESSION)
            .remove(KEY_SESSION_EXPIRES_AT)
            .remove(KEY_USER_ID)
            .apply()
    }

    fun hasActiveSession(): Boolean {
        return securePrefs.contains(KEY_USER_SESSION)
    }

    companion object {
        private const val SECURE_PREFS_FILE_NAME = "spendwise_secure_session_v1"
        private const val FALLBACK_PREFS_FILE_NAME = "spendwise_fallback_session_v1"
        private const val KEY_USER_SESSION = "key_user_session"
        private const val KEY_SESSION_EXPIRES_AT = "key_session_expires_at"
        private const val KEY_USER_ID = "key_user_id"
    }
}
