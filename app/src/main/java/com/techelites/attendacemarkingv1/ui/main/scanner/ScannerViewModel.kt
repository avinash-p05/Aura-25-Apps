package com.techelites.attendacemarkingv1.ui.main.scanner

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.techelites.attendacemarkingv1.R
import com.techelites.attendacemarkingv1.data.repository.ScannerRepository
import com.techelites.attendacemarkingv1.network.models.StudentDetails
import com.techelites.attendacemarkingv1.utils.Result1
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import javax.inject.Inject

@HiltViewModel
class ScannerViewModel @Inject constructor(
    private val scannerRepository: ScannerRepository,
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val TAG = "ScannerViewModel"

    var state by mutableStateOf(ScannerState())
        private set

    private val _navigationEvents = MutableSharedFlow<ScannerNavigationEvent>()
    val navigationEvents = _navigationEvents.asSharedFlow()

    init {
        savedStateHandle.get<String>("checkpoint")?.let { checkpoint ->
            state = state.copy(checkpoint = checkpoint)
            Log.d(TAG, "Initialized checkpoint from savedState: $checkpoint")
        }

        savedStateHandle.get<String>("action")?.let { action ->
            state = state.copy(action = action)
            Log.d(TAG, "Initialized action from savedState: $action")
        }
    }

    fun setCheckpointAndAction(checkpoint: String, action: String) {
        Log.d(TAG, "Setting checkpoint to $checkpoint and action to $action")
        state = state.copy(
            checkpoint = checkpoint,
            action = action
        )
    }

    fun processScannedData(data: String) {
        Log.d(TAG, "Processing scanned data: $data")
        viewModelScope.launch {
            state = state.copy(isLoading = true, error = null)

            // Handle JSON input
            val identifier = try {
                val json = JSONObject(data)
                if (json.has("token")) json.getString("token") else data
            } catch (e: Exception) {
                data // Not JSON, use as-is
            }

            Log.d(TAG, "Calling repository with checkpoint=${state.checkpoint}, action=${state.action}")
            when (val result = scannerRepository.processScannedData(
                data = data,
                checkpoint = state.checkpoint,
                action = state.action
            )) {
                is Result1.Success -> {
                    Log.d(TAG, "Processing success: ${result.data}")
                    playSound(if (result.data.name.isEmpty()) R.raw.warning else R.raw.success)

                    state = state.copy(
                        isLoading = false,
                        studentDetails = result.data,
                        showStudentDetailsDialog = true
                    )
                }
                is Result1.Error -> {
                    Log.e(TAG, "Processing error: ${result.exception.message}")
                    playSound(R.raw.warning)

                    // Check if the error is in JSON format
                    val errorMessage = result.exception.message ?: "Unknown error"

                    state = state.copy(
                        isLoading = false,
                        error = errorMessage
                    )
                }
                is Result1.Loading -> {
                    state = state.copy(isLoading = true)
                }
            }
        }
    }

    private fun playSound(soundResId: Int) {
        try {
            val mediaPlayer = MediaPlayer.create(context, soundResId)
            mediaPlayer.setOnCompletionListener { it.release() }
            mediaPlayer.start()
        } catch (e: Exception) {
            Log.e(TAG, "Error playing sound", e)
            // Silent fail if sound can't be played
        }
    }

    fun dismissStudentDetailsDialog() {
        Log.d(TAG, "Dismissing student details dialog")
        state = state.copy(showStudentDetailsDialog = false)
        // No longer automatically restart scanner here
    }

    fun clearError() {
        Log.d(TAG, "Clearing error state")
        state = state.copy(error = null)
        // No longer automatically restart scanner here
    }

    fun startNewScan() {
        Log.d(TAG, "Explicitly starting new scan")
        viewModelScope.launch {
            _navigationEvents.emit(ScannerNavigationEvent.RestartScanner)
        }
    }

    fun navigateBack() {
        viewModelScope.launch {
            _navigationEvents.emit(ScannerNavigationEvent.NavigateBack)
        }
    }

    fun showError(errorMessage: String) {
        Log.e(TAG, "Error: $errorMessage")
        state = state.copy(error = errorMessage)
    }
}

data class ScannerState(
    val checkpoint: String = "main-gate",
    val action: String = "entry",
    val isLoading: Boolean = false,
    val error: String? = null,
    val studentDetails: StudentDetails? = null,
    val showStudentDetailsDialog: Boolean = false
)

sealed class ScannerNavigationEvent {
    object RestartScanner : ScannerNavigationEvent()
    object NavigateBack : ScannerNavigationEvent()
}