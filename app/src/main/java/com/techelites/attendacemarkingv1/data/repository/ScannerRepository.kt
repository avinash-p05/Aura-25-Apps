package com.techelites.attendacemarkingv1.data.repository

import android.util.Log
import com.techelites.attendacemarkingv1.data.preferences.PreferencesManager
import com.techelites.attendacemarkingv1.network.models.StudentDetails
import com.techelites.attendacemarkingv1.utils.Result1
import com.techelites.attendacemarkingv1.network.AuthApiService
import com.techelites.attendacemarkingv1.network.EntryExitRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScannerRepository @Inject constructor(
    private val authApiService: AuthApiService,
    private val securePreferences: PreferencesManager
) {
    private val TAG = "ScannerRepository"

    suspend fun processScannedData(
        data: String,
        checkpoint: String,
        action: String
    ): Result1<StudentDetails> {
        val identifier = try {
            val json = JSONObject(data)
            json.getString("token") // Extracts token if data is JSON
        } catch (e: Exception) {
            data // Uses data as-is if it's not JSON
        }

        return withContext(Dispatchers.IO) {
            try {
                // Get token from SecurePreferences
                val token = "Bearer "+securePreferences.getToken()
                Log.d(TAG, "Using authorization token: ${token.take(15)}...")
                Log.d(TAG, "Processing request for checkpoint: $checkpoint, action: $action, identifier: ${identifier.take(15)}...")

                val request = EntryExitRequest(identifier = identifier)

                // Call appropriate API endpoint based on checkpoint and action
                val response = when {
                    checkpoint == "main-gate" && action == "entry" -> {
                        Log.d(TAG, "Calling mainGateEntry API")
                        authApiService.mainGateEntry(token, request)
                    }
                    checkpoint == "main-gate" && action == "exit" -> {
                        Log.d(TAG, "Calling mainGateExit API")
                        authApiService.mainGateExit(token, request)
                    }
                    checkpoint == "concert-area" && action == "entry" -> {
                        Log.d(TAG, "Calling concertGateEntry API")
                        authApiService.concertGateEntry(token, request)
                    }
                    checkpoint == "concert-area" && action == "exit" -> {
                        Log.d(TAG, "Calling concertGateExit API")
                        authApiService.concertGateExit(token, request)
                    }
                    else -> return@withContext Result1.Error(RuntimeException("Invalid checkpoint or action"))
                }

                parseResponse(response)
            } catch (e: Exception) {
                Log.e(TAG, "Error in processScannedData: ${e.message}", e)
                Result1.Error(RuntimeException("Error: ${e.localizedMessage}"))
            }
        }
    }

    private fun parseResponse(response: Response<ResponseBody>): Result1<StudentDetails> {
        if (!response.isSuccessful) {
            val errorMessage = try {
                val errorBody = response.errorBody()?.string()
                Log.e(TAG, "Error response: $errorBody")

                if (errorBody != null) {
                    try {
                        val jsonObject = JSONObject(errorBody)
                        val message = jsonObject.optString("message", "Unknown error")
                        // Return the raw JSON for better parsing in the ViewModel
                        errorBody
                    } catch (e: Exception) {
                        "Error: ${response.code()} - $errorBody"
                    }
                } else {
                    "Error: ${response.code()} - ${response.message()}"
                }
            } catch (e: Exception) {
                "Error occurred: ${e.message}"
            }
            return Result1.Error(RuntimeException(errorMessage))
        }

        try {
            val responseBodyString = response.body()?.string() ?: return Result1.Error(RuntimeException("Empty response"))
            Log.d(TAG, "Success response: $responseBodyString")

            val jsonObject = JSONObject(responseBodyString)
            val success = jsonObject.optBoolean("success", false)
            val message = jsonObject.optString("message", "Success")

            if (success && jsonObject.has("data")) {
                val dataObj = jsonObject.getJSONObject("data")
                val userType = dataObj.optString("userType", "")

                // Create a student details object that will serve as a generic user info container
                val studentDetails: StudentDetails

                // Handle different types of users
                if (dataObj.has("student")) {
                    // Regular Student
                    val studentObj = dataObj.getJSONObject("student")
                    studentDetails = StudentDetails(
                        name = studentObj.optString("name", ""),
                        college = studentObj.optString("college", ""),
                        department = studentObj.optString("department", ""),
                        year = studentObj.optInt("year", 0),
                        section = studentObj.optString("section", ""),
                        photoUrl = studentObj.optString("photoUrl", ""),
                        uid = studentObj.optString("uid", ""),
                        usn = studentObj.optString("usn", ""),
                        message = message,
                        userType = userType
                    )
                } else if (userType == "FACULTY") {
                    // Faculty member
                    // For faculty, we just show the message and userType
                    studentDetails = StudentDetails(
                        name = "Faculty Member", // Generic name since the API doesn't provide a name
                        message = message,
                        userType = userType
                    )

                    // Add faculty specific details if available
                    if (dataObj.has("entryCount")) {
                        studentDetails.additionalInfo["entryCount"] = dataObj.optInt("entryCount", 0).toString()
                    }
                    if (dataObj.has("remainingConcertEntries")) {
                        studentDetails.additionalInfo["remainingEntries"] = dataObj.optInt("remainingConcertEntries", 0).toString()
                    }
                    if (dataObj.has("status")) {
                        studentDetails.additionalInfo["status"] = dataObj.optString("status", "")
                    }
                    if (dataObj.has("currentLocation")) {
                        studentDetails.additionalInfo["location"] = dataObj.optString("currentLocation", "")
                    }

                } else {
                    // Other user types - just show the message and userType
                    studentDetails = StudentDetails(
                        message = message,
                        userType = userType
                    )
                }

                return Result1.Success(studentDetails)
            }

            // Even with success=false, return a StudentDetails with just the message
            if (!success) {
                return Result1.Success(StudentDetails(message = message))
            }

            return Result1.Error(RuntimeException(message))
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing response: ${e.message}", e)
            return Result1.Error(RuntimeException("Error parsing response: ${e.message}"))
        }
    }
}