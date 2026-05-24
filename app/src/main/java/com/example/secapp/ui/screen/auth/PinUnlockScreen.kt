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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.secapp.data.repository.AuthRepository
import com.example.secapp.data.repository.AuthResult
import com.example.secapp.ui.components.CommonSpace
import com.example.secapp.ui.components.PinCodeInput
import com.example.secapp.ui.components.PinHeaderIcon
import com.example.secapp.ui.theme.SecAppTheme
import com.example.secapp.ui.theme.backgroundColor
import com.example.secapp.ui.theme.ibm_plex_sans
import com.example.secapp.ui.theme.space_grotesk
import kotlinx.coroutines.launch

@Composable
fun PinUnlockScreen(
    openDashboard: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    val context = LocalContext.current
    val authRepository = remember { AuthRepository(context) }
    val coroutineScope = rememberCoroutineScope()
    var pin by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun continueWithoutMasterKey() {
        Toast.makeText(
            context,
            "Tiếp tục với session hiện tại. Lịch sử cũ cần PIN để khôi phục.",
            Toast.LENGTH_LONG
        ).show()
        openDashboard()
    }

    fun submit() {
        if (isLoading) return

        when (val action = PinUnlockPolicy.resolveSubmitAction(pin)) {
            PinUnlockAction.ContinueWithoutMasterKey -> {
                continueWithoutMasterKey()
                return
            }
            PinUnlockAction.AttemptUnlock -> Unit
            is PinUnlockAction.Reject -> {
                errorMessage = action.message
                return
            }
        }

        coroutineScope.launch {
            isLoading = true
            errorMessage = null
            when (val result = authRepository.unlockMasterPrivateKey(pin)) {
                is AuthResult.Success -> {
                    Toast.makeText(context, "Mở khóa thành công.", Toast.LENGTH_SHORT).show()
                    openDashboard()
                }
                is AuthResult.Failure -> {
                    errorMessage = "${result.message}. Bạn vẫn có thể tiếp tục với session hiện tại."
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
                text = "Nhập mã PIN",
                color = Color.Black,
                fontFamily = space_grotesk,
                fontWeight = FontWeight.Bold,
                fontSize = 32.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Nhập mã PIN để khôi phục khóa cá nhân, hoặc bỏ qua để dùng session hiện tại.",
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
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xff283FB1)),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text(
                    text = if (isLoading) "Đang xử lý..." else "Mở khóa",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelMedium
                )
            }

            CommonSpace(20.dp)

            Button(
                onClick = { continueWithoutMasterKey() },
                enabled = !isLoading,
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color(0xff283FB1)
                ),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text(
                    text = "Tiếp tục không nhập PIN",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            CommonSpace(12.dp)

            Button(
                onClick = onLogout,
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color(0xff283FB1)
                ),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text(
                    text = "Đăng xuất",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
@Preview(showSystemUi = true)
fun PinUnlockScreenPreview() {
    SecAppTheme() {
        PinUnlockScreen()
    }
}