package com.example.secapp.ui.screen.chat

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.secapp.data.local.security.SessionCryptoState
import com.example.secapp.data.remote.ChatRealtimeClient
import com.example.secapp.data.repository.AuthRepository
import com.example.secapp.data.repository.AuthResult
import com.example.secapp.data.repository.ChatKeyRotationPolicy
import com.example.secapp.data.repository.ChatMessageItem
import com.example.secapp.data.repository.ChatRepository
import com.example.secapp.data.repository.ChatResult
import com.example.secapp.ui.components.PinCodeInput
import com.example.secapp.ui.screen.auth.PinUnlockAction
import com.example.secapp.ui.screen.auth.PinUnlockPolicy
import kotlinx.coroutines.launch

private val ChatBackground = Color(0xFFF4F7FB)
private val ChatSurface = Color.White
private val ChatPrimary = Color(0xFF223A5E)
private val ChatAccent = Color(0xFF0F766E)
private val ChatMutedText = Color(0xFF667085)
private val ChatDivider = Color(0xFFE5EAF1)
private val ChatInfoBackground = Color(0xFFFFF7E6)

@Composable
fun ChatDetailScreen(
    conversationId: String,
    keyVersion: Int,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { ChatRepository(context) }
    val authRepository = remember { AuthRepository(context) }
    val coroutineScope = rememberCoroutineScope()
    val messageListState = rememberLazyListState()
    var activeKeyVersion by remember(conversationId, keyVersion) { mutableIntStateOf(keyVersion) }
    var messages by remember { mutableStateOf<List<ChatMessageItem>>(emptyList()) }
    var input by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var isSending by remember { mutableStateOf(false) }
    var isRotatingKey by remember { mutableStateOf(false) }
    var isUnlockingPin by remember { mutableStateOf(false) }
    var hasUnlockedMasterKey by remember { mutableStateOf(SessionCryptoState.getMasterPrivateKey() != null) }
    var showPinUnlock by remember { mutableStateOf(false) }
    var pin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var realtimeClient by remember { mutableStateOf<ChatRealtimeClient?>(null) }

    fun loadMessages() {
        if (!ChatSendUiPolicy.shouldLoadHistory(hasUnlockedMasterKey)) {
            messages = emptyList()
            isLoading = false
            errorMessage = null
            return
        }

        coroutineScope.launch {
            isLoading = true
            errorMessage = null
            when (val result = repository.getMessages(conversationId)) {
                is ChatResult.Success -> messages = result.value
                is ChatResult.Failure -> errorMessage = result.message
            }
            isLoading = false
        }
    }

    fun rotateKeyManually() {
        if (isRotatingKey) return
        coroutineScope.launch {
            isRotatingKey = true
            errorMessage = null
            statusMessage = null
            try {
                when (val result = repository.rotateConversationKey(conversationId, activeKeyVersion)) {
                    is ChatResult.Success -> {
                        activeKeyVersion = result.value
                        statusMessage = "Đã rotate khóa sang version ${result.value}"
                        loadMessages()
                    }
                    is ChatResult.Failure -> errorMessage = result.message
                }
            } finally {
                isRotatingKey = false
            }
        }
    }

    fun sendMessage() {
        val text = input
        if (!ChatSendUiPolicy.canStartSend(text, isSending)) return
        isSending = true
        coroutineScope.launch {
            try {
                errorMessage = null
                when (val realtimeRequest = repository.buildRealtimeMessageRequest(conversationId, activeKeyVersion, text)) {
                    is ChatResult.Success -> {
                        activeKeyVersion = realtimeRequest.value.keyVersion
                        val sentRealtime = if (ChatSendUiPolicy.shouldAttemptRealtime(realtimeClient?.isConnected == true)) {
                            realtimeClient?.sendMessage(realtimeRequest.value) == true
                        } else {
                            false
                        }
                        if (sentRealtime) {
                            input = ""
                        } else {
                            when (val restResult = repository.sendTextMessage(conversationId, activeKeyVersion, text)) {
                                is ChatResult.Success -> {
                                    input = ""
                                    messages = (messages + restResult.value).distinctBy { it.id }
                                }
                                is ChatResult.Failure -> errorMessage = restResult.message
                            }
                        }
                    }
                    is ChatResult.Failure -> errorMessage = realtimeRequest.message
                }
            } finally {
                isSending = false
            }
        }
    }

    fun unlockHistoryWithPin() {
        when (val action = PinUnlockPolicy.resolveSubmitAction(pin)) {
            PinUnlockAction.ContinueWithoutMasterKey -> {
                errorMessage = "Vui lòng nhập PIN để khôi phục lịch sử."
                return
            }
            PinUnlockAction.AttemptUnlock -> Unit
            is PinUnlockAction.Reject -> {
                errorMessage = action.message
                return
            }
        }

        coroutineScope.launch {
            isUnlockingPin = true
            errorMessage = null
            when (val result = authRepository.unlockMasterPrivateKey(pin)) {
                is AuthResult.Success -> {
                    pin = ""
                    showPinUnlock = false
                    hasUnlockedMasterKey = true
                    loadMessages()
                }
                is AuthResult.Failure -> errorMessage = result.message
            }
            isUnlockingPin = false
        }
    }

    LaunchedEffect(conversationId) {
        loadMessages()
    }

    LaunchedEffect(isLoading, messages.size) {
        if (!isLoading) {
            ChatSendUiPolicy.messageScrollTarget(messages.size)?.let { target ->
                messageListState.scrollToItem(target)
            }
        }
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
                    errorMessage = null
                }
            },
            onError = { message ->
                coroutineScope.launch { errorMessage = message }
            },
            onMemberLeft = memberLeft@ { event ->
                val data = event.data ?: return@memberLeft
                if (data.conversationId != conversationId) return@memberLeft
                coroutineScope.launch {
                    when (val result = repository.rotateConversationKey(
                        conversationId = conversationId,
                        requestedKeyVersion = activeKeyVersion,
                        reason = ChatKeyRotationPolicy.MEMBER_LEFT
                    )) {
                        is ChatResult.Success -> {
                            activeKeyVersion = result.value
                            statusMessage = "Thành viên rời nhóm, đã rotate khóa sang version ${result.value}"
                            loadMessages()
                        }
                        is ChatResult.Failure -> {
                            if (!result.message.contains("Only owner", ignoreCase = true)) {
                                errorMessage = result.message
                            }
                        }
                    }
                }
            }
        )
        realtimeClient = client
        client.connect()
        onDispose {
            client.close()
            realtimeClient = null
        }
    }

    val showPinRecovery = ChatSendUiPolicy.shouldShowPinRecovery(hasUnlockedMasterKey)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ChatBackground)
    ) {
        ChatHeader(
            activeKeyVersion = activeKeyVersion,
            isRotatingKey = isRotatingKey,
            onBack = onBack,
            onRefresh = { loadMessages() },
            onRotateKey = { rotateKeyManually() }
        )

        if (showPinRecovery) {
            PinRecoveryBanner(
                showPinUnlock = showPinUnlock,
                onTogglePinUnlock = { showPinUnlock = !showPinUnlock }
            )
        }

        if (showPinRecovery && showPinUnlock) {
            PinRecoveryPanel(
                pin = pin,
                isUnlockingPin = isUnlockingPin,
                onPinChange = {
                    pin = it
                    errorMessage = null
                },
                onSubmit = { unlockHistoryWithPin() }
            )
        }

        errorMessage?.let {
            Spacer(modifier = Modifier.height(10.dp))
            ErrorMessage(message = it)
        }

        statusMessage?.let {
            Spacer(modifier = Modifier.height(10.dp))
            StatusMessage(message = it)
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when {
                isLoading -> LoadingMessages()
                messages.isEmpty() -> EmptyMessageState()
                else -> LazyColumn(
                    state = messageListState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 16.dp)
                ) {
                    items(messages, key = { it.id }) { message ->
                        MessageBubble(message)
                    }
                }
            }
        }

        MessageInputBar(
            input = input,
            isSending = isSending,
            onInputChange = { input = it },
            onSend = { sendMessage() }
        )
    }
}

