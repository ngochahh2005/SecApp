package com.example.secapp.ui.screen.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import android.os.Build
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import android.widget.Toast
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.secapp.R
import com.example.secapp.data.repository.AuthRepository
import com.example.secapp.data.repository.AuthResult
import com.example.secapp.ui.components.AuthTitle
import com.example.secapp.ui.components.CommonSpace
import com.example.secapp.ui.components.PasswordTextField
import com.example.secapp.ui.components.UsernameTextField
import com.example.secapp.ui.theme.SecAppTheme
import com.example.secapp.ui.theme.backgroundColor
import com.example.secapp.ui.theme.ibm_plex_sans
import com.example.secapp.ui.theme.space_grotesk
import com.example.secapp.ui.theme.titleColor
import com.example.secapp.ui.viewmodel.LoginViewModel
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    viewModel: LoginViewModel = viewModel(),
    openRegisterScreen: () -> Unit = {},
    openPinUnlockScreen: () -> Unit = {}
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val authRepository = remember { AuthRepository(context) }
    val coroutineScope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val handleLogin = {
        coroutineScope.launch {
            isLoading = true
            errorMessage = null
            when (val result = authRepository.login(viewModel.username.trim(), viewModel.password, Build.MODEL)) {
                is AuthResult.Success -> {
                    Toast.makeText(context, "Đăng nhập thành công. PIN là tùy chọn để mở lịch sử cũ.", Toast.LENGTH_SHORT).show()
                    openPinUnlockScreen()
                }
                is AuthResult.Failure -> {
                    errorMessage = result.message
                }
            }
            isLoading = false
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(backgroundColor)) {
        Image(
            painter = painterResource(R.drawable.gradient_background),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 36.dp, end = 36.dp, top = 64.dp, bottom = 48.dp)
        ) {
            Icon(
                Icons.Default.Security,
                contentDescription = null,
                tint = Color(0xff283FB1),
                modifier = Modifier.size(80.dp).align(Alignment.CenterHorizontally)
            )
            CommonSpace(4.dp)
            Text(
                text = "Welcome Back",
                style = MaterialTheme.typography.titleLarge,
                color = Color(0xff222222),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            CommonSpace(4.dp)
            Text(
                text = "Hãy nhập thông tin của bạn để đăng nhập",
                fontFamily = ibm_plex_sans,
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp,
                color = Color(0xff888888),
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )

            CommonSpace(36.dp)

            AuthTitle("Tên đăng nhập")
            CommonSpace()
            UsernameTextField(
                username = viewModel.username,
                onValueChange = {viewModel.onUsernameChange(it)},
                onAction = { focusManager.moveFocus(FocusDirection.Down) }
            )

            CommonSpace()
            AuthTitle("Mật khẩu")
            CommonSpace()
            PasswordTextField(
                viewModel.isShowPassword,
                viewModel.password,
                onPassWordChange = { viewModel.onPasswordChange(it) },
                onShowPasswordChange = { viewModel.onShowPasswordChange() },
                onAction = { if (!isLoading) handleLogin() },
                imeAction = ImeAction.Done
            )

            CommonSpace()

            Text(
                text = "Quên mật khẩu?",
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xffF34B1B),
//                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.End)
                    .clickable(onClick = {})
            )

            CommonSpace(36.dp)

            errorMessage?.let { message ->
                Text(
                    text = message,
                    color = Color(0xFFB00020),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            }

            Column(
                modifier = Modifier.fillMaxSize(),
            ) {
                Button(
                    onClick = { handleLogin() },
                    enabled = !isLoading,
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xff283FB1)
                    ),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text(
                        text = if (isLoading) "Đang xử lý..." else "Đăng nhập",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelMedium
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        thickness = 2.dp,
                        color = Color.White.copy(alpha = 0.5f)
                    )

                    Text(
                        text = "Or Sign In With",
                        modifier = Modifier.padding(horizontal = 16.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xff888888)
                    )

                    HorizontalDivider(
                        modifier = Modifier
                            .weight(1f),
                        thickness = 2.dp,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = {},
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xffFFFFFF)
                    ),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Row(
                        modifier = Modifier.wrapContentSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Image(
                            painter = painterResource(R.drawable.btn_sign_in_with_gg),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Google",
                            style = MaterialTheme.typography.labelMedium,
                            color = titleColor
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(2.5f))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text(
                        text = "Chưa có tài khoản?",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xff888888)
                    )

                    Text(
                        text = "Đăng ký ngay",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xff283FB1),
                        modifier = Modifier
                            .clickable(onClick = openRegisterScreen)
                    )
                }
            }
        }
    }
}

@Composable
@Preview(showSystemUi = true, device = Devices.PIXEL_7)
fun LoginScreenPreview() {
    SecAppTheme() {
        LoginScreen()
    }
}

@Composable
@Preview(showSystemUi = true, device = Devices.TABLET)
fun LoginScreenPreview2() {
    SecAppTheme() {
        LoginScreen()
    }
}