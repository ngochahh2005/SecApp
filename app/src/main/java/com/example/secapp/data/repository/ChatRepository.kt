package com.example.secapp.data.repository

import android.content.Context
import com.example.secapp.data.local.security.AuthSessionState
import com.example.secapp.data.local.security.ChatCrypto
import com.example.secapp.data.local.security.ConversationKeyCache
import com.example.secapp.data.local.security.SecureStorage
import com.example.secapp.data.local.security.SessionCryptoState
import com.example.secapp.data.model.dto.ConversationResponse
import com.example.secapp.data.model.dto.CreateConversationRequest
import com.example.secapp.data.model.dto.EncryptedConversationKeyRequest
import com.example.secapp.data.model.dto.MessageResponse
import com.example.secapp.data.model.dto.RealtimeMessageRequest
import com.example.secapp.data.model.dto.SendMessageRequest
import com.example.secapp.data.model.dto.StoreConversationKeysRequest
import com.example.secapp.data.model.dto.UserResponse
import com.example.secapp.data.remote.NetworkConfig
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import javax.crypto.spec.SecretKeySpec

sealed class ChatResult<out T> {
    data class Success<out T>(val value: T) : ChatResult<T>()
    data class Failure(val message: String) : ChatResult<Nothing>()
}

data class ConversationItem(
    val id: String,
    val title: String,
    val type: String,
    val currentKeyVersion: Int,
    val lastMessageAt: String?,
    val participants: List<UserResponse>
)

data class ChatMessageItem(
    val id: String,
    val senderId: String,
    val content: String,
    val isMine: Boolean,
    val createdAt: String?,
    val canDecrypt: Boolean
)

private data class ConversationKeyMaterial(
    val keyVersion: Int,
    val key: SecretKeySpec
)

class ChatRepository(context: Context) {
    private val appContext = context.applicationContext
    private val userService = NetworkConfig.userService(appContext)
    private val conversationService = NetworkConfig.conversationService(appContext)
    private val messageService = NetworkConfig.messageService(appContext)
    private val secureStorage = SecureStorage(appContext)
    private val gson = Gson()

    suspend fun listConversations(): ChatResult<List<ConversationItem>> = withContext(Dispatchers.IO) {
        return@withContext runCatching {
            conversationService.listConversations(authHeader()).map { it.toItem() }
        }.fold(
            onSuccess = { ChatResult.Success(it) },
            onFailure = { ChatResult.Failure(readableApiError(it, "Không tải được danh sách chat")) }
        )
    }

    suspend fun searchUsers(keyword: String): ChatResult<List<UserResponse>> = withContext(Dispatchers.IO) {
        if (keyword.isBlank()) return@withContext ChatResult.Success(emptyList())
        return@withContext runCatching {
            val currentUserId = currentUserId()
            userService.searchUsers(authHeader(), keyword.trim()).filter { it.id != currentUserId }
        }.fold(
            onSuccess = { ChatResult.Success(it) },
            onFailure = { ChatResult.Failure(readableApiError(it, "Không tìm được người dùng")) }
        )
    }

    suspend fun createDirectConversation(participant: UserResponse): ChatResult<ConversationItem> = withContext(Dispatchers.IO) {
        return@withContext runCatching {
            val currentUser = secureStorage.getUserInfo() ?: userService.me(authHeader())
            val conversationKey = ChatCrypto.generateConversationKey()
            val keyVersion = 1
            val encryptedKeys = buildEncryptedKeys(
                users = listOf(currentUser, participant).distinctBy { it.id },
                conversationKey = conversationKey,
                keyVersion = keyVersion
            )
            val conversationName = ChatCrypto.encodeConversationName("Chat với ${participant.displayName}")
            val request = CreateConversationRequest(
                type = "DIRECT",
                name = conversationName,
                participantIds = listOf(participant.id),
                encryptedKeys = encryptedKeys
            )
            val conversation = conversationService.createConversation(authHeader(), request)
            ConversationKeyCache.put(conversation.id, conversation.currentKeyVersion, conversationKey)
            conversation.toItem()
        }.fold(
            onSuccess = { ChatResult.Success(it) },
            onFailure = { ChatResult.Failure(readableApiError(it, "Không tạo được cuộc trò chuyện")) }
        )
    }

    suspend fun getMessages(conversationId: String): ChatResult<List<ChatMessageItem>> = withContext(Dispatchers.IO) {
        return@withContext runCatching {
            val currentUserId = currentUserId()
            messageService.getMessages(authHeader(), conversationId).map { message ->
                val conversationKey = getConversationKeyForRead(message.conversationId, message.keyVersion)
                    ?: return@map null
                message.toItem(currentUserId, conversationKey).takeIf { it.canDecrypt }
            }.filterNotNull()
        }.fold(
            onSuccess = { ChatResult.Success(it) },
            onFailure = { ChatResult.Failure(readableApiError(it, "Không tải được tin nhắn")) }
        )
    }

