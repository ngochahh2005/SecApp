package com.example.secapp.ui.screen.auth

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.secapp.data.repository.AuthRepository
import com.example.secapp.data.repository.AuthResult
import com.example.secapp.ui.components.PinCodeInput
import com.example.secapp.ui.components.PinHeaderIcon
import com.example.secapp.ui.theme.SecAppTheme
import com.example.secapp.ui.theme.backgroundColor
import com.example.secapp.ui.theme.ibm_plex_sans
import com.example.secapp.ui.theme.space_grotesk
import kotlinx.coroutines.launch

@Composable
fun CreatePinScreen(
    username: String,
    email: String,
    password: String,
    confirmPassword: String,
    openLoginScreen: () -> Unit = {},
    onMissingRegistrationData: () -> Unit = {}
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val authRepository = remember { AuthRepository(context) }
    val coroutineScope = rememberCoroutineScope()
    var pin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    fun submit() {
        if (isLoading) return
        if (username.isBlank() || email.isBlank() || password.isBlank() || confirmPassword.isBlank()) {
            onMissingRegistrationData()
            return
        }
        if (pin.length != 6) {
            errorMessage = "PIN phải gồm đúng 6 số"
            return
        }

        coroutineScope.launch {
            isLoading = true
            errorMessage = null
            focusManager.clearFocus()
            when (val result = authRepository.signup(username, email, password, confirmPassword, pin)) {
                is AuthResult.Success -> {
                    Toast.makeText(context, "Đăng ký thành công. Vui lòng đăng nhập.", Toast.LENGTH_SHORT).show()
                    openLoginScreen()
                }
                is AuthResult.Failure -> {
                    errorMessage = result.message
                }
            }
            isLoading = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(horizontal = 24.dp, vertical = 48.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(12.dp))
            PinHeaderIcon()
            Spacer(modifier = Modifier.height(76.dp))

            Text(
                text = "Tạo mã PIN mới",
                color = Color.Black,
                fontFamily = space_grotesk,
                fontWeight = FontWeight.Bold,
                fontSize = 32.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Hãy tạo mã PIN dễ nhớ. Bạn sẽ cần nhập mã PIN để khôi phục lịch sử chat.",
                color = Color(0xFF777A82),
                fontFamily = ibm_plex_sans,
                fontSize = 18.sp,
                lineHeight = 26.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(52.dp))
            PinCodeInput(
                pin = pin,
                onPinChange = {
                    pin = it
                    errorMessage = null
                },
                enabled = !isLoading,
                onDone = { submit() }
            )

            errorMessage?.let { message ->
                Text(
                    text = message,
                    color = Color(0xFFB00020),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 20.dp)
                )
            }

            Spacer(modifier = Modifier.weight(1f))
            Button(
                onClick = { submit() },
                enabled = !isLoading,
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xff283FB1)),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text(
                    text = if (isLoading) "Đang xử lý..." else "Tạo tài khoản",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

@Composable
@Preview(showSystemUi = true, device = Devices.PIXEL_7)
fun CreatePinScreenPreview() {
    SecAppTheme {
        CreatePinScreen(
            username = "ngochahh2005",
            email = "ngochahh2005@example.com",
            password = "12345678",
            confirmPassword = "12345678"
        )
    }
}
