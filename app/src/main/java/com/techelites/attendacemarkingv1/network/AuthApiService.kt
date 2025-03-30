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
    val success: Boolean,
    val token: String,
    val data: UserData
)

data class UserData(
    val username: String,
    val role: String,
    val assignedGates: List<String>
)

// Entry & Exit Request
data class EntryExitRequest(val identifier: String)

interface AuthApiService {

    // Login API
    @Headers("Content-Type: application/json")
    @POST("admin/admin/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    // Main Gate Entry - Return raw response body for easier JSON parsing
    @POST("entry/entry/main-gate")
    suspend fun mainGateEntry(
        @Header("Authorization") token: String,
        @Body request: EntryExitRequest
    ): Response<ResponseBody>

    // Concert Gate Entry
    @POST("entry/entry/concert-area")
    suspend fun concertGateEntry(
        @Header("Authorization") token: String,
        @Body request: EntryExitRequest
    ): Response<ResponseBody>

    // Main Gate Exit
    @POST("entry/exit/main-gate")
    suspend fun mainGateExit(
        @Header("Authorization") token: String,
        @Body request: EntryExitRequest
    ): Response<ResponseBody>

    // Concert Gate Exit
    @POST("entry/exit/concert-area")
    suspend fun concertGateExit(
        @Header("Authorization") token: String,
        @Body request: EntryExitRequest
    ): Response<ResponseBody>
}