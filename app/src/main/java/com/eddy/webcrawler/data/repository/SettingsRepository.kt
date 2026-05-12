package com.eddy.webcrawler.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
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
    }

    val lastUrl: Flow<String> = dataStore.data.map { preferences ->
        preferences[KEY_LAST_URL] ?: ""
    }

    val lastPattern: Flow<String> = dataStore.data.map { preferences ->
        preferences[KEY_LAST_PATTERN] ?: ""
    }

    suspend fun saveLastCrawlSettings(url: String, pattern: String) {
        dataStore.edit { preferences ->
            preferences[KEY_LAST_URL] = url
            preferences[KEY_LAST_PATTERN] = pattern
        }
    }
}
