package com.example.secapp.data.repository

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatKeyAccessPolicyTest {
    @Test
    fun sendingSkipsExistingKeyProbeWhenMasterKeyIsLocked() {
        assertFalse(ChatKeyAccessPolicy.shouldProbeExistingKeyBeforeSending(hasUnlockedMasterKey = false))
        assertTrue(ChatKeyAccessPolicy.shouldProbeExistingKeyBeforeSending(hasUnlockedMasterKey = true))
    }
}
