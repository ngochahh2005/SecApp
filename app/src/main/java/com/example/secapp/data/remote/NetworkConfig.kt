package com.example.secapp.data.remote

import android.content.Context
import com.example.secapp.data.local.security.AuthSessionState
import com.example.secapp.data.local.security.SecureStorage
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor

object NetworkConfig {
    private const val BASE_URL = "https://secapi-ibir.onrender.com/"
    const val WEB_SOCKET_URL = "wss://secapi-ibir.onrender.com/ws/messages"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val client = NetworkTimeoutPolicy.applyTo(OkHttpClient.Builder())
        .addInterceptor(loggingInterceptor)
        .build()

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val authService: AuthService by lazy {
        retrofit.create(AuthService::class.java)
    }

    fun userService(context: Context): UserService {
        return authenticatedRetrofit(context).create(UserService::class.java)
    }

    fun conversationService(context: Context): ConversationService {
        return authenticatedRetrofit(context).create(ConversationService::class.java)
    }

    fun messageService(context: Context): MessageService {
        return authenticatedRetrofit(context).create(MessageService::class.java)
    }

    private fun authenticatedRetrofit(context: Context): Retrofit {
        val secureStorage = SecureStorage(context.applicationContext)
        val authenticatedClient = NetworkTimeoutPolicy.applyTo(OkHttpClient.Builder())
            .addInterceptor { chain ->
                val token = AuthSessionState.getAccessToken() ?: secureStorage.getAccessToken()
                val requestBuilder = chain.request().newBuilder()
                val authorization = chain.request().header("Authorization")
                    ?: token?.takeIf { it.isNotBlank() }?.toAuthorizationHeader()

                if (!authorization.isNullOrBlank()) {
                    if (chain.request().header("Authorization").isNullOrBlank()) {
                        requestBuilder.header("Authorization", authorization)
                    }
                }
                chain.proceed(requestBuilder.build())
            }
            .addInterceptor(loggingInterceptor)
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(authenticatedClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    private fun String.toAuthorizationHeader(): String {
        return if (startsWith("Bearer ", ignoreCase = true)) this else "Bearer $this"
    }
}
