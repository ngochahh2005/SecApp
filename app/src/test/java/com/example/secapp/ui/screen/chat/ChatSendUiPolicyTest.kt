package com.example.secapp.ui.screen.chat

import org.junit.Assert.assertEquals
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

    @Test
    fun attachmentOnlyMessageCanBeSent() {
        assertTrue(ChatSendUiPolicy.canStartSend(input = "", attachmentCount = 1, isSending = false))
        assertFalse(ChatSendUiPolicy.canStartSend(input = "", attachmentCount = 0, isSending = false))
    }

    @Test
    fun attachmentsAreCappedAndClassifiedByMimeType() {
        assertTrue(ChatAttachmentPolicy.canAttachMore(ChatAttachmentPolicy.MAX_ATTACHMENTS - 1))
        assertFalse(ChatAttachmentPolicy.canAttachMore(ChatAttachmentPolicy.MAX_ATTACHMENTS))

        assertEquals("IMAGE", ChatAttachmentPolicy.messageTypeFor("image/png", hasAttachments = true))
        assertEquals("VIDEO", ChatAttachmentPolicy.messageTypeFor("video/mp4", hasAttachments = true))
        assertEquals("AUDIO", ChatAttachmentPolicy.messageTypeFor("audio/mpeg", hasAttachments = true))
        assertEquals("FILE", ChatAttachmentPolicy.messageTypeFor("application/pdf", hasAttachments = true))
        assertEquals("TEXT", ChatAttachmentPolicy.messageTypeFor(null, hasAttachments = false))
    }

    @Test
    fun messageActionsAreLimitedToOwnedDecryptableMessages() {
        assertTrue(ChatMessageActionPolicy.canEdit(isMine = true, canDecrypt = true))
        assertFalse(ChatMessageActionPolicy.canEdit(isMine = true, canDecrypt = false))
        assertFalse(ChatMessageActionPolicy.canEdit(isMine = false, canDecrypt = true))

        assertTrue(ChatMessageActionPolicy.canDelete(isMine = true))
        assertFalse(ChatMessageActionPolicy.canDelete(isMine = false))
    }
}
