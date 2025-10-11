package com.secretari.app.data.network

import com.secretari.app.data.model.User
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

interface ApiService {
    
    @POST("secretari/users/register")
    suspend fun registerUser(@Body request: RegisterRequest): Response<RegisterResponse>
    
    @PUT("secretari/users")
    suspend fun updateUser(
        @Header("Authorization") token: String,
        @Body request: UpdateUserRequest
    ): Response<UpdateUserResponse>
    
    @DELETE("secretari/users")
    suspend fun deleteUser(@Header("Authorization") token: String): Response<DeleteUserResponse>
    
    @POST("secretari/users/temp")
    suspend fun createTempUser(@Body request: TempUserRequest): Response<TempUserResponse>
    
    @GET("secretari/productids")
    suspend fun getProductIDs(): Response<Map<String, Double>>
    
    @GET("secretari/notice")
    suspend fun getNotice(): Response<String>
    
    @FormUrlEncoded
    @POST("secretari/token")
    suspend fun fetchToken(
        @Field("username") username: String,
        @Field("password") password: String
    ): Response<TokenResponse>
    
    companion object {
        private const val BASE_URL = "https://secretari.leither.uk/"
        
        fun create(): ApiService {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
            
            val client = OkHttpClient.Builder()
                .addInterceptor(logging)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()
            
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(ApiService::class.java)
        }
    }
}

// Request/Response models
data class RegisterRequest(
    val username: String,
    val password: String,
    val family_name: String,
    val given_name: String,
    val email: String,
    val id: String
)

data class RegisterResponse(
    val id: String,
    val username: String,
    val family_name: String?,
    val given_name: String?,
    val email: String?,
    val token_count: Long,
    val dollar_balance: Double,
    val monthly_usage: Map<String, Double>?
)

data class UpdateUserRequest(
    val username: String,
    val password: String,
    val email: String,
    val family_name: String,
    val given_name: String
)

data class UpdateUserResponse(
    val email: String,
    val family_name: String,
    val given_name: String
)

data class DeleteUserResponse(
    val id: String
)

data class TempUserRequest(
    val username: String,
    val password: String,
    val id: String
)

data class TempUserResponse(
    val token: TokenData,
    val user: UserData
)

data class TokenData(
    val access_token: String,
    val token_type: String
)

data class UserData(
    val id: String,
    val username: String,
    val token_count: Long,
    val dollar_balance: Double,
    val monthly_usage: Map<String, Double>?
)

data class TokenResponse(
    val access_token: String,
    val token_type: String
)

