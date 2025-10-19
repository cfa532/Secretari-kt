package com.secretari.app.util

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.secretari.app.data.model.User
import com.secretari.app.data.network.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

private val Context.userDataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

class UserManager(private val context: Context) {
    
    private val userKey = stringPreferencesKey("current_user")
    private val apiService = ApiService.create()
    
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    
    private val encryptedPrefs = EncryptedSharedPreferences.create(
        context,
        "secret_shared_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    
    enum class LoginStatus {
        SIGNED_IN, SIGNED_OUT, UNREGISTERED
    }
    
    val currentUserFlow: Flow<User?> = context.userDataStore.data.map { preferences ->
        val userJson = preferences[userKey]
        userJson?.let {
            try {
                Json.decodeFromString<User>(it)
            } catch (e: Exception) {
                null
            }
        }
    }
    
    var userToken: String?
        get() = encryptedPrefs.getString("user_token", null)
        set(value) {
            encryptedPrefs.edit().putString("user_token", value).apply()
        }
    
    suspend fun persistUser(user: User) {
        context.userDataStore.edit { preferences ->
            preferences[userKey] = Json.encodeToString(user)
        }
    }
    
    suspend fun createTempUser(): User? {
        val deviceId = getDeviceId()
        val tempUser = User(
            id = deviceId,
            username = deviceId,
            password = "zaq1^WSX"
        )
        
        return try {
            val response = apiService.createTempUser(
                TempUserRequest(
                    username = tempUser.username,
                    password = tempUser.password,
                    id = tempUser.id
                )
            )
            
            if (response.isSuccessful) {
                val body = response.body()
                userToken = body?.token?.access_token
                
                val user = body?.user?.let {
                    User(
                        id = it.id,
                        username = it.username,
                        tokenCount = it.token_count,
                        dollarBalance = it.dollar_balance,
                        monthlyUsage = it.monthly_usage
                    )
                }
                user?.let { persistUser(it) }
                user
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    suspend fun register(user: User): Boolean {
        return try {
            val response = apiService.registerUser(
                RegisterRequest(
                    username = user.username,
                    password = user.password,
                    family_name = user.familyName ?: "",
                    given_name = user.givenName ?: "",
                    email = user.email ?: "",
                    id = user.id
                )
            )
            
            if (response.isSuccessful) {
                val body = response.body()
                body?.let {
                    val updatedUser = User(
                        id = it.id,
                        username = it.username,
                        familyName = it.family_name,
                        givenName = it.given_name,
                        email = it.email,
                        tokenCount = it.token_count,
                        dollarBalance = it.dollar_balance,
                        monthlyUsage = it.monthly_usage
                    )
                    persistUser(updatedUser)
                }
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }
    
    suspend fun login(username: String, password: String): Boolean {
        return try {
            val response = apiService.fetchToken(username, password)
            if (response.isSuccessful) {
                val body = response.body()
                userToken = body?.access_token
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }
    
    suspend fun updateUser(user: User): Boolean {
        val token = userToken ?: return false
        return try {
            val response = apiService.updateUser(
                token = "Bearer $token",
                request = UpdateUserRequest(
                    username = user.username,
                    password = user.password,
                    email = user.email ?: "",
                    family_name = user.familyName ?: "",
                    given_name = user.givenName ?: ""
                )
            )
            
            if (response.isSuccessful) {
                persistUser(user)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }
    
    suspend fun deleteAccount(): Boolean {
        val token = userToken ?: return false
        return try {
            val response = apiService.deleteUser("Bearer $token")
            if (response.isSuccessful) {
                userToken = null
                context.userDataStore.edit { it.clear() }
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }
    
    suspend fun redeemCoupon(coupon: String): Boolean {
        val token = userToken ?: return false
        return try {
            val response = apiService.redeemCoupon("Bearer $token", coupon)
            response.isSuccessful && response.body() == true
        } catch (e: Exception) {
            false
        }
    }
    
    suspend fun getServerStatus(): ServerStatusResponse? {
        return try {
            val response = apiService.getServerStatus()
            if (response.isSuccessful) {
                response.body()
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    private fun getDeviceId(): String {
        val deviceId = encryptedPrefs.getString("device_id", null)
        return if (deviceId != null) {
            deviceId
        } else {
            val newId = UUID.randomUUID().toString()
            encryptedPrefs.edit().putString("device_id", newId).apply()
            newId
        }
    }
    
    companion object {
        @Volatile
        private var INSTANCE: UserManager? = null
        
        fun getInstance(context: Context): UserManager {
            return INSTANCE ?: synchronized(this) {
                val instance = UserManager(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}

