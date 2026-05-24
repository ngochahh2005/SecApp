package com.example.secapp.data.remote

internal object RealtimeEventTypePolicy {
    fun isMessageCreated(type: String?): Boolean = type == "message.created"

    fun isMemberLeft(type: String?): Boolean = type == "member.left"
}
