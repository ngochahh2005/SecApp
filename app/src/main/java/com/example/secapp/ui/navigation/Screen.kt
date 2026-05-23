package com.example.secapp.ui.navigation

sealed class Screen(val route: String) {
    object Login: Screen("login")
    object Register: Screen("register")
    object CreatePin: Screen("create_pin")
    object PinUnlock: Screen("pin_unlock")
    object Dashboard: Screen("dashboard")
    object Conversations: Screen("conversations")
    object CreateConversation: Screen("create_conversation")
    object ChatDetail: Screen("chat_detail/{conversationId}/{keyVersion}") {
        fun createRoute(conversationId: String, keyVersion: Int): String {
            return "chat_detail/$conversationId/$keyVersion"
        }
    }
}
