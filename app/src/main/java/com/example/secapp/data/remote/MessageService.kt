package com.example.secapp.data.remote

import com.example.secapp.data.model.dto.ApiMessageResponse
import com.example.secapp.data.model.dto.MessageCreatedResponse
import com.example.secapp.data.model.dto.MessageResponse
import com.example.secapp.data.model.dto.SendMessageRequest
import com.example.secapp.data.model.dto.UpdateMessageRequest
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface MessageService {
    @GET("api/v1/conversations/{conversationId}/messages")
    suspend fun getMessages(
        @Header("Authorization") authorization: String,
        @Path("conversationId") conversationId: String,
        @Query("limit") limit: Int = 30
    ): List<MessageResponse>

    @POST("api/v1/conversations/{conversationId}/messages")
    suspend fun sendMessage(
        @Header("Authorization") authorization: String,
        @Path("conversationId") conversationId: String,
        @Body request: SendMessageRequest
    ): MessageCreatedResponse

    @PATCH("api/v1/conversations/{conversationId}/messages/{messageId}")
    suspend fun updateMessage(
        @Header("Authorization") authorization: String,
        @Path("conversationId") conversationId: String,
        @Path("messageId") messageId: String,
        @Body request: UpdateMessageRequest
    ): MessageResponse

    @DELETE("api/v1/conversations/{conversationId}/messages/{messageId}")
    suspend fun deleteMessage(
        @Header("Authorization") authorization: String,
        @Path("conversationId") conversationId: String,
        @Path("messageId") messageId: String
    ): ApiMessageResponse
}
