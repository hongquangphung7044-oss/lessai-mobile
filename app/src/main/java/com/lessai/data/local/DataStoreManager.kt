package com.lessai.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class DataStoreManager(private val context: Context) {
    companion object {
        val API_BASE_URL = stringPreferencesKey("api_base_url")
        val API_KEY = stringPreferencesKey("api_key")
        val MODEL = stringPreferencesKey("model")
        val SEGMENT_STRATEGY = stringPreferencesKey("segment_strategy")
    }

    val apiBaseUrl: Flow<String> = context.dataStore.data.map { it[API_BASE_URL] ?: "" }
    val apiKey: Flow<String> = context.dataStore.data.map { it[API_KEY] ?: "" }
    val model: Flow<String> = context.dataStore.data.map { it[MODEL] ?: "gpt-4.1-mini" }
    val segmentStrategy: Flow<String> = context.dataStore.data.map { it[SEGMENT_STRATEGY] ?: "sentence" }

    suspend fun saveSettings(baseUrl: String, apiKey: String, model: String, strategy: String) {
        context.dataStore.edit { prefs ->
            prefs[API_BASE_URL] = baseUrl
            prefs[API_KEY] = apiKey
            prefs[MODEL] = model
            prefs[SEGMENT_STRATEGY] = strategy
        }
    }
}