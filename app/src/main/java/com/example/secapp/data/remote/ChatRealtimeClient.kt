package com.example.secapp.data.remote

import android.content.Context
import com.example.secapp.data.local.security.AuthSessionState
import com.example.secapp.data.local.security.SecureStorage
import com.example.secapp.data.model.dto.RealtimeErrorEvent
import com.example.secapp.data.model.dto.RealtimeMessageEvent
import com.example.secapp.data.model.dto.RealtimeMessageRequest
import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

class ChatRealtimeClient(
    context: Context,
    private val onMessage: (RealtimeMessageEvent) -> Unit,
    private val onError: (String) -> Unit
) {
    private val secureStorage = SecureStorage(context.applicationContext)
    private val client = OkHttpClient.Builder().build()
    private val gson = Gson()
    private var webSocket: WebSocket? = null
    var isConnected: Boolean = false
        private set

    fun connect() {
        val token = AuthSessionState.getAccessToken() ?: secureStorage.getAccessToken()
        if (token.isNullOrBlank()) {
            onError("Thiếu access token để kết nối realtime")
            return
        }

        val request = Request.Builder()
            .url(NetworkConfig.WEB_SOCKET_URL)
            .addHeader("Authorization", "Bearer $token")
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                isConnected = true
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                val error = runCatching { gson.fromJson(text, RealtimeErrorEvent::class.java) }.getOrNull()
                if (error?.type == "error") {
                    onError(error.message ?: "Realtime error")
                    return
                }

                val event = runCatching { gson.fromJson(text, RealtimeMessageEvent::class.java) }.getOrNull()
                if (event?.type == "message.created") {
                    onMessage(event)
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                isConnected = false
                onError(t.message ?: "Mất kết nối realtime")
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                isConnected = false
            }
        })
    }

    fun sendMessage(request: RealtimeMessageRequest): Boolean {
        return webSocket?.send(gson.toJson(request)) == true
    }

    fun close() {
        webSocket?.close(1000, "Screen closed")
        webSocket = null
        isConnected = false
    }
}
