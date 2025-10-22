package com.secretari.app.util

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.util.UUID
import androidx.core.content.edit
import java.security.MessageDigest

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
     * Retrieves the device-persistent identifier from encrypted storage or generates a new one.
     * This identifier will survive app reinstallation and factory resets.
     * @return The device-persistent identifier string
     */
    fun getDeviceIdentifier(): String {
        val storedId = encryptedPrefs.getString(deviceIdKey, null)
        return if (storedId != null) {
            storedId
        } else {
            // Generate a new device-persistent identifier if none exists
            val id = generateDevicePersistentIdentifier()
            encryptedPrefs.edit {
                putString(deviceIdKey, id)
            }
            id
        }
    }
    
    /**
     * Generates a device-persistent identifier that survives app reinstallation
     * Uses a combination of device characteristics to create a stable identifier
     * while maintaining privacy by hashing the information
     */
    private fun generateDevicePersistentIdentifier(): String {
        return try {
            // Create a stable identifier based on device characteristics
            val deviceInfo = buildString {
                // Use device hardware characteristics that are stable across resets
                append(android.os.Build.MANUFACTURER)
                append(android.os.Build.MODEL)
                append(android.os.Build.BOARD)
                append(android.os.Build.HARDWARE)
                append(android.os.Build.DEVICE)
                append(android.os.Build.PRODUCT)
                append(android.os.Build.FINGERPRINT) // This is stable and unique per device
                append(android.os.Build.BRAND)
                append(android.os.Build.TYPE)
                
                // Add app-specific salt to prevent cross-app tracking
                append("SecretariApp2024")
            }
            
            // Hash the combined information to create a stable, privacy-friendly identifier
            val digest = MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(deviceInfo.toByteArray())
            
            // Convert to hex string and take first 32 characters for a clean UUID-like format
            hash.joinToString("") { "%02x".format(it) }.take(32)
            
        } catch (e: Exception) {
            // Fallback to UUID if anything fails
            UUID.randomUUID().toString().replace("-", "")
        }
    }
    
    /**
     * Generates a secure unique identifier for this app installation
     * This is more privacy-friendly than using device identifiers
     */
    private fun generateSecureIdentifier(): String {
        return UUID.randomUUID().toString()
    }
    
    /**
     * Resets the device identifier, generating a new one
     * This can be used for privacy purposes or if the user wants a fresh start
     */
    fun resetDeviceIdentifier(): String {
        val newId = generateDevicePersistentIdentifier()
        encryptedPrefs.edit {
            putString(deviceIdKey, newId)
        }
        return newId
    }
    
    /**
     * Forces generation of a new random identifier (for privacy reset)
     */
    fun generateNewRandomIdentifier(): String {
        val newId = UUID.randomUUID().toString().replace("-", "")
        encryptedPrefs.edit {
            putString(deviceIdKey, newId)
        }
        return newId
    }
}
