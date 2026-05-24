package com.example.secapp.ui.screen.chat

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatSendUiPolicyTest {
    @Test
    fun historyIsLoadedOnlyAfterMasterKeyUnlock() {
        assertFalse(ChatSendUiPolicy.shouldLoadHistory(hasUnlockedMasterKey = false))
        assertTrue(ChatSendUiPolicy.shouldLoadHistory(hasUnlockedMasterKey = true))
    }

    @Test
    fun restFallbackDoesNotReloadLockedHistoryAfterSend() {
        assertFalse(ChatSendUiPolicy.shouldReloadMessagesAfterSend(sentRealtime = false))
        assertFalse(ChatSendUiPolicy.shouldReloadMessagesAfterSend(sentRealtime = true))
    }
}
