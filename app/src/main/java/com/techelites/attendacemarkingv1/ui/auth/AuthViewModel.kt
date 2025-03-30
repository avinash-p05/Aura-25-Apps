package com.techelites.attendacemarkingv1.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.techelites.attendacemarkingv1.data.repository.AuthRepository
import com.techelites.attendacemarkingv1.network.LoginResponse
import com.techelites.attendacemarkingv1.utils.Result1
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val username: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val userData: UserDisplayData? = null,
    val errorMessage: String = ""
)

data class UserDisplayData(
    val username: String,
    val role: String,
    val assignedGates: List<String>
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _authState = MutableStateFlow(AuthUiState())
    val authState: StateFlow<AuthUiState> = _authState.asStateFlow()

    init {
        checkLoggedIn()
    }

    fun onUsernameChange(username: String) {
        _authState.update { it.copy(username = username, errorMessage = "") }
    }

    fun onPasswordChange(password: String) {
        _authState.update { it.copy(password = password, errorMessage = "") }
    }

    fun login() {
        val currentState = _authState.value
        val username = currentState.username
        val password = currentState.password

        if (username.isBlank() || password.isBlank()) {
            _authState.update { it.copy(errorMessage = "Please fill in all fields") }
            return
        }

        _authState.update { it.copy(isLoading = true, errorMessage = "") }

        viewModelScope.launch {
            when (val result = authRepository.login(username, password)) {
                is Result1.Success -> {
                    handleLoginSuccess(result.data)
                }
                is Result1.Error -> {
                    val errorMessage = result.exception.message ?: "Login failed"
                    _authState.update { it.copy(
                        isLoading = false,
                        errorMessage = errorMessage
                    ) }
                }
                is Result1.Loading -> {} // Already handled
            }
        }
    }

    private fun handleLoginSuccess(loginResponse: LoginResponse) {
        val userData = UserDisplayData(
            username = loginResponse.data.username,
            role = loginResponse.data.role,
            assignedGates = loginResponse.data.assignedGates
        )

        _authState.update { it.copy(
            isLoading = false,
            isLoggedIn = true,
            userData = userData
        ) }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _authState.update { it.copy(
                isLoggedIn = false,
                userData = null
            ) }
        }
    }

    fun checkLoggedIn() {
        val isLoggedIn = authRepository.isLoggedIn()

        if (isLoggedIn) {
            val (username, role, gates) = authRepository.getUserInfo()
            _authState.update { it.copy(
                isLoggedIn = true,
                userData = UserDisplayData(
                    username = username,
                    role = role,
                    assignedGates = gates
                )
            ) }
        } else {
            _authState.update { it.copy(isLoggedIn = false) }
        }
    }
}