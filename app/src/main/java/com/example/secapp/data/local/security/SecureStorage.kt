package com.example.secapp.data.local.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.example.secapp.data.model.dto.MasterKeyResponse
import com.example.secapp.data.model.dto.UserResponse
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class SecureStorage(context: Context) {
    companion object {
        private const val PREFS_NAME = "secapp_secure_prefs"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_SESSION_KEY_ID = "session_key_id"
        private const val KEY_MASTER_PUBLIC_KEY = "master_public_key"
        private const val KEY_ENCRYPTED_PRIVATE_KEY = "encrypted_private_key"
        private const val KEY_PRIVATE_KEY_IV = "private_key_iv"
        private const val KEY_PIN_SALT = "pin_salt"
        private const val KEY_KDF_PARAMS = "kdf_params"
        private const val KEY_MASTER_KEY_ID = "master_key_id"
        private const val KEY_MASTER_STATUS = "master_key_status"
        private const val KEY_USER_INFO = "user_info"
    }

    private val appContext = context.applicationContext
    private val sharedPreferences: SharedPreferences by lazy { createSharedPreferences() }
    private val gson = Gson()

    private fun createSharedPreferences(): SharedPreferences {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        return runCatching {
            EncryptedSharedPreferences.create(
                PREFS_NAME,
                masterKeyAlias,
                appContext,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }.getOrElse {
            appContext.deleteSharedPreferences(PREFS_NAME)
            EncryptedSharedPreferences.create(
                PREFS_NAME,
                masterKeyAlias,
                appContext,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }
    }

    fun storeAuthData(
        accessToken: String,
        refreshToken: String,
        sessionKeyId: String,
        userResponse: UserResponse,
        masterKey: MasterKeyResponse
    ): Boolean {
        return sharedPreferences.edit()
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .putString(KEY_REFRESH_TOKEN, refreshToken)
            .putString(KEY_SESSION_KEY_ID, sessionKeyId)
            .putString(KEY_MASTER_KEY_ID, masterKey.id)
            .putString(KEY_MASTER_PUBLIC_KEY, masterKey.publicKey)
            .putString(KEY_ENCRYPTED_PRIVATE_KEY, masterKey.encryptedPrivateKey)
            .putString(KEY_PRIVATE_KEY_IV, masterKey.privateKeyIv)
            .putString(KEY_PIN_SALT, masterKey.pinSalt)
            .putString(KEY_KDF_PARAMS, gson.toJson(masterKey.kdfParams))
            .putString(KEY_MASTER_STATUS, masterKey.status)
            .putString(KEY_USER_INFO, gson.toJson(userResponse))
            .commit()
    }

    fun getAccessToken(): String? = sharedPreferences.getString(KEY_ACCESS_TOKEN, null)
    fun getRefreshToken(): String? = sharedPreferences.getString(KEY_REFRESH_TOKEN, null)
    fun getSessionKeyId(): String? = sharedPreferences.getString(KEY_SESSION_KEY_ID, null)

    fun getStoredMasterKey(): MasterKeyResponse? {
        val id = sharedPreferences.getString(KEY_MASTER_KEY_ID, null) ?: return null
        val publicKey = sharedPreferences.getString(KEY_MASTER_PUBLIC_KEY, null) ?: return null
        val encryptedPrivateKey = sharedPreferences.getString(KEY_ENCRYPTED_PRIVATE_KEY, null) ?: return null
        val privateKeyIv = sharedPreferences.getString(KEY_PRIVATE_KEY_IV, null) ?: return null
        val pinSalt = sharedPreferences.getString(KEY_PIN_SALT, null) ?: return null
        val kdfParamsJson = sharedPreferences.getString(KEY_KDF_PARAMS, null) ?: return null
        val kdfParamsType = object : TypeToken<Map<String, Any>>() {}.type
        val kdfParams: Map<String, Any> = gson.fromJson(kdfParamsJson, kdfParamsType)
        val status = sharedPreferences.getString(KEY_MASTER_STATUS, null) ?: "UNKNOWN"
        return MasterKeyResponse(id, publicKey, encryptedPrivateKey, privateKeyIv, pinSalt, kdfParams, status, null)
    }

    fun getUserInfo(): UserResponse? {
        val json = sharedPreferences.getString(KEY_USER_INFO, null) ?: return null
        return gson.fromJson(json, UserResponse::class.java)
    }

    fun clear() {
        sharedPreferences.edit().clear().commit()
    }
}
