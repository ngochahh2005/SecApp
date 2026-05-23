package com.example.secapp.data.local.security

import javax.crypto.spec.SecretKeySpec

object ConversationKeyCache {
    private val keys = mutableMapOf<String, SecretKeySpec>()

    fun get(conversationId: String, keyVersion: Int): SecretKeySpec? {
        return synchronized(keys) { keys[cacheKey(conversationId, keyVersion)] }
    }

    fun put(conversationId: String, keyVersion: Int, key: SecretKeySpec) {
        synchronized(keys) { keys[cacheKey(conversationId, keyVersion)] = key }
    }

    fun clear() {
        synchronized(keys) { keys.clear() }
    }

    private fun cacheKey(conversationId: String, keyVersion: Int): String {
        return "$conversationId:$keyVersion"
    }
}
