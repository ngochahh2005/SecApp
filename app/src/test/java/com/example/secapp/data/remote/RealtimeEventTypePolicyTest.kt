package com.example.secapp.data.remote

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RealtimeEventTypePolicyTest {
    @Test
    fun identifiesMemberLeftEvents() {
        assertTrue(RealtimeEventTypePolicy.isMemberLeft("member.left"))
        assertFalse(RealtimeEventTypePolicy.isMemberLeft("message.created"))
    }

    @Test
    fun identifiesMessageCreatedEvents() {
        assertTrue(RealtimeEventTypePolicy.isMessageCreated("message.created"))
        assertFalse(RealtimeEventTypePolicy.isMessageCreated("member.left"))
    }
}
