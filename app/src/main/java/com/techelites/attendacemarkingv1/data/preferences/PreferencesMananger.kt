package com.techelites.attendacemarkingv1.data.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences by lazy { getEncryptedSharedPreferences() }

    companion object {
        private const val PREFS_NAME = "secure_prefs"
        private const val KEY_TOKEN = "token"
        private const val KEY_USERNAME = "username"
        private const val KEY_ROLE = "role"
        private const val KEY_ASSIGNED_GATES = "assigned_gates"
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

    // Basic String operations
    fun putString(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    fun getString(key: String, defaultValue: String = ""): String {
        return prefs.getString(key, defaultValue) ?: defaultValue
    }

    fun putStringSet(key: String, values: Set<String>) {
        prefs.edit().putStringSet(key, values).apply()
    }

    fun getStringSet(key: String, defaultValue: Set<String> = emptySet()): Set<String> {
        return prefs.getStringSet(key, defaultValue) ?: defaultValue
    }

    // Username and Role
    fun getUsername(): String {
        return getString(KEY_USERNAME)
    }

    fun getRole(): String {
        return getString(KEY_ROLE)
    }

    // Assigned Gates
    fun getAssignedGates(): List<String> {
        val gatesSet = getStringSet(KEY_ASSIGNED_GATES)
        return gatesSet.toList()
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