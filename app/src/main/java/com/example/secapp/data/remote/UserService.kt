package com.example.secapp.data.remote

import com.example.secapp.data.model.dto.ActiveSessionResponse
import com.example.secapp.data.model.dto.PublicKeyResponse
import com.example.secapp.data.model.dto.UserResponse
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

interface UserService {
    @GET("api/v1/users/me")
    suspend fun me(@Header("Authorization") authorization: String): UserResponse

    @GET("api/v1/users/search")
    suspend fun searchUsers(
        @Header("Authorization") authorization: String,
        @Query("q") keyword: String
    ): List<UserResponse>

    @GET("api/v1/users/{userId}/public-key")
    suspend fun getPublicKey(
        @Header("Authorization") authorization: String,
        @Path("userId") userId: String
    ): PublicKeyResponse

    @GET("api/v1/users/{userId}/sessions")
    suspend fun getActiveSessions(
        @Header("Authorization") authorization: String,
        @Path("userId") userId: String
    ): List<ActiveSessionResponse>
}