    suspend fun sendTextMessage(conversationId: String, keyVersion: Int, content: String): ChatResult<Unit> = withContext(Dispatchers.IO) {
        if (content.isBlank()) return@withContext ChatResult.Failure("Tin nhắn không được để trống")
        return@withContext runCatching {
            val request = buildSendMessageRequest(conversationId, keyVersion, content)
            messageService.sendMessage(authHeader(), conversationId, request)
            Unit
        }.fold(
            onSuccess = { ChatResult.Success(Unit) },
            onFailure = { ChatResult.Failure(readableApiError(it, "Không gửi được tin nhắn")) }
        )
    }

    suspend fun buildRealtimeMessageRequest(
        conversationId: String,
        keyVersion: Int,
        content: String
    ): ChatResult<RealtimeMessageRequest> = withContext(Dispatchers.IO) {
        if (content.isBlank()) return@withContext ChatResult.Failure("Tin nhắn không được để trống")
        return@withContext runCatching {
            val sendRequest = buildSendMessageRequest(conversationId, keyVersion, content)
            RealtimeMessageRequest(
                conversationId = conversationId,
                senderId = currentUserId(),
                clientMessageId = sendRequest.clientMessageId,
                cipherData = sendRequest.cipherData,
                iv = sendRequest.iv,
                aad = sendRequest.aad,
                keyVersion = sendRequest.keyVersion,
                messageType = sendRequest.messageType,
                clientCreatedAt = sendRequest.clientCreatedAt,
                attachments = sendRequest.attachments
            )
        }.fold(
            onSuccess = { ChatResult.Success(it) },
            onFailure = { ChatResult.Failure(readableApiError(it, "Không mã hóa được tin nhắn realtime")) }
        )
    }

    suspend fun decryptIncomingMessage(message: MessageResponse): ChatMessageItem = withContext(Dispatchers.IO) {
        val conversationKey = getConversationKeyForRead(message.conversationId, message.keyVersion)
            ?: return@withContext message.toLockedItem(currentUserId())
        message.toItem(currentUserId(), conversationKey)
    }

    private suspend fun buildEncryptedKeys(
        users: List<UserResponse>,
        conversationKey: SecretKeySpec,
        keyVersion: Int
    ): List<EncryptedConversationKeyRequest> {
        val encryptedKeys = mutableListOf<EncryptedConversationKeyRequest>()
        val currentUserId = currentUserId()
        val currentMasterKey = secureStorage.getStoredMasterKey()

        for (user in users) {
            val publicKey = if (user.id == currentUserId && currentMasterKey != null) {
                com.example.secapp.data.model.dto.PublicKeyResponse(
                    userId = user.id,
                    masterKeyId = currentMasterKey.id,
                    publicKey = currentMasterKey.publicKey
                )
            } else {
                userService.getPublicKey(authHeader(), user.id)
            }

            encryptedKeys += EncryptedConversationKeyRequest(
                userId = user.id,
                recipientType = "MASTER",
                recipientKeyId = publicKey.masterKeyId,
                encryptedConversationKey = ChatCrypto.wrapConversationKey(conversationKey, publicKey.publicKey),
                keyVersion = keyVersion
            )

            val sessions = runCatching { userService.getActiveSessions(authHeader(), user.id) }.getOrDefault(emptyList())
            sessions.forEach { session ->
                encryptedKeys += EncryptedConversationKeyRequest(
                    userId = user.id,
                    recipientType = "SESSION",
                    recipientKeyId = session.sessionKeyId,
                    encryptedConversationKey = ChatCrypto.wrapConversationKey(conversationKey, session.sessionPublicKey),
                    keyVersion = keyVersion
                )
            }
        }

        return encryptedKeys
    }

    private suspend fun getConversationKey(conversationId: String, keyVersion: Int): SecretKeySpec {
        ConversationKeyCache.get(conversationId, keyVersion)?.let { return it }
        val encryptedKey = conversationService.getMyConversationKey(authHeader(), conversationId, keyVersion)
        val privateKey = when (encryptedKey.recipientType) {
            "SESSION" -> SessionCryptoState.getSessionPrivateKey()
            else -> SessionCryptoState.getMasterPrivateKey()
        } ?: throw IllegalStateException(
            if (encryptedKey.recipientType == "SESSION") {
                "Không còn session private key trong bộ nhớ. Hãy đăng nhập lại."
            } else {
                "Chưa mở khóa master private key. Hãy nhập PIN trước."
            }
        )
        val conversationKey = ChatCrypto.unwrapConversationKey(encryptedKey.encryptedConversationKey, privateKey)
        ConversationKeyCache.put(conversationId, encryptedKey.keyVersion, conversationKey)
        return conversationKey
    }

    private suspend fun getConversationKeyForRead(conversationId: String, keyVersion: Int): SecretKeySpec? {
        ConversationKeyCache.get(conversationId, keyVersion)?.let { return it }
        return runCatching { getConversationKey(conversationId, keyVersion) }.getOrNull()
    }

    private suspend fun getConversationKeyForSending(conversationId: String, requestedKeyVersion: Int): ConversationKeyMaterial {
        ConversationKeyCache.latest(conversationId, minimumVersion = requestedKeyVersion)?.let { cached ->
            return ConversationKeyMaterial(cached.keyVersion, cached.key)
        }

        return runCatching {
            ConversationKeyMaterial(requestedKeyVersion, getConversationKey(conversationId, requestedKeyVersion))
        }.getOrElse {
            provisionNewConversationKey(conversationId, requestedKeyVersion)
        }
    }

