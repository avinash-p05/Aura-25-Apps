package com.techelites.attendacemarkingv1.network

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST

// Login Request & Response
data class LoginRequest(val username: String, val password: String)
data class LoginResponse(
    val message: String,
    val scanner: Scanner,
    val assignedEvents: List<Event>,
    val canScanAllEvents: Boolean,
    val token: String
)

data class Scanner(
    val id: String,
    val name: String,
    val username: String,
    val role: String
)

data class Event(
    val _id: String,
    val id: Int,
    val name: String,
    val date: String,
    val venue: String
)

// Generic Request for all API endpoints
data class MainRequest(
    val registrationId: String,
    val userId: String,
    val eventId: String
)

interface AuthApiService {
    // Login API
    @Headers("Content-Type: application/json")
    @POST("scan/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    // Mark Attendance
    @POST("scan/attendance/mark")
    suspend fun markAttendance(
        @Body request: MainRequest
    ): Response<ResponseBody>

    // Unmark Attendance
    @POST("scan/attendance/unmark")
    suspend fun unmarkAttendance(
        @Body request: MainRequest
    ): Response<ResponseBody>

    // ID Collection APIs
    // Collect ID
    @POST("scan/mark-id-card/collect")
    suspend fun collectId(
        @Body request: MainRequest
    ): Response<ResponseBody>

    // Return ID
    @POST("scan/mark-id-card/return")
    suspend fun returnId(
        @Body request: MainRequest
    ): Response<ResponseBody>
}