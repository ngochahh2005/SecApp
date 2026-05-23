package com.example.secapp.ui.screen.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.secapp.data.remote.ChatRealtimeClient
import com.example.secapp.data.repository.ChatMessageItem
import com.example.secapp.data.repository.ChatRepository
import com.example.secapp.data.repository.ChatResult
import kotlinx.coroutines.launch

@Composable
fun ChatDetailScreen(
    conversationId: String,
    keyVersion: Int,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { ChatRepository(context) }
    val coroutineScope = rememberCoroutineScope()
    var messages by remember { mutableStateOf<List<ChatMessageItem>>(emptyList()) }
    var input by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var realtimeClient by remember { mutableStateOf<ChatRealtimeClient?>(null) }

    fun loadMessages() {
        coroutineScope.launch {
            isLoading = true
            errorMessage = null
            when (val result = repository.getMessages(conversationId, keyVersion)) {
                is ChatResult.Success -> messages = result.value
                is ChatResult.Failure -> errorMessage = result.message
            }
            isLoading = false
        }
    }

    fun sendMessage() {
        val text = input
        if (text.isBlank()) return
        coroutineScope.launch {
            errorMessage = null
            when (val realtimeRequest = repository.buildRealtimeMessageRequest(conversationId, keyVersion, text)) {
                is ChatResult.Success -> {
                    val sentRealtime = realtimeClient?.sendMessage(realtimeRequest.value) == true
                    input = ""
                    if (!sentRealtime) {
                        when (val restResult = repository.sendTextMessage(conversationId, keyVersion, text)) {
                            is ChatResult.Success -> loadMessages()
                            is ChatResult.Failure -> errorMessage = restResult.message
                        }
                    }
                }
                is ChatResult.Failure -> errorMessage = realtimeRequest.message
            }
        }
    }

    LaunchedEffect(conversationId, keyVersion) {
        loadMessages()
    }

    DisposableEffect(conversationId) {
        val client = ChatRealtimeClient(
            context = context,
            onMessage = onMessage@ { event ->
                val message = event.message ?: return@onMessage
                if (message.conversationId != conversationId) return@onMessage
                coroutineScope.launch {
                    val item = repository.decryptIncomingMessage(message)
                    messages = (messages + item).distinctBy { it.id }
                }
            },
            onError = { message ->
                coroutineScope.launch { errorMessage = message }
            }
        )
        realtimeClient = client
        client.connect()
        onDispose {
            client.close()
            realtimeClient = null
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Quay lại")
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("Chat", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(
                    text = "Key version $keyVersion",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF777A82)
                )
            }
            Button(onClick = { loadMessages() }) {
                Text("Tải lại")
            }
        }

        errorMessage?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = it, color = Color(0xFFB00020))
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (isLoading) {
            Column(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages, key = { it.id }) { message ->
                    MessageBubble(message)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Nhập tin nhắn") },
                minLines = 1,
                maxLines = 4
            )
            IconButton(onClick = { sendMessage() }) {
                Icon(Icons.Default.Send, contentDescription = "Gửi")
            }
        }
    }
}

@Composable
private fun MessageBubble(message: ChatMessageItem) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isMine) Arrangement.End else Arrangement.Start
    ) {
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (message.isMine) Color(0xFF283FB1) else Color.White
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth(0.78f)
        ) {
            Column(
                modifier = Modifier
                    .background(if (message.isMine) Color(0xFF283FB1) else Color.White)
                    .padding(14.dp)
            ) {
                Text(
                    text = message.content,
                    color = if (message.isMine) Color.White else Color(0xFF222222)
                )
                if (!message.canDecrypt) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Cần đúng khóa để đọc",
                        color = Color(0xFFB00020),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}
