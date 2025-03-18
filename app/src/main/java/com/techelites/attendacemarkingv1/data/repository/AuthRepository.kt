package com.techelites.attendacemarkingv1.data.repository

import com.techelites.attendacemarkingv1.data.preferences.PreferencesManager
import com.techelites.attendacemarkingv1.network.AuthApiService
import com.techelites.attendacemarkingv1.network.LoginRequest
import com.techelites.attendacemarkingv1.utils.Result1
import android.util.Log
import com.techelites.attendacemarkingv1.network.MainRequest
import com.techelites.attendacemarkingv1.network.LoginResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import retrofit2.Response
import java.io.IOException
import javax.inject.Inject

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
                    // Save user information to encrypted preferences
                    saveUserDetails(loginResponse)
                    Result1.Success(loginResponse)
                } else {
                    val errorMessage = response.errorBody()?.string() ?: "Login failed"
                    Result1.Error(Exception(errorMessage))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Login error: ${e.message}", e)
                Result1.Error(e)
            }
        }
    }

    private suspend fun saveUserDetails(loginResponse: LoginResponse) {
        withContext(Dispatchers.IO) {
            preferencesManager.apply {
                // Save token
                saveToken(loginResponse.token)

                // Save scanner details
                saveScannerDetails(loginResponse.scanner)

                // Save assigned events
                saveAssignedEvents(loginResponse.assignedEvents)

                // Save scan all events permission
                saveCanScanAllEvents(loginResponse.canScanAllEvents)
            }
        }
    }

    suspend fun processScannedData(
        registrationId: String,
        userId: String,
        eventId: String,
        endpoint: String
    ): Result1<JSONObject> {
        return withContext(Dispatchers.IO) {
            try {
                val token = "Bearer ${preferencesManager.getToken()}"
                val request = MainRequest(
                    registrationId = registrationId,
                    userId = userId,
                    eventId = eventId
                )

                val response = when (endpoint) {
                    "attendance/mark" ->
                        authApiService.markAttendance(request)
                    "attendance/unmark" ->
                        authApiService.unmarkAttendance( request)
                    "id/collect" ->
                        authApiService.collectId( request)
                    "id/return" ->
                        authApiService.returnId(request)
                    else ->
                        authApiService.markAttendance(request) // Default fallback
                }

                if (response.isSuccessful && response.body() != null) {
                    val responseBody = response.body()!!.string()
                    Result1.Success(JSONObject(responseBody))
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Unknown error"
                    Result1.Error(IOException(errorBody))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing scanned data: ${e.message}", e)
                Result1.Error(e)
            }
        }
    }

    fun isLoggedIn(): Boolean {
        return preferencesManager.isLoggedIn()
    }

    suspend fun logout() {
        withContext(Dispatchers.IO) {
            preferencesManager.clearAll()
        }
    }
}