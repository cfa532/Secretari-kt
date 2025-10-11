package com.secretari.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class User(
    @SerialName("id")
    val id: String,
    
    @SerialName("username")
    val username: String,
    
    @SerialName("password")
    val password: String = "",
    
    @SerialName("token_count")
    val tokenCount: Long = 0,
    
    @SerialName("dollar_balance")
    val dollarBalance: Double = 0.0,
    
    @SerialName("monthly_usage")
    val monthlyUsage: Map<String, Double>? = null,
    
    @SerialName("family_name")
    val familyName: String? = null,
    
    @SerialName("given_name")
    val givenName: String? = null,
    
    @SerialName("email")
    val email: String? = null
) {
    val initials: String
        get() {
            val firstName = givenName ?: "John"
            val lastName = familyName ?: "Smith"
            return "${firstName.firstOrNull()?.uppercaseChar() ?: ""}${lastName.firstOrNull()?.uppercaseChar() ?: ""}"
        }
    
    val displayUsername: String?
        get() = if (username.length > 20) null else username
}

