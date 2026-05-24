package com.example.secapp.data.model.dto

data class PublicKeyResponse(
    val userId: String,
    val masterKeyId: String,
    val publicKey: String
)

data class ActiveSessionResponse(
    val sessionKeyId: String,
    val sessionPublicKey: String,
    val deviceInfo: String?,
    val createdAt: String?,
    val lastActiveAt: String?
)

data class CreateConversationRequest(
    val type: String,
    val name: String?,
    val participantIds: List<String>,
    val encryptedKeys: List<EncryptedConversationKeyRequest>
)

data class EncryptedConversationKeyRequest(
    val userId: String,
    val recipientType: String,
    val recipientKeyId: String,
    val encryptedConversationKey: String,
    val keyVersion: Int
)

data class StoreConversationKeysRequest(
    val newKeyVersion: Int,
    val reason: String?,
    val encryptedKeys: List<EncryptedConversationKeyRequest>
)

data class ConversationResponse(
    val id: String,
    val type: String,
    val name: String?,
    val currentKeyVersion: Int,
    val lastMessageAt: String?,
    val participants: List<UserResponse>,
    val createdAt: String?,
    val updatedAt: String?
)

data class EncryptedConversationKeyResponse(
    val recipientType: String,
    val recipientKeyId: String,
    val encryptedConversationKey: String,
    val keyVersion: Int
)

data class SendMessageRequest(
    val clientMessageId: String,
    val cipherData: String,
    val iv: String,
    val aad: String?,
    val keyVersion: Int,
    val messageType: String,
    val clientCreatedAt: String,
    val attachments: List<MessageAttachmentRequest>? = null
)

data class UpdateMessageRequest(
    val cipherData: String,
    val iv: String,
    val aad: String?,
    val keyVersion: Int,
    val messageType: String
)

data class MessageCreatedResponse(
    val messageId: String,
    val conversationId: String,
    val senderId: String,
    val serverCreatedAt: String?
)

data class MessageResponse(
    val id: String,
    val conversationId: String,
    val senderId: String,
    val keyVersion: Int,
    val clientMessageId: String?,
    val cipherData: String,
    val iv: String,
    val aad: String?,
    val messageType: String,
    val clientCreatedAt: String?,
    val serverCreatedAt: String?,
    val editedAt: String?,
    val attachments: List<MessageAttachmentResponse>? = null
)

data class MessageAttachmentRequest(
    val storageProvider: String,
    val storageKey: String,
    val encryptedFileKey: String,
    val encryptedMetadata: String
)

data class MessageAttachmentResponse(
    val id: String,
    val storageProvider: String,
    val storageKey: String,
    val encryptedFileKey: String,
    val encryptedMetadata: String,
    val createdAt: String?
)

data class ApiMessageResponse(
    val message: String
)

data class RealtimeMessageRequest(
    val conversationId: String,
    val senderId: String,
    val clientMessageId: String,
    val cipherData: String,
    val iv: String,
    val aad: String?,
    val keyVersion: Int,
    val messageType: String,
    val clientCreatedAt: String,
    val attachments: List<MessageAttachmentRequest>? = null
)

data class RealtimeMessageEvent(
    val type: String,
    val message: MessageResponse?
)

data class RealtimeMemberLeftData(
    val conversationId: String,
    val leftUserId: String
)

data class RealtimeMemberLeftEvent(
    val type: String,
    val data: RealtimeMemberLeftData?
)

data class RealtimeErrorEvent(
    val type: String,
    val message: String?
)
