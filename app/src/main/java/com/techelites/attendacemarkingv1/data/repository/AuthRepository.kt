package com.techelites.attendacemarkingv1.data.repository


import android.util.Log
import com.techelites.attendacemarkingv1.data.preferences.PreferencesManager
import com.techelites.attendacemarkingv1.network.AuthApiService
import com.techelites.attendacemarkingv1.network.LoginRequest
import com.techelites.attendacemarkingv1.network.LoginResponse
import com.techelites.attendacemarkingv1.utils.Result1
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val authApiService: AuthApiService,
    private val preferencesManager: PreferencesManager
) {
    private val TAG = "AuthRepository"

    suspend fun login(username: String, password: String): Result1<LoginResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = authApiService.login(LoginRequest(username, password))
                if (response.isSuccessful && response.body() != null) {
                    val loginResponse = response.body()!!
                    Log.d(TAG, "Login successful: $loginResponse")

                    // Save user information to encrypted preferences
                    saveUserDetails(loginResponse)
                    Result1.Success(loginResponse)
                } else {
                    val errorMessage = response.errorBody()?.string() ?: "Login failed"
                    Log.e(TAG, "Login failed: $errorMessage")
                    Result1.Error(Exception(errorMessage))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Login error: ${e.message}", e)
                Result1.Error(e)
            }
        }
    }

    private fun saveUserDetails(loginResponse: LoginResponse) {
        // Save token
        preferencesManager.saveToken(loginResponse.token)

        // Save username and role
        preferencesManager.putString("username", loginResponse.data.username)
        preferencesManager.putString("role", loginResponse.data.role)

        // Save assigned gates
        val assignedGatesSet = loginResponse.data.assignedGates.toSet()
        preferencesManager.putStringSet("assigned_gates", assignedGatesSet)

        Log.d(TAG, "Saved user details: username=${loginResponse.data.username}, role=${loginResponse.data.role}, gates=${loginResponse.data.assignedGates}")
    }

    fun isLoggedIn(): Boolean {
        return preferencesManager.isLoggedIn()
    }

    suspend fun logout() {
        withContext(Dispatchers.IO) {
            preferencesManager.clearAll()
        }
    }

    fun getUserInfo(): Triple<String, String, List<String>> {
        return Triple(
            preferencesManager.getUsername(),
            preferencesManager.getRole(),
            preferencesManager.getAssignedGates()
        )
    }
}