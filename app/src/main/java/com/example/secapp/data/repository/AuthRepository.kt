package com.example.secapp.data.repository

import android.content.Context
import com.example.secapp.data.local.security.CryptoHelper
import com.example.secapp.data.local.security.SecureStorage
import com.example.secapp.data.model.dto.AuthResponse
import com.example.secapp.data.model.dto.LoginRequest
import com.example.secapp.data.model.dto.MasterKeyRequest
import com.example.secapp.data.model.dto.SignupRequest
import com.example.secapp.data.remote.AuthService
import com.example.secapp.data.remote.NetworkConfig
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.security.KeyFactory
import java.security.KeyPair
import java.security.PrivateKey
import java.security.spec.PKCS8EncodedKeySpec

sealed class AuthResult<out T> {
    data class Success<out T>(val value: T) : AuthResult<T>()
    data class Failure(val message: String) : AuthResult<Nothing>()
}

class AuthRepository(context: Context) {
    private val authService: AuthService = NetworkConfig.authService
    private val secureStorage = SecureStorage(context.applicationContext)
    private val gson = Gson()
    private var sessionKeyPair: KeyPair? = null
    private var unlockedMasterPrivateKey: PrivateKey? = null

    suspend fun signup(
        username: String,
        email: String,
        password: String,
        confirmPassword: String,
        pin: String
    ): AuthResult<Unit> = withContext(Dispatchers.IO) {
        if (username.isBlank() || email.isBlank() || password.isBlank() || confirmPassword.isBlank() || pin.isBlank()) {
            return@withContext AuthResult.Failure("Vui lòng nhập đầy đủ thông tin")
        }
        if (password != confirmPassword) {
            return@withContext AuthResult.Failure("Mật khẩu và xác nhận mật khẩu không khớp")
        }
        if (!pin.matches(Regex("\\d{6}"))) {
            return@withContext AuthResult.Failure("PIN phải gồm đúng 6 số")
        }

        return@withContext try {
            val keyPair = CryptoHelper.generateRsaKeyPair()
            val pinSalt = CryptoHelper.generateRandomBytes(16)
            val kdfParams = mapOf(
                "algorithm" to "PBKDF2WithHmacSHA256",
                "iterations" to 20000,
                "keyLength" to 256
            )
            val aesKey = CryptoHelper.deriveAesKeyFromPin(pin, pinSalt, 20000, 256)
            val iv = CryptoHelper.generateRandomBytes(12)
            val encryptedPrivateKey = CryptoHelper.encryptAesGcm(keyPair.private.encoded, aesKey, iv)
            val masterKeyRequest = MasterKeyRequest(
                publicKey = CryptoHelper.publicKeyToBase64(keyPair.public),
                encryptedPrivateKey = CryptoHelper.toBase64(encryptedPrivateKey),
                privateKeyIv = CryptoHelper.toBase64(iv),
                pinSalt = CryptoHelper.toBase64(pinSalt),
                kdfParams = kdfParams
            )
            val request = SignupRequest(
                username = username,
                email = email,
                displayName = username,
                password = password,
                confirmPassword = confirmPassword,
                masterKey = masterKeyRequest
            )
            authService.signup(request)
            AuthResult.Success(Unit)
        } catch (throwable: Throwable) {
            AuthResult.Failure(readableApiError(throwable, "Đăng ký thất bại"))
        }
    }

    suspend fun login(usernameOrEmail: String, password: String, deviceInfo: String): AuthResult<Unit> = withContext(Dispatchers.IO) {
        if (usernameOrEmail.isBlank() || password.isBlank()) {
            return@withContext AuthResult.Failure("Tên đăng nhập và mật khẩu không được để trống")
        }

        return@withContext try {
            val keyPair = CryptoHelper.generateRsaKeyPair()
            sessionKeyPair = keyPair
            val request = LoginRequest(
                usernameOrEmail = usernameOrEmail,
                password = password,
                sessionPublicKey = CryptoHelper.publicKeyToBase64(keyPair.public),
                deviceInfo = deviceInfo
            )
            val response: AuthResponse = authService.login(request)
            val refreshToken = CryptoHelper.decryptRsaOaep(response.encryptedRefreshToken, keyPair.private)
            secureStorage.storeAuthData(
                accessToken = response.accessToken,
                refreshToken = refreshToken,
                sessionKeyId = response.sessionKeyId,
                userResponse = response.user,
                masterKey = response.masterKey
            )
            AuthResult.Success(Unit)
        } catch (throwable: Throwable) {
            val message = throwable.message.orEmpty()
            val readableMessage = if (
                message.contains("OAEP", ignoreCase = true) ||
                message.contains("padding", ignoreCase = true) ||
                message.contains("BadPadding", ignoreCase = true)
            ) {
                "Không thể giải mã phiên đăng nhập. Vui lòng thử đăng nhập lại."
            } else {
                readableApiError(throwable, "Đăng nhập thất bại")
            }
            AuthResult.Failure(readableMessage)
        }
    }

