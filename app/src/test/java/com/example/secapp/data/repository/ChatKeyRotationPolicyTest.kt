package com.example.secapp.data.repository

import org.junit.Assert.assertEquals
import org.junit.Test

class ChatKeyRotationPolicyTest {
    @Test
    fun nextVersionIsGreaterThanRequestedServerAndCachedVersions() {
        val nextVersion = ChatKeyRotationPolicy.nextVersion(
            requestedKeyVersion = 2,
            currentServerVersion = 4,
            latestCachedVersion = 5
        )

        assertEquals(6, nextVersion)
    }

    @Test
    fun nextVersionStartsAfterVersionOneWhenNoCachedKeyExists() {
        val nextVersion = ChatKeyRotationPolicy.nextVersion(
            requestedKeyVersion = 1,
            currentServerVersion = 1,
            latestCachedVersion = null
        )

        assertEquals(2, nextVersion)
    }

    @Test
    fun exposesStableRotationReasonsForApiAuditTrail() {
        assertEquals("MANUAL_SECURITY_ROTATION", ChatKeyRotationPolicy.MANUAL_SECURITY_ROTATION)
        assertEquals("SESSION_WITHOUT_MASTER_KEY", ChatKeyRotationPolicy.SESSION_WITHOUT_MASTER_KEY)
        assertEquals("MEMBER_ADDED", ChatKeyRotationPolicy.MEMBER_ADDED)
        assertEquals("MEMBER_LEFT", ChatKeyRotationPolicy.MEMBER_LEFT)
    }
}
