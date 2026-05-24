package com.example.secapp.ui.screen.chat

internal object ChatSendUiPolicy {
    fun canStartSend(input: String, isSending: Boolean): Boolean {
        return input.isNotBlank() && !isSending
    }

    fun canStartSend(input: String, attachmentCount: Int, isSending: Boolean): Boolean {
        return (input.isNotBlank() || attachmentCount > 0) && !isSending
    }

    fun shouldAttemptRealtime(isRealtimeConnected: Boolean): Boolean {
        return isRealtimeConnected
    }

    fun shouldReloadMessagesAfterSend(sentRealtime: Boolean): Boolean {
        return false
    }

    fun shouldShowPinRecovery(hasUnlockedMasterKey: Boolean): Boolean {
        return !hasUnlockedMasterKey
    }

    fun shouldLoadHistory(hasUnlockedMasterKey: Boolean): Boolean {
        return hasUnlockedMasterKey
    }

    fun messageScrollTarget(messagesCount: Int): Int? {
        return if (messagesCount > 0) messagesCount - 1 else null
    }
}

internal object ChatAttachmentPolicy {
    const val MAX_ATTACHMENTS = 5
    const val MAX_ATTACHMENT_BYTES = 2L * 1024L * 1024L

    fun canAttachMore(currentCount: Int): Boolean {
        return currentCount < MAX_ATTACHMENTS
    }

    fun canAcceptFile(sizeBytes: Long): Boolean {
        return sizeBytes in 1..MAX_ATTACHMENT_BYTES
    }

    fun messageTypeFor(mimeType: String?, hasAttachments: Boolean): String {
        if (!hasAttachments) return "TEXT"
        val normalized = mimeType.orEmpty().lowercase()
        return when {
            normalized.startsWith("image/") -> "IMAGE"
            normalized.startsWith("video/") -> "VIDEO"
            normalized.startsWith("audio/") -> "AUDIO"
            else -> "FILE"
        }
    }
}

internal object ChatMessageActionPolicy {
    fun canEdit(isMine: Boolean, canDecrypt: Boolean): Boolean {
        return isMine && canDecrypt
    }

    fun canDelete(isMine: Boolean): Boolean {
        return isMine
    }
}
