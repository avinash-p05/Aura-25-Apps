package com.techelites.attendacemarkingv1.data.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.google.gson.Gson
import com.techelites.attendacemarkingv1.network.Event
import com.techelites.attendacemarkingv1.network.Scanner
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences by lazy { getEncryptedSharedPreferences() }
    private val gson = Gson()

    companion object {
        private const val PREFS_NAME = "secure_prefs"
        private const val KEY_TOKEN = "token"
        private const val KEY_USERNAME = "username"
        private const val KEY_ROLE = "role"
        private const val KEY_SCANNER_ID = "scanner_id"
        private const val KEY_SCANNER_NAME = "scanner_name"
        private const val KEY_ASSIGNED_EVENTS = "assigned_events"
        private const val KEY_CAN_SCAN_ALL_EVENTS = "can_scan_all_events"
    }

    private fun getEncryptedSharedPreferences(): SharedPreferences {
        val masterKey = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        return EncryptedSharedPreferences.create(
            PREFS_NAME,
            masterKey,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    // Token Management
    fun saveToken(token: String) {
        prefs.edit().putString(KEY_TOKEN, token).apply()
    }

    fun getToken(): String {
        return prefs.getString(KEY_TOKEN, "") ?: ""
    }

    // Scanner Details
    fun saveScannerDetails(scanner: Scanner) {
        prefs.edit().apply {
            putString(KEY_SCANNER_ID, scanner.id)
            putString(KEY_USERNAME, scanner.username)
            putString(KEY_SCANNER_NAME, scanner.name)
            putString(KEY_ROLE, scanner.role)
        }.apply()
    }

    fun getScannerDetails(): Scanner {
        return Scanner(
            id = prefs.getString(KEY_SCANNER_ID, "") ?: "",
            username = prefs.getString(KEY_USERNAME, "") ?: "",
            name = prefs.getString(KEY_SCANNER_NAME, "") ?: "",
            role = prefs.getString(KEY_ROLE, "") ?: ""
        )
    }

    // Assigned Events Management
    fun saveAssignedEvents(events: List<Event>) {
        val eventsJson = gson.toJson(events)
        prefs.edit().putString(KEY_ASSIGNED_EVENTS, eventsJson).apply()
    }

    fun getAssignedEvents(): List<Event> {
        val eventsJson = prefs.getString(KEY_ASSIGNED_EVENTS, null)
        return if (eventsJson != null) {
            gson.fromJson(eventsJson, Array<Event>::class.java).toList()
        } else {
            emptyList()
        }
    }

    // Scan All Events Permission
    fun saveCanScanAllEvents(canScanAllEvents: Boolean) {
        prefs.edit().putBoolean(KEY_CAN_SCAN_ALL_EVENTS, canScanAllEvents).apply()
    }

    fun getCanScanAllEvents(): Boolean {
        return prefs.getBoolean(KEY_CAN_SCAN_ALL_EVENTS, false)
    }

    // Clear all stored preferences
    fun clearAll() {
        prefs.edit().clear().apply()
    }

    // Check if user is logged in
    fun isLoggedIn(): Boolean {
        return getToken().isNotBlank()
    }
}