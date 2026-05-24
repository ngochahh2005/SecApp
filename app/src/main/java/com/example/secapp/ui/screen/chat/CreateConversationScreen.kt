package com.example.secapp.ui.screen.chat

import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.secapp.data.model.dto.UserResponse
import com.example.secapp.data.repository.ChatRepository
import com.example.secapp.data.repository.ChatResult
import com.example.secapp.data.repository.ConversationItem
import com.example.secapp.data.repository.ConversationCreationPolicy
import kotlinx.coroutines.launch

@Composable
fun CreateConversationScreen(
    onBack: () -> Unit,
    onConversationCreated: (ConversationItem) -> Unit
) {
    val context = LocalContext.current
    val repository = remember { ChatRepository(context) }
    val coroutineScope = rememberCoroutineScope()
    var keyword by remember { mutableStateOf("") }
    var groupName by remember { mutableStateOf("") }
    var users by remember { mutableStateOf<List<UserResponse>>(emptyList()) }
    var selectedUsers by remember { mutableStateOf<List<UserResponse>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val groupMode = selectedUsers.size > 1
    val canCreate = ConversationCreationPolicy.canCreate(
        selectedParticipantCount = selectedUsers.size,
        groupMode = groupMode,
        groupName = groupName
    )

    fun search() {
        coroutineScope.launch {
            isLoading = true
            errorMessage = null
            when (val result = repository.searchUsers(keyword)) {
                is ChatResult.Success -> users = result.value
                is ChatResult.Failure -> errorMessage = result.message
            }
            isLoading = false
        }
    }

    fun toggleUser(user: UserResponse) {
        selectedUsers = if (selectedUsers.any { it.id == user.id }) {
            selectedUsers.filterNot { it.id == user.id }
        } else {
            selectedUsers + user
        }
        errorMessage = null
    }

    fun createSelectedConversation() {
        coroutineScope.launch {
            isLoading = true
            errorMessage = null
            val result = if (groupMode) {
                repository.createGroupConversation(selectedUsers, groupName)
            } else {
                repository.createDirectConversation(selectedUsers.first())
            }
            when (result) {
                is ChatResult.Success -> onConversationCreated(result.value)
                is ChatResult.Failure -> errorMessage = result.message
            }
            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Quay lại")
            }
            Text(
                text = "Tạo cuộc trò chuyện",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = keyword,
            onValueChange = { keyword = it },
            label = { Text("Tìm username/email") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = { search() },
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isLoading) "Đang xử lý..." else "Tìm người dùng")
        }

        if (selectedUsers.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Đã chọn ${selectedUsers.size} thành viên",
                color = Color(0xFF475467),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium
            )
        }

        if (groupMode) {
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = groupName,
                onValueChange = {
                    groupName = it
                    errorMessage = null
                },
                label = { Text("Tên nhóm") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = { createSelectedConversation() },
            enabled = !isLoading && canCreate,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                when {
                    isLoading -> "Đang xử lý..."
                    groupMode -> "Tạo nhóm"
                    else -> "Tạo chat trực tiếp"
                }
            )
        }

        errorMessage?.let {
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = it, color = Color(0xFFB00020))
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(users, key = { it.id }) { user ->
                    UserRow(
                        user = user,
                        selected = selectedUsers.any { it.id == user.id },
                        onClick = { toggleUser(user) }
                    )
                }
            }
        }
    }
}

@Composable
private fun UserRow(user: UserResponse, selected: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(user.displayName, fontWeight = FontWeight.Bold)
                Text(
                    text = "@${user.username} • ${user.email}",
                    color = Color(0xFF777A82),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Checkbox(checked = selected, onCheckedChange = { onClick() })
        }
    }
}