@Composable
private fun ChatHeader(
    activeKeyVersion: Int,
    isRotatingKey: Boolean,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onRotateKey: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(ChatSurface)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Color(0xFFE7F4F1))
        ) {
            Icon(
                Icons.Default.ArrowBack,
                contentDescription = "Quay lại",
                tint = ChatAccent
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Chat",
                style = MaterialTheme.typography.titleLarge,
                color = ChatPrimary,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "Key version $activeKeyVersion",
                style = MaterialTheme.typography.bodySmall,
                color = ChatMutedText
            )
        }

        IconButton(
            onClick = onRotateKey,
            enabled = !isRotatingKey,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Color(0xFFF2F4F7))
        ) {
            if (isRotatingKey) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = ChatAccent
                )
            } else {
                Icon(
                    Icons.Default.Security,
                    contentDescription = "Rotate key",
                    tint = ChatPrimary
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        IconButton(
            onClick = onRefresh,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Color(0xFFF2F4F7))
        ) {
            Icon(
                Icons.Default.Refresh,
                contentDescription = "Tải lại",
                tint = ChatPrimary
            )
        }
    }
}

@Composable
private fun PinRecoveryBanner(
    showPinUnlock: Boolean,
    onTogglePinUnlock: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = ChatInfoBackground),
        border = BorderStroke(1.dp, Color(0xFFF8D9A2)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Security,
                    contentDescription = null,
                    tint = Color(0xFF9A5B00)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = "Nhập mã PIN để khôi phục lịch sử đoạn chat",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF7A4A00),
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Button(
                onClick = onTogglePinUnlock,
                colors = ButtonDefaults.buttonColors(containerColor = ChatPrimary),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(if (showPinUnlock) "Ẩn" else "Nhập PIN")
            }
        }
    }
}

