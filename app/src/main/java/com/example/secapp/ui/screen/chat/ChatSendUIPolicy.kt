package com.example.secapp.ui.screen.chat

internal object ChatSendUiPolicy {
    fun canStartSend(input: String, isSending: Boolean): Boolean {
        return input.isNotBlank() && !isSending
    }

    fun shouldAttemptRealtime(isRealtimeConnected: Boolean): Boolean {
        return isRealtimeConnected
    }

    fun shouldReloadMessagesAfterSend(sentRealtime: Boolean): Boolean {
        return !sentRealtime
    }

    fun shouldShowPinRecovery(hasUnlockedMasterKey: Boolean): Boolean {
        return !hasUnlockedMasterKey
    }

    fun messageScrollTarget(messagesCount: Int): Int? {
        return if (messagesCount > 0) messagesCount - 1 else null
    }
}