package com.example.secapp.ui.screen.chat

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.secapp.data.repository.ChatRepository
import com.example.secapp.data.repository.ChatResult
import com.example.secapp.data.repository.ConversationItem
import kotlinx.coroutines.launch

private val ChatBackground = Color(0xFFF4F7FB)
private val ChatSurface = Color.White
private val ChatPrimary = Color(0xFF223A5E)
private val ChatAccent = Color(0xFF0F766E)
private val ChatWarning = Color(0xFFFFF7E6)
private val ChatMutedText = Color(0xFF667085)
private val ChatDivider = Color(0xFFE5EAF1)

@Composable
fun ConversationListScreen(
    onOpenConversation: (ConversationItem) -> Unit,
    onCreateConversation: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { ChatRepository(context) }
    val coroutineScope = rememberCoroutineScope()
    var refreshKey by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var conversations by remember { mutableStateOf<List<ConversationItem>>(emptyList()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var pendingDeleteConversation by remember { mutableStateOf<ConversationItem?>(null) }
    var deletingConversationId by remember { mutableStateOf<String?>(null) }

    fun loadConversations() {
        coroutineScope.launch {
            isLoading = true
            errorMessage = null
            when (val result = repository.listConversations()) {
                is ChatResult.Success -> conversations = result.value
                is ChatResult.Failure -> errorMessage = result.message
            }
            isLoading = false
        }
    }

    LaunchedEffect(refreshKey) {
        loadConversations()
    }

    fun deleteConversation(conversation: ConversationItem) {
        coroutineScope.launch {
            deletingConversationId = conversation.id
            errorMessage = null
            when (val result = repository.deleteConversation(conversation.id)) {
                is ChatResult.Success -> {
                    conversations = conversations.filterNot { it.id == conversation.id }
                    pendingDeleteConversation = null
                }
                is ChatResult.Failure -> errorMessage = result.message
            }
            deletingConversationId = null
        }
    }

    pendingDeleteConversation?.let { conversation ->
        AlertDialog(
            onDismissRequest = {
                if (deletingConversationId == null) pendingDeleteConversation = null
            },
            title = { Text("Xóa cuộc trò chuyện?") },
            text = { Text("Cuộc trò chuyện sẽ được xóa khỏi danh sách của bạn.") },
            confirmButton = {
                TextButton(
                    onClick = { deleteConversation(conversation) },
                    enabled = deletingConversationId == null
                ) {
                    Text(if (deletingConversationId != null) "Đang xóa..." else "Xóa")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { pendingDeleteConversation = null },
                    enabled = deletingConversationId == null
                ) {
                    Text("Hủy")
                }
            }
        )
    }

    Scaffold(
        containerColor = ChatBackground,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateConversation,
                containerColor = ChatAccent,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Tạo chat")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            ConversationListHeader(
                conversationCount = conversations.size,
                onLogout = onLogout
            )

            Spacer(modifier = Modifier.height(18.dp))

            errorMessage?.let {
                ErrorBanner(message = it, onRetry = { refreshKey++ })
                Spacer(modifier = Modifier.height(14.dp))
            }

            when {
                isLoading -> {
                    LoadingState()
                }

                errorMessage != null -> {
                    Spacer(modifier = Modifier.weight(1f))
                }

                conversations.isEmpty() -> {
                    EmptyConversationState(onCreateConversation = onCreateConversation)
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 88.dp)
                    ) {
                        items(conversations, key = { it.id }) { conversation ->
                            ConversationRow(
                                conversation = conversation,
                                onClick = { onOpenConversation(conversation) },
                                onDelete = { pendingDeleteConversation = conversation }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConversationListHeader(
    conversationCount: Int,
    onLogout: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Tin nhắn",
                style = MaterialTheme.typography.headlineMedium,
                color = ChatPrimary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "$conversationCount cuộc trò chuyện",
                style = MaterialTheme.typography.bodyMedium,
                color = ChatMutedText
            )
        }
        IconButton(onClick = onLogout) {
            Icon(
                Icons.Default.Logout,
                contentDescription = "Đăng xuất",
                tint = ChatPrimary
            )
        }
    }
}

@Composable
private fun ErrorBanner(message: String, onRetry: () -> Unit) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = ChatWarning),
        border = BorderStroke(1.dp, Color(0xFFF8D9A2)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = message,
                color = Color(0xFF8A4B00),
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = ChatPrimary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Thử lại")
            }
        }
    }
}

@Composable
private fun LoadingState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(color = ChatAccent)
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Đang tải tin nhắn...",
            color = ChatMutedText,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun EmptyConversationState(onCreateConversation: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(Color(0xFFE7F4F1), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Security,
                contentDescription = null,
                tint = ChatAccent,
                modifier = Modifier.size(32.dp)
            )
        }
        Spacer(modifier = Modifier.height(18.dp))
        Text(
            text = "Chưa có cuộc trò chuyện nào",
            style = MaterialTheme.typography.titleMedium,
            color = ChatPrimary,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Bắt đầu một đoạn chat mới",
            style = MaterialTheme.typography.bodyMedium,
            color = ChatMutedText
        )
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = onCreateConversation,
            colors = ButtonDefaults.buttonColors(containerColor = ChatAccent),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("Tạo chat đầu tiên")
        }
    }
}

@Composable
private fun ConversationRow(
    conversation: ConversationItem,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = ChatSurface),
        border = BorderStroke(1.dp, ChatDivider),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(Color(0xFFE7F4F1), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = conversationInitial(conversation.title),
                    color = ChatAccent,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = conversation.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF182230),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    ConversationTypeChip(conversation.type)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = conversation.lastMessageAt.asConversationTime(),
                    style = MaterialTheme.typography.bodySmall,
                    color = ChatMutedText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Key version ${conversation.currentKeyVersion}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF8A94A6)
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Xóa cuộc trò chuyện",
                    tint = Color(0xFFB42318)
                )
            }

            Spacer(modifier = Modifier.width(2.dp))
            Icon(
                Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint = Color(0xFF98A2B3)
            )
        }
    }
}

@Composable
private fun ConversationTypeChip(type: String) {
    Box(
        modifier = Modifier
            .background(Color(0xFFFFF7E6), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = type.toConversationTypeLabel(),
            color = Color(0xFF9A5B00),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun conversationInitial(title: String): String {
    return title.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"
}

private fun String?.asConversationTime(): String {
    return this
        ?.replace('T', ' ')
        ?.take(16)
        ?.takeIf { it.isNotBlank() }
        ?: "Chưa có tin nhắn"
}

private fun String.toConversationTypeLabel(): String {
    return when (uppercase()) {
        "DIRECT" -> "Trực tiếp"
        "GROUP" -> "Nhóm"
        else -> this
    }
}