    private suspend fun provisionNewConversationKey(conversationId: String, requestedKeyVersion: Int): ConversationKeyMaterial {
        val conversation = conversationService.getConversation(authHeader(), conversationId)
        val conversationKey = ChatCrypto.generateConversationKey()
        val latestCachedVersion = ConversationKeyCache.latestVersion(conversationId) ?: 0
        val newKeyVersion = maxOf(requestedKeyVersion, conversation.currentKeyVersion, latestCachedVersion) + 1
        val encryptedKeys = buildEncryptedKeys(
            users = conversation.participants,
            conversationKey = conversationKey,
            keyVersion = newKeyVersion
        )
        val response = conversationService.storeConversationKeys(
            authHeader(),
            conversationId,
            StoreConversationKeysRequest(
                newKeyVersion = newKeyVersion,
                reason = "SESSION_WITHOUT_MASTER_KEY",
                encryptedKeys = encryptedKeys
            )
        )
        if (!response.isSuccessful) {
            throw HttpException(response)
        }
        ConversationKeyCache.put(conversationId, newKeyVersion, conversationKey)
        return ConversationKeyMaterial(newKeyVersion, conversationKey)
    }

    private suspend fun buildSendMessageRequest(
        conversationId: String,
        keyVersion: Int,
        content: String
    ): SendMessageRequest {
        val conversationKey = getConversationKeyForSending(conversationId, keyVersion)
        val createdAt = utcNow()
        val encrypted = ChatCrypto.encryptMessage(
            content = content.trim(),
            clientCreatedAt = createdAt,
            conversationKey = conversationKey.key,
            aadValue = conversationId
        )
        return SendMessageRequest(
            clientMessageId = UUID.randomUUID().toString(),
            cipherData = encrypted.cipherData,
            iv = encrypted.iv,
            aad = encrypted.aad,
            keyVersion = conversationKey.keyVersion,
            messageType = "TEXT",
            clientCreatedAt = createdAt
        )
    }

    private fun MessageResponse.toItem(currentUserId: String, conversationKey: SecretKeySpec): ChatMessageItem {
        return runCatching {
            val content = ChatCrypto.decryptMessageContent(cipherData, iv, aad, conversationKey)
            ChatMessageItem(
                id = id,
                senderId = senderId,
                content = content,
                isMine = senderId == currentUserId,
                createdAt = clientCreatedAt ?: serverCreatedAt,
                canDecrypt = true
            )
        }.getOrElse {
            ChatMessageItem(
                id = id,
                senderId = senderId,
                content = "Không thể giải mã tin nhắn này",
                isMine = senderId == currentUserId,
                createdAt = clientCreatedAt ?: serverCreatedAt,
                canDecrypt = false
            )
        }
    }

    private fun MessageResponse.toLockedItem(currentUserId: String): ChatMessageItem {
        return ChatMessageItem(
            id = id,
            senderId = senderId,
            content = "Không thể giải mã tin nhắn này",
            isMine = senderId == currentUserId,
            createdAt = clientCreatedAt ?: serverCreatedAt,
            canDecrypt = false
        )
    }

    private fun ConversationResponse.toItem(): ConversationItem {
        val currentUserId = secureStorage.getUserInfo()?.id
        val directTitle = participants.firstOrNull { it.id != currentUserId }?.displayName
        return ConversationItem(
            id = id,
            title = directTitle ?: ChatCrypto.decodeConversationName(name),
            type = type,
            currentKeyVersion = currentKeyVersion,
            lastMessageAt = lastMessageAt,
            participants = participants
        )
    }

    private suspend fun currentUserId(): String {
        return secureStorage.getUserInfo()?.id ?: userService.me(authHeader()).id
    }

    private fun authHeader(): String {
        val token = AuthSessionState.getAccessToken() ?: secureStorage.getAccessToken()
        val cleanToken = token?.trim()
        check(!cleanToken.isNullOrBlank()) {
            "Thiếu access token ở client. Hãy đăng xuất rồi đăng nhập lại."
        }
        return if (cleanToken.startsWith("Bearer ", ignoreCase = true)) cleanToken else "Bearer $cleanToken"
    }

    private fun utcNow(): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        formatter.timeZone = TimeZone.getTimeZone("UTC")
        return formatter.format(Date())
    }

    private fun readableApiError(throwable: Throwable, fallback: String): String {
        if (throwable is HttpException) {
            val rawBody = throwable.response()?.errorBody()?.string()
            val json = rawBody
                ?.let { runCatching { gson.fromJson(it, JsonObject::class.java) }.getOrNull() }
            val serverMessage = json
                ?.get("message")
                ?.takeIf { !it.isJsonNull }
                ?.asString
            return serverMessage ?: "HTTP ${throwable.code()}"
        }
        return throwable.message?.takeIf { it.isNotBlank() } ?: fallback
    }
}
