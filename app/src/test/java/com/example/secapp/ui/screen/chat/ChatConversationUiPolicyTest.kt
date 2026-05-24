package com.example.secapp.ui.screen.chat

import com.example.secapp.data.model.dto.UserResponse
import com.example.secapp.data.repository.ConversationItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.TimeZone

class ChatConversationUiPolicyTest {
    @Test
    fun conversationSearchMatchesTitleTypeAndParticipants() {
        val conversation = ConversationItem(
            id = "conversation-1",
            title = "Nhom ATBM",
            type = "GROUP",
            currentKeyVersion = 3,
            lastMessageAt = null,
            participants = listOf(
                user(id = "user-1", username = "viet", displayName = "Viet Nguyen", email = "viet@example.com"),
                user(id = "user-2", username = "lan", displayName = "Lan Tran", email = "lan@example.com")
            )
        )

        assertTrue(ConversationSearchPolicy.matches(conversation, "atbm"))
        assertTrue(ConversationSearchPolicy.matches(conversation, "nhom"))
        assertTrue(ConversationSearchPolicy.matches(conversation, "lan"))
        assertTrue(ConversationSearchPolicy.matches(conversation, "group"))
        assertFalse(ConversationSearchPolicy.matches(conversation, "finance"))
    }

    @Test
    fun blankConversationSearchKeepsEveryConversationVisible() {
        val conversation = ConversationItem(
            id = "conversation-1",
            title = "Chat voi Lan",
            type = "DIRECT",
            currentKeyVersion = 1,
            lastMessageAt = null,
            participants = listOf(user(id = "user-2", username = "lan", displayName = "Lan Tran", email = "lan@example.com"))
        )

        assertTrue(ConversationSearchPolicy.matches(conversation, " "))
    }

    @Test
    fun utcTimestampIsFormattedInProvidedLocalTimezone() {
        val bangkok = TimeZone.getTimeZone("Asia/Bangkok")

        assertEquals("25/05 09:30", ChatTimeFormatter.format("2026-05-25T02:30:15.000Z", bangkok))
        assertEquals("25/05 09:30", ChatTimeFormatter.format("2026-05-25T02:30:15Z", bangkok))
    }

    @Test
    fun emptyConversationTimestampUsesFallbackText() {
        val bangkok = TimeZone.getTimeZone("Asia/Bangkok")

        assertEquals("Chưa có tin nhắn", ChatTimeFormatter.formatConversationTime(null, bangkok))
        assertEquals("Chưa có tin nhắn", ChatTimeFormatter.formatConversationTime("", bangkok))
    }

    @Test
    fun deliveryStatusExposesStableUserLabels() {
        assertEquals("Đang gửi", MessageDeliveryStatus.SENDING.label)
        assertEquals("Đã gửi", MessageDeliveryStatus.SENT.label)
        assertEquals("Gửi lỗi", MessageDeliveryStatus.FAILED.label)
    }

    private fun user(id: String, username: String, displayName: String, email: String): UserResponse {
        return UserResponse(
            id = id,
            email = email,
            username = username,
            displayName = displayName,
            avatarUrl = null,
            role = "USER",
            status = "ACTIVE",
            createdAt = null,
            updatedAt = null
        )
    }
}
