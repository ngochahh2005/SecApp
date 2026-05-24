package com.example.secapp.data.repository

import org.junit.Assert.assertTrue
import org.junit.Test

class ChatKeyAccessPolicyTest {
    @Test
    fun sendingProbesExistingSessionKeyBeforeProvisioningNewVersionWhenMasterKeyIsLocked() {
        assertTrue(ChatKeyAccessPolicy.shouldProbeExistingKeyBeforeSending(hasUnlockedMasterKey = false))
        assertTrue(ChatKeyAccessPolicy.shouldProbeExistingKeyBeforeSending(hasUnlockedMasterKey = true))
    }
}
