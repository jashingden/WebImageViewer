package com.eddy.webcrawler.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        private val KEY_LAST_URL = stringPreferencesKey("last_url")
        private val KEY_LAST_PATTERN = stringPreferencesKey("last_pattern")
        private val KEY_LAST_RULE = stringPreferencesKey("last_rule")
        private val KEY_LOCK_ENABLED = booleanPreferencesKey("lock_enabled")
        private val KEY_LOCK_PIN = stringPreferencesKey("lock_pin")
    }

    val lockEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_LOCK_ENABLED] ?: false
    }

    val lockPin: Flow<String?> = dataStore.data.map { preferences ->
        preferences[KEY_LOCK_PIN]
    }

    suspend fun setLockEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_LOCK_ENABLED] = enabled
        }
    }

    suspend fun setLockPin(pin: String) {
        dataStore.edit { preferences ->
            preferences[KEY_LOCK_PIN] = pin
        }
    }

    val lastUrl: Flow<String> = dataStore.data.map { preferences ->
        preferences[KEY_LAST_URL] ?: ""
    }

    val lastPattern: Flow<String> = dataStore.data.map { preferences ->
        preferences[KEY_LAST_PATTERN] ?: ""
    }

    val lastRule: Flow<String> = dataStore.data.map { preferences ->
        preferences[KEY_LAST_RULE] ?: ""
    }

    suspend fun saveLastCrawlSettings(url: String, pattern: String, rule: String) {
        dataStore.edit { preferences ->
            preferences[KEY_LAST_URL] = url
            preferences[KEY_LAST_PATTERN] = pattern
            preferences[KEY_LAST_RULE] = rule
        }
    }
}
