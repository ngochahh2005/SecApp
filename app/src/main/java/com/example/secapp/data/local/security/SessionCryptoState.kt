package com.example.secapp.data.local.security

import java.security.PrivateKey

object SessionCryptoState {
    @Volatile
    private var sessionPrivateKey: PrivateKey? = null

    @Volatile
    private var masterPrivateKey: PrivateKey? = null

    fun setSessionPrivateKey(privateKey: PrivateKey) {
        sessionPrivateKey = privateKey
    }

    fun setMasterPrivateKey(privateKey: PrivateKey) {
        masterPrivateKey = privateKey
    }

    fun getSessionPrivateKey(): PrivateKey? = sessionPrivateKey

    fun getMasterPrivateKey(): PrivateKey? = masterPrivateKey

    fun clear() {
        sessionPrivateKey = null
        masterPrivateKey = null
        ConversationKeyCache.clear()
    }
}
