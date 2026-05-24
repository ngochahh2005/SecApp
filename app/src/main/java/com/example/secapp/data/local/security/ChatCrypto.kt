package com.example.secapp.data.local.security

import com.google.gson.Gson
import javax.crypto.spec.SecretKeySpec

data class EncryptedPayload(
    val cipherData: String,
    val iv: String,
    val aad: String?
)

data class ChatPayloadAttachment(
    val id: String,
    val displayName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val base64Data: String
)

data class DecryptedChatPayload(
    val content: String,
    val clientCreatedAt: String?,
    val attachments: List<ChatPayloadAttachment>
)

private data class PlainTextMessagePayload(
    val content: String?,
    val clientCreatedAt: String?,
    val attachments: List<ChatPayloadAttachment>?
)

private data class AttachmentMetadataPayload(
    val id: String,
    val displayName: String,
    val mimeType: String,
    val sizeBytes: Long
)

object ChatCrypto {
    private const val AES_ALGORITHM = "AES"
    private const val CONVERSATION_KEY_BYTES = 32
    private const val GCM_IV_BYTES = 12
    private val gson = Gson()

    fun generateConversationKey(): SecretKeySpec {
        return SecretKeySpec(CryptoHelper.generateRandomBytes(CONVERSATION_KEY_BYTES), AES_ALGORITHM)
    }

    fun wrapConversationKey(conversationKey: SecretKeySpec, publicKeyBase64: String): String {
        val publicKey = CryptoHelper.rsaPublicKeyFromBase64(publicKeyBase64)
        return CryptoHelper.encryptRsaOaepToBase64(conversationKey.encoded, publicKey)
    }

    fun unwrapConversationKey(encryptedConversationKey: String, privateKey: java.security.PrivateKey): SecretKeySpec {
        val rawKey = CryptoHelper.decryptRsaOaepToBytes(encryptedConversationKey, privateKey)
        return SecretKeySpec(rawKey, AES_ALGORITHM)
    }

    fun encryptMessage(
        content: String,
        clientCreatedAt: String,
        conversationKey: SecretKeySpec,
        aadValue: String,
        attachments: List<ChatPayloadAttachment> = emptyList()
    ): EncryptedPayload {
        val ivBytes = CryptoHelper.generateRandomBytes(GCM_IV_BYTES)
        val aadBytes = aadValue.toByteArray(Charsets.UTF_8)
        val payload = PlainTextMessagePayload(content, clientCreatedAt, attachments)
        val cipherBytes = CryptoHelper.encryptAesGcm(
            plainText = gson.toJson(payload).toByteArray(Charsets.UTF_8),
            secretKey = conversationKey,
            iv = ivBytes,
            aad = aadBytes
        )
        return EncryptedPayload(
            cipherData = CryptoHelper.toBase64(cipherBytes),
            iv = CryptoHelper.toBase64(ivBytes),
            aad = CryptoHelper.toBase64(aadBytes)
        )
    }

    fun encryptAttachmentMetadata(
        attachment: ChatPayloadAttachment,
        conversationKey: SecretKeySpec,
        aadValue: String
    ): String {
        val ivBytes = CryptoHelper.generateRandomBytes(GCM_IV_BYTES)
        val aadBytes = aadValue.toByteArray(Charsets.UTF_8)
        val metadata = AttachmentMetadataPayload(
            id = attachment.id,
            displayName = attachment.displayName,
            mimeType = attachment.mimeType,
            sizeBytes = attachment.sizeBytes
        )
        val cipherBytes = CryptoHelper.encryptAesGcm(
            plainText = gson.toJson(metadata).toByteArray(Charsets.UTF_8),
            secretKey = conversationKey,
            iv = ivBytes,
            aad = aadBytes
        )
        val encrypted = EncryptedPayload(
            cipherData = CryptoHelper.toBase64(cipherBytes),
            iv = CryptoHelper.toBase64(ivBytes),
            aad = CryptoHelper.toBase64(aadBytes)
        )
        return CryptoHelper.toBase64(gson.toJson(encrypted).toByteArray(Charsets.UTF_8))
    }

    fun randomAttachmentFileKey(): String {
        return CryptoHelper.toBase64(CryptoHelper.generateRandomBytes(CONVERSATION_KEY_BYTES))
    }

    fun decryptMessageContent(
        cipherData: String,
        iv: String,
        aad: String?,
        conversationKey: SecretKeySpec
    ): String {
        return decryptMessagePayload(cipherData, iv, aad, conversationKey).content
    }

    fun decryptMessagePayload(
        cipherData: String,
        iv: String,
        aad: String?,
        conversationKey: SecretKeySpec
    ): DecryptedChatPayload {
        val aadBytes = aad?.let(CryptoHelper::fromBase64)
        val plainBytes = CryptoHelper.decryptAesGcm(
            cipherText = CryptoHelper.fromBase64(cipherData),
            secretKey = conversationKey,
            iv = CryptoHelper.fromBase64(iv),
            aad = aadBytes
        )
        val plainText = String(plainBytes, Charsets.UTF_8)
        return runCatching {
            val payload = gson.fromJson(plainText, PlainTextMessagePayload::class.java)
            DecryptedChatPayload(
                content = payload.content.orEmpty(),
                clientCreatedAt = payload.clientCreatedAt,
                attachments = payload.attachments.orEmpty()
            )
        }.getOrElse {
            DecryptedChatPayload(
                content = plainText,
                clientCreatedAt = null,
                attachments = emptyList()
            )
        }
    }

    fun encodeConversationName(name: String): String {
        return CryptoHelper.toBase64(name.toByteArray(Charsets.UTF_8))
    }

    fun decodeConversationName(name: String?): String {
        if (name.isNullOrBlank()) return "Cuộc trò chuyện"
        return runCatching {
            String(CryptoHelper.fromBase64(name), Charsets.UTF_8)
        }.getOrDefault(name)
    }
}
