package com.example.secapp.ui.screen.chat

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image as ComposeImage
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.secapp.data.repository.ChatAttachmentDraft
import com.example.secapp.data.repository.ChatAttachmentItem
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
import java.io.ByteArrayOutputStream
import java.util.UUID
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
    var attachmentDrafts by remember { mutableStateOf<List<ChatAttachmentDraft>>(emptyList()) }
    var editingMessage by remember { mutableStateOf<ChatMessageItem?>(null) }
    var pendingDeleteMessage by remember { mutableStateOf<ChatMessageItem?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isSending by remember { mutableStateOf(false) }
    var isDeletingMessage by remember { mutableStateOf(false) }
    var isRotatingKey by remember { mutableStateOf(false) }
    var isUnlockingPin by remember { mutableStateOf(false) }
    var hasUnlockedMasterKey by remember { mutableStateOf(SessionCryptoState.getMasterPrivateKey() != null) }
    var showPinUnlock by remember { mutableStateOf(false) }
    var pin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var realtimeClient by remember { mutableStateOf<ChatRealtimeClient?>(null) }
    val attachmentPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        val availableSlots = ChatAttachmentPolicy.MAX_ATTACHMENTS - attachmentDrafts.size
        if (availableSlots <= 0) {
            errorMessage = "Bạn chỉ có thể gửi tối đa ${ChatAttachmentPolicy.MAX_ATTACHMENTS} file mỗi lần."
            return@rememberLauncherForActivityResult
        }

        val picked = uris.take(availableSlots).mapNotNull { uri ->
            when (val result = context.readAttachmentDraft(uri)) {
                is AttachmentPickResult.Success -> result.draft
                is AttachmentPickResult.Failure -> {
                    errorMessage = result.message
                    null
                }
            }
        }
        if (picked.isNotEmpty()) {
            attachmentDrafts = attachmentDrafts + picked
            errorMessage = null
        }
    }

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

    fun startEdit(message: ChatMessageItem) {
        if (!ChatMessageActionPolicy.canEdit(message.isMine, message.canDecrypt)) return
        editingMessage = message
        input = message.content
        attachmentDrafts = emptyList()
        errorMessage = null
        statusMessage = null
    }

    fun cancelEdit() {
        editingMessage = null
        input = ""
        errorMessage = null
    }

    fun deleteSelectedMessage() {
        val message = pendingDeleteMessage ?: return
        if (isDeletingMessage) return
        coroutineScope.launch {
            isDeletingMessage = true
            errorMessage = null
            when (val result = repository.deleteMessage(conversationId, message.id)) {
                is ChatResult.Success -> {
                    messages = messages.filterNot { it.id == message.id }
                    pendingDeleteMessage = null
                    statusMessage = "Đã xóa tin nhắn"
                }
                is ChatResult.Failure -> errorMessage = result.message
            }
            isDeletingMessage = false
        }
    }

    fun submitEdit(message: ChatMessageItem, updatedText: String) {
        if (isSending) return
        isSending = true
        coroutineScope.launch {
            try {
                errorMessage = null
                when (val result = repository.editMessage(conversationId, message, updatedText)) {
                    is ChatResult.Success -> {
                        messages = messages.map { if (it.id == message.id) result.value else it }
                        editingMessage = null
                        input = ""
                        statusMessage = "Đã cập nhật tin nhắn"
                    }
                    is ChatResult.Failure -> errorMessage = result.message
                }
            } finally {
                isSending = false
            }
        }
    }

    fun sendMessage() {
        editingMessage?.let { message ->
            submitEdit(message, input)
            return
        }

        val text = input
        val drafts = attachmentDrafts
        if (!ChatSendUiPolicy.canStartSend(text, drafts.size, isSending)) return
        isSending = true
        coroutineScope.launch {
            try {
                errorMessage = null
                when (val realtimeRequest = repository.buildRealtimeMessageRequest(conversationId, activeKeyVersion, text, drafts)) {
                    is ChatResult.Success -> {
                        activeKeyVersion = realtimeRequest.value.keyVersion
                        val sentRealtime = if (ChatSendUiPolicy.shouldAttemptRealtime(realtimeClient?.isConnected == true)) {
                            realtimeClient?.sendMessage(realtimeRequest.value) == true
                        } else {
                            false
                        }
                        if (sentRealtime) {
                            input = ""
                            attachmentDrafts = emptyList()
                        } else {
                            when (val restResult = repository.sendMessage(conversationId, activeKeyVersion, text, drafts)) {
                                is ChatResult.Success -> {
                                    input = ""
                                    attachmentDrafts = emptyList()
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

    pendingDeleteMessage?.let {
        AlertDialog(
            onDismissRequest = { if (!isDeletingMessage) pendingDeleteMessage = null },
            title = { Text("Xóa tin nhắn?") },
            text = { Text("Tin nhắn sẽ được xóa khỏi cuộc trò chuyện của bạn.") },
            confirmButton = {
                TextButton(
                    onClick = { deleteSelectedMessage() },
                    enabled = !isDeletingMessage
                ) {
                    Text(if (isDeletingMessage) "Đang xóa..." else "Xóa")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { pendingDeleteMessage = null },
                    enabled = !isDeletingMessage
                ) {
                    Text("Hủy")
                }
            }
        )
    }

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

        editingMessage?.let {
            EditModeBanner(
                onCancel = { cancelEdit() }
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
                        MessageBubble(
                            message = message,
                            onEdit = { startEdit(message) },
                            onDelete = { pendingDeleteMessage = message }
                        )
                    }
                }
            }
        }

        MessageInputBar(
            input = input,
            attachments = attachmentDrafts,
            isSending = isSending,
            isEditing = editingMessage != null,
            onInputChange = { input = it },
            onAttach = { attachmentPicker.launch("*/*") },
            onRemoveAttachment = { draftId ->
                attachmentDrafts = attachmentDrafts.filterNot { it.id == draftId }
            },
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
private fun EditModeBanner(onCancel: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .padding(bottom = 10.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE7F4F1)),
        border = BorderStroke(1.dp, Color(0xFFB7E4DA)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Edit,
                contentDescription = null,
                tint = ChatAccent,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Đang sửa tin nhắn",
                color = ChatPrimary,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onCancel) {
                Text("Hủy")
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
    attachments: List<ChatAttachmentDraft>,
    isSending: Boolean,
    isEditing: Boolean,
    onInputChange: (String) -> Unit,
    onAttach: () -> Unit,
    onRemoveAttachment: (String) -> Unit,
    onSend: () -> Unit
) {
    val canSend = ChatSendUiPolicy.canStartSend(input, attachments.size, isSending)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(ChatSurface)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        if (attachments.isNotEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 10.dp)
            ) {
                items(attachments, key = { it.id }) { attachment ->
                    AttachmentDraftChip(
                        attachment = attachment,
                        enabled = !isSending,
                        onRemove = { onRemoveAttachment(attachment.id) }
                    )
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onAttach,
                enabled = !isSending && !isEditing && ChatAttachmentPolicy.canAttachMore(attachments.size),
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF2F4F7))
            ) {
                Icon(
                    Icons.Default.AttachFile,
                    contentDescription = "Đính kèm file",
                    tint = if (!isEditing) ChatPrimary else Color(0xFF98A2B3)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            OutlinedTextField(
                value = input,
                onValueChange = onInputChange,
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(if (isEditing) "Cập nhật tin nhắn" else "Nhập tin nhắn")
                },
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
                    contentDescription = if (isEditing) "Cập nhật" else "Gửi",
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
private fun AttachmentDraftChip(
    attachment: ChatAttachmentDraft,
    enabled: Boolean,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .background(Color(0xFFF2F4F7), RoundedCornerShape(8.dp))
            .padding(start = 10.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            iconForMimeType(attachment.mimeType),
            contentDescription = null,
            tint = ChatAccent,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Column(modifier = Modifier.width(150.dp)) {
            Text(
                text = attachment.displayName,
                color = ChatPrimary,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = attachment.sizeBytes.formatFileSize(),
                color = ChatMutedText,
                style = MaterialTheme.typography.labelSmall
            )
        }
        IconButton(
            onClick = onRemove,
            enabled = enabled,
            modifier = Modifier.size(30.dp)
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Bỏ file",
                tint = ChatMutedText,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun MessageBubble(
    message: ChatMessageItem,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val messageTime = message.createdAt?.takeIf { it.isNotBlank() }?.asMessageTime()
    val editText = if (message.isEdited) "đã sửa" else null
    val metaText = listOfNotNull("Key v${message.keyVersion}", messageTime, editText).joinToString(" • ")
    var showMenu by remember { mutableStateOf(false) }
    val canEdit = ChatMessageActionPolicy.canEdit(message.isMine, message.canDecrypt)
    val canDelete = ChatMessageActionPolicy.canDelete(message.isMine)

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
                Row(verticalAlignment = Alignment.Top) {
                    Column(modifier = Modifier.weight(1f)) {
                        if (message.attachments.isNotEmpty()) {
                            MessageAttachments(
                                attachments = message.attachments,
                                isMine = message.isMine
                            )
                            if (message.content.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }

                        if (message.content.isNotBlank() || message.attachments.isEmpty()) {
                            Text(
                                text = message.content.ifBlank { "Đã gửi file" },
                                color = if (message.isMine) Color.White else Color(0xFF182230),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    if (canEdit || canDelete) {
                        Box {
                            IconButton(
                                onClick = { showMenu = true },
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(
                                    Icons.Default.MoreVert,
                                    contentDescription = "Tùy chọn tin nhắn",
                                    tint = if (message.isMine) Color(0xFFD7E3FF) else ChatMutedText,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                if (canEdit) {
                                    DropdownMenuItem(
                                        text = { Text("Sửa") },
                                        leadingIcon = {
                                            Icon(Icons.Default.Edit, contentDescription = null)
                                        },
                                        onClick = {
                                            showMenu = false
                                            onEdit()
                                        }
                                    )
                                }
                                if (canDelete) {
                                    DropdownMenuItem(
                                        text = { Text("Xóa") },
                                        leadingIcon = {
                                            Icon(Icons.Default.Delete, contentDescription = null)
                                        },
                                        onClick = {
                                            showMenu = false
                                            onDelete()
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

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

@Composable
private fun MessageAttachments(
    attachments: List<ChatAttachmentItem>,
    isMine: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        attachments.forEach { attachment ->
            if (attachment.mimeType.startsWith("image/", ignoreCase = true)) {
                val bitmap = remember(attachment.base64Data) {
                    runCatching {
                        val bytes = Base64.decode(attachment.base64Data, Base64.DEFAULT)
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    }.getOrNull()
                }
                if (bitmap != null) {
                    ComposeImage(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = attachment.displayName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                } else {
                    AttachmentFileCard(attachment = attachment, isMine = isMine)
                }
            } else {
                AttachmentFileCard(attachment = attachment, isMine = isMine)
            }
        }
    }
}

@Composable
private fun AttachmentFileCard(
    attachment: ChatAttachmentItem,
    isMine: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = if (isMine) Color(0x1AFFFFFF) else Color(0xFFF2F4F7),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(
                    color = if (isMine) Color(0x26FFFFFF) else Color(0xFFE7F4F1),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                iconForMimeType(attachment.mimeType),
                contentDescription = null,
                tint = if (isMine) Color.White else ChatAccent,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = attachment.displayName,
                color = if (isMine) Color.White else ChatPrimary,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = attachment.sizeBytes.formatFileSize(),
                color = if (isMine) Color(0xFFD7E3FF) else ChatMutedText,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

private fun String.asMessageTime(): String {
    return replace('T', ' ').take(16)
}

private fun iconForMimeType(mimeType: String): ImageVector {
    return when {
        mimeType.startsWith("image/", ignoreCase = true) -> Icons.Default.Image
        mimeType.startsWith("video/", ignoreCase = true) -> Icons.Default.Movie
        mimeType.startsWith("audio/", ignoreCase = true) -> Icons.Default.AudioFile
        else -> Icons.Default.Description
    }
}

private fun Long.formatFileSize(): String {
    if (this < 1024) return "$this B"
    val kb = this / 1024.0
    if (kb < 1024) return String.format("%.1f KB", kb)
    return String.format("%.1f MB", kb / 1024.0)
}

private sealed class AttachmentPickResult {
    data class Success(val draft: ChatAttachmentDraft) : AttachmentPickResult()
    data class Failure(val message: String) : AttachmentPickResult()
}

private fun Context.readAttachmentDraft(uri: Uri): AttachmentPickResult {
    val resolver = contentResolver
    val mimeType = resolver.getType(uri) ?: "application/octet-stream"
    val displayName = resolver.query(uri, null, null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (cursor.moveToFirst() && nameIndex >= 0) cursor.getString(nameIndex) else null
    } ?: "media-${System.currentTimeMillis()}"

    val bytes = runCatching {
        resolver.openInputStream(uri)?.use { input ->
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            var total = 0L
            while (true) {
                val read = input.read(buffer)
                if (read == -1) break
                total += read
                if (total > ChatAttachmentPolicy.MAX_ATTACHMENT_BYTES) {
                    throw IllegalArgumentException("File vượt quá giới hạn")
                }
                output.write(buffer, 0, read)
            }
            output.toByteArray()
        } ?: ByteArray(0)
    }.getOrElse {
        return AttachmentPickResult.Failure("Không đọc được file $displayName")
    }

    if (!ChatAttachmentPolicy.canAcceptFile(bytes.size.toLong())) {
        return AttachmentPickResult.Failure("File $displayName vượt quá giới hạn 2 MB")
    }

    return AttachmentPickResult.Success(
        ChatAttachmentDraft(
            id = UUID.randomUUID().toString(),
            displayName = displayName,
            mimeType = mimeType,
            sizeBytes = bytes.size.toLong(),
            base64Data = Base64.encodeToString(bytes, Base64.NO_WRAP)
        )
    )
}
