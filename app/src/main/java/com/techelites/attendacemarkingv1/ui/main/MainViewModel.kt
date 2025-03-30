package com.techelites.attendacemarkingv1.ui.main

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.techelites.attendacemarkingv1.R
import com.techelites.attendacemarkingv1.data.preferences.PreferencesManager
import com.techelites.attendacemarkingv1.data.repository.AuthRepository
import com.techelites.attendacemarkingv1.data.repository.ScannerRepository
import com.techelites.attendacemarkingv1.network.models.StudentDetails
import com.techelites.attendacemarkingv1.utils.Result1
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MainUiState(
    val username: String = "",
    val role: String = "",
    val assignedGates: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String = "",
    val successMessage: String = "",
    val studentDetails: StudentDetails? = null,
    val showStudentDetails: Boolean = false,
    val processingStatus: String = "",
    val currentCheckpoint: String = "main-gate", // main-gate or concert-area
    val currentAction: String = "entry" // entry or exit
)

sealed class MainNavigationEvent {
    data class NavigateToScanner(val checkpoint: String, val action: String) : MainNavigationEvent()
    object NavigateToLogin : MainNavigationEvent()
}

@HiltViewModel
class MainViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val scannerRepository: ScannerRepository,
    private val preferencesManager: PreferencesManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val TAG = "MainViewModel"

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val _navigationEvents = MutableSharedFlow<MainNavigationEvent>()
    val navigationEvents = _navigationEvents.asSharedFlow()

    init {
        loadUserInfo()
    }

    private fun loadUserInfo() {
        _uiState.update {
            it.copy(
                username = preferencesManager.getUsername(),
                role = preferencesManager.getRole(),
                assignedGates = preferencesManager.getAssignedGates()
            )
        }
    }

    fun setCheckpoint(checkpoint: String) {
        _uiState.update { it.copy(currentCheckpoint = checkpoint) }
        Log.d(TAG, "Checkpoint set to: $checkpoint")
    }

    fun setAction(action: String) {
        _uiState.update { it.copy(currentAction = action) }
        Log.d(TAG, "Action set to: $action")
    }

    fun navigateToScanner() {
        val checkpoint = _uiState.value.currentCheckpoint
        val action = _uiState.value.currentAction

        Log.d(TAG, "Navigating to scanner with checkpoint: $checkpoint, action: $action")
        viewModelScope.launch {
            _navigationEvents.emit(MainNavigationEvent.NavigateToScanner(checkpoint, action))
        }
    }

    fun processScannedData(data: String) {
        val currentState = _uiState.value
        val checkpoint = currentState.currentCheckpoint
        val action = currentState.currentAction

        Log.d(TAG, "Processing scanned data: $data for $checkpoint - $action")

        _uiState.update {
            it.copy(
                isLoading = true,
                errorMessage = "",
                successMessage = "",
                studentDetails = null,
                showStudentDetails = false
            )
        }

        viewModelScope.launch {
            try {
                when (val result = scannerRepository.processScannedData(data, checkpoint, action)) {
                    is Result1.Success -> {
                        playSound(R.raw.success)
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                studentDetails = result.data,
                                showStudentDetails = true,
                                successMessage = result.data.message,
                                processingStatus = "success"
                            )
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
                    is Result1.Loading -> {} // Already handled
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
                studentDetails = null
            )
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _navigationEvents.emit(MainNavigationEvent.NavigateToLogin)
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