package com.example.secapp.data.repository

internal object ChatKeyRotationPolicy {
    const val MANUAL_SECURITY_ROTATION = "MANUAL_SECURITY_ROTATION"
    const val SESSION_WITHOUT_MASTER_KEY = "SESSION_WITHOUT_MASTER_KEY"
    const val MEMBER_ADDED = "MEMBER_ADDED"
    const val MEMBER_LEFT = "MEMBER_LEFT"

    fun nextVersion(
        requestedKeyVersion: Int,
        currentServerVersion: Int,
        latestCachedVersion: Int?
    ): Int {
        return maxOf(requestedKeyVersion, currentServerVersion, latestCachedVersion ?: 0) + 1
    }
}

internal object ChatKeyAccessPolicy {
    fun shouldProbeExistingKeyBeforeSending(hasUnlockedMasterKey: Boolean): Boolean {
        return hasUnlockedMasterKey
    }
}

internal object ConversationCreationPolicy {
    fun resolveType(selectedParticipantCount: Int, groupMode: Boolean): String {
        return if (groupMode || selectedParticipantCount > 1) "GROUP" else "DIRECT"
    }

    fun canCreate(selectedParticipantCount: Int, groupMode: Boolean, groupName: String): Boolean {
        if (selectedParticipantCount < 1) return false
        return when (resolveType(selectedParticipantCount, groupMode)) {
            "DIRECT" -> selectedParticipantCount == 1
            else -> groupName.isNotBlank()
        }
    }
}
