package com.example.secapp.data.local.security

object AuthSessionState {
    @Volatile
    private var accessToken: String? = null

    @Volatile
    private var refreshToken: String? = null

    @Volatile
    private var sessionKeyId: String? = null

    fun setTokens(accessToken: String, refreshToken: String, sessionKeyId: String) {
        this.accessToken = accessToken
        this.refreshToken = refreshToken
        this.sessionKeyId = sessionKeyId
    }

    fun getAccessToken(): String? = accessToken

    fun getRefreshToken(): String? = refreshToken

    fun getSessionKeyId(): String? = sessionKeyId

    fun clear() {
        accessToken = null
        refreshToken = null
        sessionKeyId = null
    }
}
