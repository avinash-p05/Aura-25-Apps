package com.techelites.attendacemarkingv1.ui.main

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.techelites.attendacemarkingv1.R
import com.techelites.attendacemarkingv1.data.preferences.PreferencesManager
import com.techelites.attendacemarkingv1.data.repository.AuthRepository
import com.techelites.attendacemarkingv1.network.Event
import com.techelites.attendacemarkingv1.utils.Result1
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject
import javax.inject.Inject

data class StudentInfo(
    val name: String = "",
    val college: String = "",
    val department: String = "",
    val year: Int = 0,
    val section: String = "",
    val photoUrl: String = "",
    val uid: String = "",
    val usn: String = "",
    val registrationId: String = "",
    val userId: String = "",
    val eventId: String = ""
)

data class MainUiState(
    val username: String = "",
    val role: String = "",
    val assignedGates: List<Event> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String = "",
    val successMessage: String = "",
    val studentInfo: StudentInfo? = null,
    val showStudentDetails: Boolean = false,
    val processingStatus: String = "",
    val endpointType: String = "attendance", // "attendance" or "id-collection"
    val currentCheckpoint: String = "main-gate",
    val currentAction: String = "entry" // "entry", "exit", "collect", "return"
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val preferencesManager: PreferencesManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val TAG = "MainViewModel"

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        loadUserInfo()
    }

    private fun loadUserInfo() {
        _uiState.update {
            it.copy(
                username = preferencesManager.getScannerDetails().username,
                role = preferencesManager.getScannerDetails().role,
                assignedGates = preferencesManager.getAssignedEvents()
            )
        }
    }

    fun setEndpointType(type: String) {
        _uiState.update { it.copy(endpointType = type) }
    }

    fun setCheckpoint(checkpoint: String) {
        _uiState.update { it.copy(currentCheckpoint = checkpoint) }
    }

    fun setAction(action: String) {
        _uiState.update { it.copy(currentAction = action) }
    }

    fun processScannedData(data: String) {
        val currentState = _uiState.value
        val endpointType = currentState.endpointType
        val checkpoint = currentState.currentCheckpoint
        val action = currentState.currentAction

        Log.d(TAG, "Processing scanned data: $data for $endpointType $checkpoint $action")

        _uiState.update {
            it.copy(
                isLoading = true,
                errorMessage = "",
                successMessage = "",
                studentInfo = null,
                showStudentDetails = false
            )
        }

        viewModelScope.launch {
            try {
                // Parse the JSON data
                val jsonObject = JSONObject(data)

                // Extract necessary identifiers
                val registrationId = jsonObject.optString("registrationId", "")
                val userId = jsonObject.optString("userId", "")
                val eventId = jsonObject.optString("eventId", "")

                // Determine the correct API endpoint based on the type and action
                val endpoint = when (endpointType) {
                    "attendance" -> {
                        if (action == "entry") "attendance/mark" else "attendance/unmark"
                    }
                    "id-collection" -> {
                        if (action == "collect") "id/collect" else "id/return"
                    }

                    else -> "attendance/mark" // Default fallback
                }

                // Process the request
                when (val result = authRepository.processScannedData(
                    registrationId,
                    userId,
                    eventId,
                    endpoint
                )) {
                    is Result1.Success -> {
                        val jsonResponse = result.data
                        val success = jsonResponse.optBoolean("success", false)
                        val message = jsonResponse.optString("message", "Success")
                        playSound(R.raw.success)
                        if (success && jsonResponse.has("data")) {
                            val dataObj = jsonResponse.getJSONObject("data")
                            val userType = dataObj.optString("userType", "")

                            if (userType == "GIT_STUDENT" && dataObj.has("student")) {
                                val studentObj = dataObj.getJSONObject("student")
                                Log.d(TAG, "Student Info: $studentObj")

                                if (studentObj.has("name")) {
                                    // Extract student info
                                    val studentInfo = StudentInfo(
                                        name = studentObj.optString("name", ""),
                                        college = studentObj.optString("college", ""),
                                        department = studentObj.optString("department", ""),
                                        year = studentObj.optInt("year", 0),
                                        section = studentObj.optString("section", ""),
                                        photoUrl = studentObj.optString("photoUrl", ""),
                                        uid = studentObj.optString("uid", ""),
                                        usn = studentObj.optString("usn", ""),
                                        registrationId = registrationId,
                                        userId = userId,
                                        eventId = eventId
                                    )

                                    _uiState.update {
                                        it.copy(
                                            isLoading = false,
                                            studentInfo = studentInfo,
                                            showStudentDetails = true,
                                            successMessage = message,
                                            processingStatus = "success"
                                        )
                                    }
                                } else {
                                    _uiState.update {
                                        it.copy(
                                            isLoading = false,
                                            successMessage = message,
                                            processingStatus = "success"
                                        )
                                    }
                                }
                            } else {
                                _uiState.update {
                                    it.copy(
                                        isLoading = false,
                                        successMessage = message,
                                        processingStatus = "success"
                                    )
                                }
                            }
                        } else {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    successMessage = message,
                                    processingStatus = "success"
                                )
                            }
                        }
                    }

                    is Result1.Error -> {
                        playSound(R.raw.warning)
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = result.exception.message ?: "Unknown error",
                                processingStatus = "error"
                            )
                        }
                    }

                    Result1.Loading -> {} // Already handled
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing scanned data: ${e.message}", e)
                playSound(R.raw.warning)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Error: ${e.message}",
                        processingStatus = "error"
                    )
                }
            }
        }
    }

    fun dismissStudentDetails() {
        _uiState.update {
            it.copy(
                showStudentDetails = false,
                studentInfo = null
            )
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
        }
    }

    private fun playSound(soundResId: Int) {
        try {
            val mediaPlayer = MediaPlayer.create(context, soundResId)
            mediaPlayer.setOnCompletionListener { it.release() }
            mediaPlayer.start()
        } catch (e: Exception) {
            Log.e(TAG, "Error playing sound: ${e.message}")
        }
    }
}