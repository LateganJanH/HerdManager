package com.herdmanager.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.herdmanager.app.domain.model.AuthUser
import com.herdmanager.app.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    val authState: StateFlow<AuthUser?> = authRepository.authState()
        .catch { emit(null) }
        .stateIn(
            scope = viewModelScope,
            started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _errorMessage.value = null
            _isLoading.value = true
            authRepository.signIn(email, password)
                .onSuccess { _isLoading.value = false }
                .onFailure { e ->
                    _isLoading.value = false
                    _errorMessage.value = e.message ?: "Sign in failed"
                }
        }
    }

    fun signUp(email: String, password: String) {
        viewModelScope.launch {
            _errorMessage.value = null
            _isLoading.value = true
            authRepository.signUp(email, password)
                .onSuccess { _isLoading.value = false }
                .onFailure { e ->
                    _isLoading.value = false
                    _errorMessage.value = e.message ?: "Sign up failed"
                }
        }
    }

    fun signOut() {
        authRepository.signOut()
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
