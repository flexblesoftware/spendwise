package com.shahid.spendwise.auth.repository

import com.shahid.spendwise.auth.data.SecureSessionManager
import com.shahid.spendwise.auth.model.AuthProvider
import com.shahid.spendwise.auth.model.UserSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val sessionManager: SecureSessionManager
) {
    private val _currentSession = MutableStateFlow<UserSession?>(sessionManager.getSession())
    val currentSession: StateFlow<UserSession?> = _currentSession.asStateFlow()

    val isAuthenticated: Boolean
        get() = _currentSession.value != null

    init {
        // Ensure default local owner profile exists if first install
        if (_currentSession.value == null) {
            val initialSession = UserSession(
                userId = "user_shahid_default",
                displayName = "MD Shahid",
                email = "shahiidkhan108@gmail.com",
                photoUrl = null,
                accessToken = generateToken("access"),
                refreshToken = generateToken("refresh"),
                expiresAtMillis = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(30),
                authProvider = AuthProvider.OFFLINE_LOCAL
            )
            sessionManager.saveSession(initialSession)
            _currentSession.value = initialSession
        }
    }

    suspend fun checkAndRefreshSession(): Result<UserSession?> = withContext(Dispatchers.IO) {
        val session = sessionManager.getSession()
        if (session == null) {
            _currentSession.value = null
            return@withContext Result.success(null)
        }

        val now = System.currentTimeMillis()
        // Refresh token if within 2 days of expiration or already expired
        val twoDaysMillis = TimeUnit.DAYS.toMillis(2)
        if (now + twoDaysMillis >= session.expiresAtMillis) {
            val refreshed = session.copy(
                accessToken = generateToken("access_refreshed"),
                refreshToken = generateToken("refresh_renewed"),
                expiresAtMillis = now + TimeUnit.DAYS.toMillis(30),
                lastActiveMillis = now
            )
            sessionManager.saveSession(refreshed)
            _currentSession.value = refreshed
            Result.success(refreshed)
        } else {
            // Update last active
            val activeSession = session.copy(lastActiveMillis = now)
            sessionManager.saveSession(activeSession)
            _currentSession.value = activeSession
            Result.success(activeSession)
        }
    }

    suspend fun loginWithEmail(email: String, displayName: String): Result<UserSession> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val session = UserSession(
            userId = "usr_${UUID.randomUUID().toString().take(8)}",
            displayName = displayName.ifBlank { "SpendWise User" },
            email = email.trim(),
            accessToken = generateToken("access"),
            refreshToken = generateToken("refresh"),
            expiresAtMillis = now + TimeUnit.DAYS.toMillis(30),
            authProvider = AuthProvider.EMAIL,
            createdAtMillis = now,
            lastActiveMillis = now
        )
        sessionManager.saveSession(session)
        _currentSession.value = session
        Result.success(session)
    }

    suspend fun loginWithGoogle(accountName: String, email: String, photoUrl: String? = null): Result<UserSession> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val session = UserSession(
            userId = "goog_${UUID.randomUUID().toString().take(10)}",
            displayName = accountName.ifBlank { "Google User" },
            email = email.trim(),
            photoUrl = photoUrl,
            accessToken = generateToken("google_oauth"),
            refreshToken = generateToken("google_refresh"),
            expiresAtMillis = now + TimeUnit.DAYS.toMillis(30),
            authProvider = AuthProvider.GOOGLE,
            createdAtMillis = now,
            lastActiveMillis = now
        )
        sessionManager.saveSession(session)
        _currentSession.value = session
        Result.success(session)
    }

    suspend fun logout(): Result<Unit> = withContext(Dispatchers.IO) {
        sessionManager.clearSession()
        _currentSession.value = null
        Result.success(Unit)
    }

    private fun generateToken(prefix: String): String {
        return "$prefix-${UUID.randomUUID()}-${System.currentTimeMillis()}"
    }
}
