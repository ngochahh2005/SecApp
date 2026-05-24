package com.example.secapp.ui.screen.auth

sealed class PinUnlockAction {
    data object ContinueWithoutMasterKey : PinUnlockAction()
    data object AttemptUnlock : PinUnlockAction()
    data class Reject(val message: String) : PinUnlockAction()
}

object PinUnlockPolicy {
    private val sixDigitPin = Regex("\\d{6}")

    fun resolveSubmitAction(pin: String): PinUnlockAction {
        val normalizedPin = pin.trim()
        return when {
            normalizedPin.isBlank() -> PinUnlockAction.ContinueWithoutMasterKey
            normalizedPin.matches(sixDigitPin) -> PinUnlockAction.AttemptUnlock
            else -> PinUnlockAction.Reject("Vui lòng nhập đủ 6 số PIN hoặc bỏ qua để dùng phiên hiện tại")
        }
    }
}