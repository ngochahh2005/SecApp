package com.example.secapp.data.remote

import com.example.secapp.data.model.dto.ApiMessageResponse
import com.example.secapp.data.model.dto.ConversationResponse
import com.example.secapp.data.model.dto.CreateConversationRequest
import com.example.secapp.data.model.dto.EncryptedConversationKeyResponse
import com.example.secapp.data.model.dto.StoreConversationKeysRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface ConversationService {
    @GET("api/v1/conversations")
    suspend fun listConversations(
        @Header("Authorization") authorization: String
    ): List<ConversationResponse>

    @GET("api/v1/conversations/{conversationId}")
    suspend fun getConversation(
        @Header("Authorization") authorization: String,
        @Path("conversationId") conversationId: String
    ): ConversationResponse

    @POST("api/v1/conversations")
    suspend fun createConversation(
        @Header("Authorization") authorization: String,
        @Body request: CreateConversationRequest
    ): ConversationResponse

    @GET("api/v1/conversations/{conversationId}/keys/me")
    suspend fun getMyConversationKey(
        @Header("Authorization") authorization: String,
        @Path("conversationId") conversationId: String,
        @Query("keyVersion") keyVersion: Int? = null
    ): EncryptedConversationKeyResponse

    @POST("api/v1/conversations/{conversationId}/keys")
    suspend fun storeConversationKeys(
        @Header("Authorization") authorization: String,
        @Path("conversationId") conversationId: String,
        @Body request: StoreConversationKeysRequest
    ): Response<ApiMessageResponse>

    @PUT("api/v1/conversations/{conversationId}/keys")
    suspend fun rotateConversationKeys(
        @Header("Authorization") authorization: String,
        @Path("conversationId") conversationId: String,
        @Body request: StoreConversationKeysRequest
    ): ApiMessageResponse

    @DELETE("api/v1/conversations/{conversationId}")
    suspend fun deleteConversation(
        @Header("Authorization") authorization: String,
        @Path("conversationId") conversationId: String
    ): ApiMessageResponse
}
