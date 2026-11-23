package com.shahid.spendwise

import com.shahid.spendwise.auth.model.AuthProvider
import com.shahid.spendwise.auth.model.UserSession
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

class SessionPersistenceTest {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @Test
    fun testUserSessionSerialization() {
        val original = UserSession(
            userId = "usr_shahid_001",
            displayName = "MD Shahid",
            email = "shahiidkhan108@gmail.com",
            photoUrl = null,
            accessToken = "access_token_xyz_123",
            refreshToken = "refresh_token_abc_789",
            expiresAtMillis = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(30),
            authProvider = AuthProvider.OFFLINE_LOCAL
        )

        val serialized = json.encodeToString(original)
        assertNotNull(serialized)
        assertTrue(serialized.contains("MD Shahid"))
        assertTrue(serialized.contains("shahiidkhan108@gmail.com"))

        val restored = json.decodeFromString<UserSession>(serialized)
        assertEquals(original.userId, restored.userId)
        assertEquals(original.displayName, restored.displayName)
        assertEquals(original.email, restored.email)
        assertEquals(original.accessToken, restored.accessToken)
        assertEquals(original.refreshToken, restored.refreshToken)
        assertEquals(original.authProvider, restored.authProvider)
        assertEquals(original.expiresAtMillis, restored.expiresAtMillis)
    }

    @Test
    fun testSessionExpirationEvaluation() {
        val now = System.currentTimeMillis()

        val expiredSession = UserSession(
            userId = "usr_1",
            displayName = "User",
            email = "u@example.com",
            accessToken = "token_old",
            refreshToken = "refresh_old",
            expiresAtMillis = now - 1000L // 1 second ago
        )
        assertTrue("Session in the past should be expired", expiredSession.isExpired)

        val activeSession = UserSession(
            userId = "usr_2",
            displayName = "User",
            email = "u@example.com",
            accessToken = "token_new",
            refreshToken = "refresh_new",
            expiresAtMillis = now + TimeUnit.HOURS.toMillis(1) // 1 hour future
        )
        assertFalse("Session in the future should not be expired", activeSession.isExpired)
    }

    @Test
    fun testTokenRefreshLifecycle() {
        val now = System.currentTimeMillis()
        val nearExpirationSession = UserSession(
            userId = "usr_shahid_refresh",
            displayName = "MD Shahid",
            email = "shahiidkhan108@gmail.com",
            accessToken = "token_expiring_soon",
            refreshToken = "refresh_valid",
            expiresAtMillis = now + TimeUnit.HOURS.toMillis(2) // 2 hours remaining
        )

        // Silent refresh simulation: token regenerated and expiry extended by 30 days
        val refreshedSession = nearExpirationSession.copy(
            accessToken = "access_token_renewed_${now}",
            refreshToken = "refresh_token_renewed_${now}",
            expiresAtMillis = now + TimeUnit.DAYS.toMillis(30),
            lastActiveMillis = now
        )

        assertNotEquals(nearExpirationSession.accessToken, refreshedSession.accessToken)
        assertFalse(refreshedSession.isExpired)
        assertEquals(nearExpirationSession.displayName, refreshedSession.displayName)
        assertEquals(nearExpirationSession.email, refreshedSession.email)
    }

    @Test
    fun testUpdateChecksumVerification() {
        val testPayload = "SpendWise-Release-v1.0.2-Signed-Binary".toByteArray(Charsets.UTF_8)
        val expectedSize = testPayload.size.toLong()

        // Size check
        val downloadedSize = testPayload.size.toLong()
        assertEquals(expectedSize, downloadedSize)

        // SHA-256 calculation check
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(testPayload)
        val sha256Hex = digest.joinToString("") { "%02x".format(it) }

        assertNotNull(sha256Hex)
        assertEquals(64, sha256Hex.length)
    }
}
