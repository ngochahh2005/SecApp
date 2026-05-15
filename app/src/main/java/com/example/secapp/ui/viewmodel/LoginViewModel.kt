package com.example.secapp.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

class LoginViewModel: ViewModel() {
    var username by mutableStateOf("")
    private set

    var password by mutableStateOf("")
    private set

    var isShowPassword by mutableStateOf(false)

    fun onUsernameChange(newValue: String) {
        username = newValue
    }

    fun onPasswordChange(newValue: String) {
        password = newValue
    }

    fun onShowPasswordChange() {
        isShowPassword = !isShowPassword
    }
}