package com.example.secapp.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConversationCreationPolicyTest {
    @Test
    fun oneSelectedParticipantCreatesDirectConversationWhenGroupModeIsOff() {
        assertEquals(
            "DIRECT",
            ConversationCreationPolicy.resolveType(selectedParticipantCount = 1, groupMode = false)
        )
    }

    @Test
    fun multipleSelectedParticipantsCreateGroupConversation() {
        assertEquals(
            "GROUP",
            ConversationCreationPolicy.resolveType(selectedParticipantCount = 2, groupMode = false)
        )
    }

    @Test
    fun groupModeRequiresAtLeastOneParticipantAndANonBlankName() {
        assertFalse(ConversationCreationPolicy.canCreate(selectedParticipantCount = 0, groupMode = true, groupName = "Team"))
        assertFalse(ConversationCreationPolicy.canCreate(selectedParticipantCount = 2, groupMode = true, groupName = " "))
        assertTrue(ConversationCreationPolicy.canCreate(selectedParticipantCount = 2, groupMode = true, groupName = "Project room"))
    }

    @Test
    fun directModeRequiresExactlyOneParticipant() {
        assertFalse(ConversationCreationPolicy.canCreate(selectedParticipantCount = 0, groupMode = false, groupName = ""))
        assertTrue(ConversationCreationPolicy.canCreate(selectedParticipantCount = 1, groupMode = false, groupName = ""))
        assertFalse(ConversationCreationPolicy.canCreate(selectedParticipantCount = 2, groupMode = false, groupName = ""))
    }
}
