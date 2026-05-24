package com.example.secapp.data.local.security

import javax.crypto.spec.SecretKeySpec

data class CachedConversationKey(
    val keyVersion: Int,
    val key: SecretKeySpec
)

object ConversationKeyCache {
    private val keys = mutableMapOf<String, SecretKeySpec>()

    fun get(conversationId: String, keyVersion: Int): SecretKeySpec? {
        return synchronized(keys) { keys[cacheKey(conversationId, keyVersion)] }
    }

    fun put(conversationId: String, keyVersion: Int, key: SecretKeySpec) {
        synchronized(keys) { keys[cacheKey(conversationId, keyVersion)] = key }
    }

    fun latest(conversationId: String, minimumVersion: Int = 1): CachedConversationKey? {
        return synchronized(keys) {
            keys.entries
                .mapNotNull { (cacheKey, key) ->
                    val (cachedConversationId, keyVersion) = parseCacheKey(cacheKey) ?: return@mapNotNull null
                    if (cachedConversationId == conversationId && keyVersion >= minimumVersion) {
                        CachedConversationKey(keyVersion, key)
                    } else {
                        null
                    }
                }
                .maxByOrNull { it.keyVersion }
        }
    }

    fun latestVersion(conversationId: String): Int? = latest(conversationId)?.keyVersion

    fun clear() {
        synchronized(keys) { keys.clear() }
    }

    private fun cacheKey(conversationId: String, keyVersion: Int): String {
        return "$conversationId:$keyVersion"
    }

    private fun parseCacheKey(cacheKey: String): Pair<String, Int>? {
        val separator = cacheKey.lastIndexOf(':')
        if (separator <= 0 || separator == cacheKey.lastIndex) return null
        val conversationId = cacheKey.substring(0, separator)
        val keyVersion = cacheKey.substring(separator + 1).toIntOrNull() ?: return null
        return conversationId to keyVersion
    }
}