    suspend fun unlockMasterPrivateKey(pin: String): AuthResult<PrivateKey> = withContext(Dispatchers.IO) {
        val masterKey = secureStorage.getStoredMasterKey()
            ?: return@withContext AuthResult.Failure("Không tìm thấy thông tin master key")
        if (pin.isBlank()) {
            return@withContext AuthResult.Failure("Vui lòng nhập PIN")
        }
        return@withContext try {
            val salt = CryptoHelper.fromBase64(masterKey.pinSalt)
            val params = masterKey.kdfParams
            val iterations = (params["iterations"] as? Number)?.toInt()
                ?: (params["iterations"] as? String)?.toIntOrNull()
                ?: 20000
            val keyLength = (params["keyLength"] as? Number)?.toInt()
                ?: (params["keyLength"] as? String)?.toIntOrNull()
                ?: 256
            val aesKey = CryptoHelper.deriveAesKeyFromPin(pin, salt, iterations, keyLength)
            val iv = CryptoHelper.fromBase64(masterKey.privateKeyIv)
            val ciphered = CryptoHelper.fromBase64(masterKey.encryptedPrivateKey)
            val decryptedBytes = CryptoHelper.decryptAesGcm(ciphered, aesKey, iv)
            val keyFactory = KeyFactory.getInstance("RSA")
            val privateKey = keyFactory.generatePrivate(PKCS8EncodedKeySpec(decryptedBytes))
            unlockedMasterPrivateKey = privateKey
            AuthResult.Success(privateKey)
        } catch (throwable: Throwable) {
            AuthResult.Failure("PIN không đúng hoặc dữ liệu đã bị thay đổi")
        }
    }

    fun getCurrentUserDisplayName(): String? = secureStorage.getUserInfo()?.displayName

    fun clearSession() {
        sessionKeyPair = null
        unlockedMasterPrivateKey = null
        secureStorage.clear()
    }

    private fun readableApiError(throwable: Throwable, fallback: String): String {
        if (throwable is HttpException) {
            val rawBody = throwable.response()?.errorBody()?.string()
            val json = rawBody
                ?.let { runCatching { gson.fromJson(it, JsonObject::class.java) }.getOrNull() }

            val fieldErrors = json
                ?.getAsJsonObject("fieldErrors")
                ?.entrySet()
                ?.map { (field, error) -> "${field.displayName()}: ${error.asString.toVietnameseValidationMessage()}" }
                ?.takeIf { it.isNotEmpty() }

            if (fieldErrors != null) {
                return fieldErrors.joinToString("\n")
            }

            val serverMessage = json
                ?.get("message")
                ?.takeIf { !it.isJsonNull }
                ?.asString

            return when (serverMessage) {
                "Email already exists" -> "Email đã tồn tại"
                "Username already exists" -> "Tên đăng nhập đã tồn tại"
                null -> "HTTP ${throwable.code()}"
                else -> serverMessage
            }
        }

        return throwable.message?.takeIf { it.isNotBlank() } ?: fallback
    }

    private fun String.displayName(): String = when (this) {
        "password" -> "Mật khẩu"
        "confirmPassword" -> "Xác nhận mật khẩu"
        "email" -> "Email"
        "username" -> "Tên đăng nhập"
        else -> this
    }

    private fun String.toVietnameseValidationMessage(): String = when {
        this == "size must be between 8 and 128" -> "phải có từ 8 đến 128 ký tự"
        this.contains("must not be blank", ignoreCase = true) -> "không được để trống"
        else -> this
    }
}
