package com.shahid.spendwise.auth.model

import kotlinx.serialization.Serializable

enum class AuthProvider {
    GOOGLE,
    EMAIL,
    OFFLINE_LOCAL
}

@Serializable
data class UserSession(
    val userId: String,
    val displayName: String,
    val email: String,
    val photoUrl: String? = null,
    val accessToken: String,
    val refreshToken: String,
    val expiresAtMillis: Long,
    val authProvider: AuthProvider = AuthProvider.OFFLINE_LOCAL,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val lastActiveMillis: Long = System.currentTimeMillis()
) {
    val isExpired: Boolean
        get() = System.currentTimeMillis() >= expiresAtMillis
}
