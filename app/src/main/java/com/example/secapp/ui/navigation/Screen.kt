package com.example.secapp.ui.navigation

sealed class Screen(val route: String) {
    object Login: Screen("login")
    object Register: Screen("register")
    object PinUnlock: Screen("pin_unlock")
}