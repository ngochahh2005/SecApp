package com.example.secapp.data.remote

import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Test

class NetworkTimeoutPolicyTest {
    @Test
    fun restClientTimeoutsAllowColdStartConversationCreationToFinish() {
        val client = NetworkTimeoutPolicy.applyTo(OkHttpClient.Builder()).build()

        assertEquals(30_000, client.connectTimeoutMillis)
        assertEquals(60_000, client.readTimeoutMillis)
        assertEquals(60_000, client.writeTimeoutMillis)
        assertEquals(90_000, client.callTimeoutMillis)
    }
}
