package com.example.secapp.data.local.security

import com.google.gson.Gson
import javax.crypto.spec.SecretKeySpec

data class EncryptedPayload(
    val cipherData: String,
    val iv: String,
    val aad: String?
)

private data class PlainTextMessagePayload(
    val content: String,
    val clientCreatedAt: String
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
        aadValue: String
    ): EncryptedPayload {
        val ivBytes = CryptoHelper.generateRandomBytes(GCM_IV_BYTES)
        val aadBytes = aadValue.toByteArray(Charsets.UTF_8)
        val payload = PlainTextMessagePayload(content, clientCreatedAt)
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

    fun decryptMessageContent(
        cipherData: String,
        iv: String,
        aad: String?,
        conversationKey: SecretKeySpec
    ): String {
        val aadBytes = aad?.let(CryptoHelper::fromBase64)
        val plainBytes = CryptoHelper.decryptAesGcm(
            cipherText = CryptoHelper.fromBase64(cipherData),
            secretKey = conversationKey,
            iv = CryptoHelper.fromBase64(iv),
            aad = aadBytes
        )
        val plainText = String(plainBytes, Charsets.UTF_8)
        return runCatching {
            gson.fromJson(plainText, PlainTextMessagePayload::class.java).content
        }.getOrElse {
            plainText
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
