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
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.lang.ref.WeakReference

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsManager(context: Context) {
    private val contextRef = WeakReference(context)
    private val context: Context get() = contextRef.get() ?: throw IllegalStateException("Context has been garbage collected")
    
    private val settingsKey = stringPreferencesKey("app_settings")
    
    @OptIn(InternalSerializationApi::class)
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
    
    @OptIn(InternalSerializationApi::class)
    suspend fun updateSettings(settings: Settings) {
        context.dataStore.edit { preferences ->
            preferences[settingsKey] = Json.encodeToString(settings)
        }
    }
    
    @OptIn(InternalSerializationApi::class)
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
        
        fun clearInstance() {
            INSTANCE = null
        }
    }
}

