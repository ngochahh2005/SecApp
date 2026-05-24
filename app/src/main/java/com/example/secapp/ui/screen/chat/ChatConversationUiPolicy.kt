package com.example.secapp.ui.screen.chat

import com.example.secapp.data.repository.ConversationItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

internal enum class MessageDeliveryStatus(val label: String) {
    SENDING("Đang gửi"),
    SENT("Đã gửi"),
    FAILED("Gửi lỗi")
}

internal object ConversationSearchPolicy {
    fun matches(conversation: ConversationItem, query: String): Boolean {
        val normalizedQuery = query.trim().lowercase(Locale.ROOT)
        if (normalizedQuery.isBlank()) return true

        val searchable = buildString {
            append(conversation.title)
            append(' ')
            append(conversation.type)
            append(' ')
            append(conversation.type.toConversationTypeLabel())
            conversation.participants.forEach { participant ->
                append(' ')
                append(participant.displayName)
                append(' ')
                append(participant.username)
                append(' ')
                append(participant.email)
            }
        }.lowercase(Locale.ROOT)

        return searchable.contains(normalizedQuery)
    }
}

internal object ChatTimeFormatter {
    private val outputFormat = SimpleDateFormat("dd/MM HH:mm", Locale.forLanguageTag("vi-VN"))

    fun formatConversationTime(value: String?, timeZone: TimeZone = TimeZone.getDefault()): String {
        return format(value, timeZone) ?: "Chưa có tin nhắn"
    }

    fun format(value: String?, timeZone: TimeZone = TimeZone.getDefault()): String? {
        val date = parseUtcInstant(value) ?: return value?.takeIf { it.isNotBlank() }
        return synchronized(outputFormat) {
            outputFormat.timeZone = timeZone
            outputFormat.format(date)
        }
    }

    private fun parseUtcInstant(value: String?): Date? {
        val normalized = normalizeIsoInstant(value) ?: return null
        val patterns = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'"
        )
        return patterns.firstNotNullOfOrNull { pattern ->
            runCatching {
                SimpleDateFormat(pattern, Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                    isLenient = false
                }.parse(normalized)
            }.getOrNull()
        }
    }

    private fun normalizeIsoInstant(value: String?): String? {
        val trimmed = value?.trim()?.takeIf { it.isNotBlank() } ?: return null
        if (!trimmed.endsWith("Z")) return trimmed
        val dotIndex = trimmed.lastIndexOf('.')
        if (dotIndex < 0) return trimmed

        val fraction = trimmed.substring(dotIndex + 1, trimmed.length - 1)
        if (fraction.isEmpty()) return trimmed
        val millis = fraction.padEnd(3, '0').take(3)
        return trimmed.substring(0, dotIndex + 1) + millis + "Z"
    }
}

internal fun String.toConversationTypeLabel(): String {
    return when (uppercase(Locale.ROOT)) {
        "DIRECT" -> "Trực tiếp"
        "GROUP" -> "Nhóm"
        else -> this
    }
}
