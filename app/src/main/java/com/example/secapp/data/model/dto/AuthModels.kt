package com.example.secapp.data.model.dto

import com.google.gson.annotations.SerializedName

data class MasterKeyRequest(
    val publicKey: String,
    val encryptedPrivateKey: String,
    val privateKeyIv: String,
    val pinSalt: String,
    val kdfParams: Map<String, Any>
)

data class SignupRequest(
    val username: String,
    val email: String,
    val displayName: String? = null,
    val password: String,
    val confirmPassword: String,
    val masterKey: MasterKeyRequest
)

data class LoginRequest(
    val usernameOrEmail: String,
    val password: String,
    val sessionPublicKey: String,
    val deviceInfo: String
)

data class MasterKeyResponse(
    val id: String,
    val publicKey: String,
    val encryptedPrivateKey: String,
    val privateKeyIv: String,
    val pinSalt: String,
    val kdfParams: Map<String, Any>,
    val status: String,
    val createdAt: String?
)

data class UserResponse(
    val id: String,
    val email: String,
    val username: String,
    val displayName: String,
    val avatarUrl: String?,
    val role: String,
    val status: String,
    val createdAt: String?,
    val updatedAt: String?
)

data class AuthResponse(
    val accessToken: String,
    val encryptedRefreshToken: String,
    val sessionKeyId: String,
    val user: UserResponse,
    val masterKey: MasterKeyResponse
)
