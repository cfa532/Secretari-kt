package com.secretari.app.util

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.secretari.app.data.model.AppConstants
import com.secretari.app.data.model.Settings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsManager(private val context: Context) {
    
    private val settingsKey = stringPreferencesKey("app_settings")
    
    val settingsFlow: Flow<Settings> = context.dataStore.data.map { preferences ->
        val settingsJson = preferences[settingsKey]
        if (settingsJson != null) {
            try {
                Json.decodeFromString<Settings>(settingsJson)
            } catch (e: Exception) {
                AppConstants.DEFAULT_SETTINGS
            }
        } else {
            AppConstants.DEFAULT_SETTINGS
        }
    }
    
    suspend fun updateSettings(settings: Settings) {
        context.dataStore.edit { preferences ->
            preferences[settingsKey] = Json.encodeToString(settings)
        }
    }
    
    suspend fun getSettings(): Settings {
        var settings = AppConstants.DEFAULT_SETTINGS
        context.dataStore.data.collect { preferences ->
            val settingsJson = preferences[settingsKey]
            if (settingsJson != null) {
                try {
                    settings = Json.decodeFromString(settingsJson)
                } catch (e: Exception) {
                    settings = AppConstants.DEFAULT_SETTINGS
                }
            }
        }
        return settings
    }
    
    companion object {
        @Volatile
        private var INSTANCE: SettingsManager? = null
        
        fun getInstance(context: Context): SettingsManager {
            return INSTANCE ?: synchronized(this) {
                val instance = SettingsManager(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}

