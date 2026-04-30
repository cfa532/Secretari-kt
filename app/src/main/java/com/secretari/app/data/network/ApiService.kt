package com.secretari.app.data.network

import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Query
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
    suspend fun getProductIDs(): Response<Map<String, ProductIDsData>>
    
    @GET("secretari/notice")
    suspend fun getNotice(): Response<String>
    
    @POST("secretari/users/redeem")
    suspend fun redeemCoupon(
        @Header("Authorization") token: String,
        @Query("coupon") coupon: String
    ): Response<Boolean>
    
    @GET("secretari/server/status")
    suspend fun getServerStatus(): Response<ServerStatusResponse>
    
    @POST("secretari/token")
    suspend fun fetchToken(
        @Body requestBody: RequestBody
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
    @SerializedName("username") val username: String,
    @SerializedName("password") val password: String,
    @SerializedName("family_name") val family_name: String,
    @SerializedName("given_name") val given_name: String,
    @SerializedName("email") val email: String,
    @SerializedName("id") val id: String
)

data class RegisterResponse(
    @SerializedName("id") val id: String,
    @SerializedName("username") val username: String,
    @SerializedName("family_name") val family_name: String?,
    @SerializedName("given_name") val given_name: String?,
    @SerializedName("email") val email: String?,
    @SerializedName("token_count") val token_count: Long,
    @SerializedName("dollar_balance") val dollar_balance: Double,
    @SerializedName("monthly_usage") val monthly_usage: Map<String, Double>?
)

data class UpdateUserRequest(
    @SerializedName("username") val username: String,
    @SerializedName("password") val password: String,
    @SerializedName("email") val email: String,
    @SerializedName("family_name") val family_name: String,
    @SerializedName("given_name") val given_name: String
)

data class UpdateUserResponse(
    @SerializedName("email") val email: String,
    @SerializedName("family_name") val family_name: String,
    @SerializedName("given_name") val given_name: String
)

data class DeleteUserResponse(
    @SerializedName("id") val id: String
)

data class TempUserRequest(
    @SerializedName("username") val username: String,
    @SerializedName("password") val password: String,
    @SerializedName("id") val id: String
)

data class TempUserResponse(
    @SerializedName("token") val token: TokenData,
    @SerializedName("user") val user: UserData
)

data class TokenData(
    @SerializedName("access_token") val access_token: String,
    @SerializedName("token_type") val token_type: String
)

data class UserData(
    @SerializedName("id") val id: String,
    @SerializedName("username") val username: String,
    @SerializedName("token_count") val token_count: Long,
    @SerializedName("dollar_balance") val dollar_balance: Double,
    @SerializedName("monthly_usage") val monthly_usage: Map<String, Double>?
)

data class TokenResponse(
    @SerializedName("token") val token: TokenData,
    @SerializedName("user") val user: UserData
)

data class ServerStatusResponse(
    @SerializedName("server_time") val server_time: String,
    @SerializedName("leither_port") val leither_port: Int?,
    @SerializedName("leither_connected") val leither_connected: Boolean,
    @SerializedName("active_connections") val active_connections: Int,
    @SerializedName("llm_model") val llm_model: String,
    @SerializedName("server_maintenance") val server_maintenance: String,
    @SerializedName("max_token_limits") val max_token_limits: Map<String, Int>,
    @SerializedName("error") val error: String? = null
)

data class ProductIDsData(
    @SerializedName("productIDs") val productIDs: Map<String, Double>
)


