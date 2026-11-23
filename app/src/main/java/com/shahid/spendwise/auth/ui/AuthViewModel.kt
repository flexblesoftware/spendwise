package com.shahid.spendwise.auth.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shahid.spendwise.auth.model.UserSession
import com.shahid.spendwise.auth.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    data class Authenticated(val session: UserSession) : AuthUiState()
    object Unauthenticated : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    val currentSession: StateFlow<UserSession?> = authRepository.currentSession

    init {
        checkSession()
    }

    fun checkSession() {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val result = authRepository.checkAndRefreshSession()
            result.onSuccess { session ->
                if (session != null) {
                    _uiState.value = AuthUiState.Authenticated(session)
                } else {
                    _uiState.value = AuthUiState.Unauthenticated
                }
            }.onFailure { error ->
                _uiState.value = AuthUiState.Error(error.message ?: "Failed to verify session")
            }
        }
    }

    fun loginWithEmail(email: String, name: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val result = authRepository.loginWithEmail(email, name)
            result.onSuccess { session ->
                _uiState.value = AuthUiState.Authenticated(session)
            }.onFailure { error ->
                _uiState.value = AuthUiState.Error(error.message ?: "Login failed")
            }
        }
    }

    fun loginWithGoogle(accountName: String, email: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val result = authRepository.loginWithGoogle(accountName, email)
            result.onSuccess { session ->
                _uiState.value = AuthUiState.Authenticated(session)
            }.onFailure { error ->
                _uiState.value = AuthUiState.Error(error.message ?: "Google login failed")
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            authRepository.logout()
            _uiState.value = AuthUiState.Unauthenticated
        }
    }
}
