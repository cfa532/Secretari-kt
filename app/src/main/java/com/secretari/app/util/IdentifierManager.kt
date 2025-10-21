package com.secretari.app.util

import android.content.Context
import android.provider.Settings
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.util.UUID
import androidx.core.content.edit

class IdentifierManager(private val context: Context) {
    
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    
    private val encryptedPrefs = EncryptedSharedPreferences.create(
        context,
        "device_identifier_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    
    private val deviceIdKey = "device_identifier"
    private val hasLaunchedBeforeKey = "has_launched_before"
    
    /**
     * Sets up the device identifier on first launch.
     * @return true if this is the first launch, false otherwise
     */
    fun setupIdentifier(): Boolean {
        val hasLaunchedBefore = encryptedPrefs.getBoolean(hasLaunchedBeforeKey, true)
        
        if (hasLaunchedBefore) {
            println("Launch for the first time.")
            val identifier = getDeviceIdentifier()
            encryptedPrefs.edit {
                putBoolean(hasLaunchedBeforeKey, false)
                    .putString(deviceIdKey, identifier)
            }
            println("Device identifier: $identifier")
            return true
        }
        return false
    }
    
    /**
     * Retrieves the device identifier from encrypted storage or generates a new one.
     * @return The device identifier string
     */
    fun getDeviceIdentifier(): String {
        val storedId = encryptedPrefs.getString(deviceIdKey, null)
        return if (storedId != null) {
            storedId
        } else {
            // Generate a new identifier if none exists
            val id = getAndroidId() ?: UUID.randomUUID().toString()
            encryptedPrefs.edit {
                putString(deviceIdKey, id)
            }
            id
        }
    }
    
    /**
     * Gets the Android ID, which is unique per device per app installation
     */
    private fun getAndroidId(): String? {
        return try {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        } catch (e: Exception) {
            null
        }
    }
}