@Composable
private fun PinRecoveryPanel(
    pin: String,
    isUnlockingPin: Boolean,
    onPinChange: (String) -> Unit,
    onSubmit: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .padding(bottom = 10.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = ChatSurface),
        border = BorderStroke(1.dp, ChatDivider),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "Mã PIN gồm 6 số",
                style = MaterialTheme.typography.labelLarge,
                color = ChatPrimary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            PinCodeInput(
                pin = pin,
                onPinChange = onPinChange,
                enabled = !isUnlockingPin,
                onDone = onSubmit
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onSubmit,
                enabled = !isUnlockingPin,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = ChatAccent),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(if (isUnlockingPin) "Đang khôi phục..." else "Khôi phục lịch sử")
            }
        }
    }
}

@Composable
private fun StatusMessage(message: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE7F4F1)),
        border = BorderStroke(1.dp, Color(0xFFB7E4DA)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Text(
            text = message,
            color = Color(0xFF0F766E),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(12.dp)
        )
    }
}

@Composable
private fun ErrorMessage(message: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF1F3)),
        border = BorderStroke(1.dp, Color(0xFFFFCCD5)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Text(
            text = message,
            color = Color(0xFFB42318),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(12.dp)
        )
    }
}

@Composable
private fun LoadingMessages() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(color = ChatAccent)
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Đang tải cuộc trò chuyện...",
            color = ChatMutedText,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun EmptyMessageState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(58.dp)
                .background(Color(0xFFE7F4F1), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Security,
                contentDescription = null,
                tint = ChatAccent,
                modifier = Modifier.size(30.dp)
            )
        }
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = "Chưa có tin nhắn",
            color = ChatPrimary,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Hãy gửi tin nhắn đầu tiên",
            color = ChatMutedText,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun MessageInputBar(
    input: String,
    isSending: Boolean,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit
) {
    val canSend = ChatSendUiPolicy.canStartSend(input, isSending)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(ChatSurface)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = input,
            onValueChange = onInputChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text("Nhập tin nhắn") },
            enabled = !isSending,
            minLines = 1,
            maxLines = 4,
            shape = RoundedCornerShape(8.dp)
        )

        Spacer(modifier = Modifier.width(10.dp))

        IconButton(
            onClick = onSend,
            enabled = canSend,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(if (canSend) ChatAccent else Color(0xFFD0D5DD))
        ) {
            Icon(
                Icons.Default.Send,
                contentDescription = "Gửi",
                tint = Color.White
            )
        }
    }
}

@Composable
private fun MessageBubble(message: ChatMessageItem) {
    val messageTime = message.createdAt?.takeIf { it.isNotBlank() }?.asMessageTime()
    val metaText = listOfNotNull("Key v${message.keyVersion}", messageTime).joinToString(" • ")

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isMine) Arrangement.End else Arrangement.Start
    ) {
        Card(
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (message.isMine) ChatPrimary else ChatSurface
            ),
            border = if (message.isMine) null else BorderStroke(1.dp, ChatDivider),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier.fillMaxWidth(0.82f)
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                Text(
                    text = message.content,
                    color = if (message.isMine) Color.White else Color(0xFF182230),
                    style = MaterialTheme.typography.bodyMedium
                )
                if (!message.canDecrypt) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Cần đúng khóa để đọc",
                        color = if (message.isMine) Color(0xFFFFDAD6) else Color(0xFFB42318),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                metaText.takeIf { it.isNotBlank() }?.let {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = it,
                        color = if (message.isMine) Color(0xFFD7E3FF) else ChatMutedText,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.align(if (message.isMine) Alignment.End else Alignment.Start)
                    )
                }
            }
        }
    }
}

private fun String.asMessageTime(): String {
    return replace('T', ' ').take(16)
}
