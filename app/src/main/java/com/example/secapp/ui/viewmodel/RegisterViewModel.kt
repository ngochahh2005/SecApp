package com.example.secapp.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

class RegisterViewModel: ViewModel() {
    var username by mutableStateOf("")
    private set

    var email by mutableStateOf("")
    private set

    var password by mutableStateOf("")
    private set

    var confirmPassword by mutableStateOf("")
    private set

    var isShowPassword by mutableStateOf(false)
    private set

    var isShowConfirmPassword by mutableStateOf(false)
    private set

    fun onUsernameChange(newValue: String) {
        username = newValue
    }

    fun onEmailChange(newValue: String) {
        email = newValue
    }

    fun onPasswordChange(newValue: String) {
        password = newValue
    }

    fun onConfirmPasswordChange(newValue: String) {
        confirmPassword = newValue
    }

    fun onShowPasswordChange() {
        isShowPassword = !isShowPassword
    }

    fun onShowConfirmPasswordChange() {
        isShowConfirmPassword = !isShowConfirmPassword
    }
}