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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
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
import com.example.secapp.ui.components.AuthTitle
import com.example.secapp.ui.components.CommonSpace
import com.example.secapp.ui.components.PasswordTextField
import com.example.secapp.ui.components.UsernameTextField
import com.example.secapp.ui.theme.SecAppTheme
import com.example.secapp.ui.theme.backgroundColor
import com.example.secapp.ui.theme.ibm_plex_sans
import com.example.secapp.ui.viewmodel.RegisterViewModel

@Composable
fun RegisterScreen(
    viewModel: RegisterViewModel = viewModel(),
    openLoginScreen: () -> Unit = {},
    openCreatePinScreen: (username: String, email: String, password: String, confirmPassword: String) -> Unit = { _, _, _, _ -> }
) {
    val focusManager = LocalFocusManager.current
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val handleClickNext = {
        val error = when {
            viewModel.username.isBlank() || viewModel.email.isBlank() || viewModel.password.isBlank() || viewModel.confirmPassword.isBlank() ->
                "Vui lòng nhập đầy đủ thông tin"
            viewModel.password.length !in 8..128 ->
                "Mật khẩu phải có từ 8 đến 128 ký tự"
            viewModel.password != viewModel.confirmPassword ->
                "Mật khẩu và xác nhận mật khẩu không khớp"
            else -> null
        }
        
        errorMessage = error

        if (error == null) {
            openCreatePinScreen(
                viewModel.username.trim(),
                viewModel.email.trim(),
                viewModel.password,
                viewModel.confirmPassword
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(backgroundColor).clickable(
        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
        indication = null
    ) { focusManager.clearFocus() }) {
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
                .padding(start = 36.dp, end = 36.dp, top = 48.dp, bottom = 48.dp)
        ) {
            Icon(
                Icons.Default.Security,
                contentDescription = null,
                tint = Color(0xff283FB1),
                modifier = Modifier.size(80.dp).align(Alignment.CenterHorizontally)
            )
            CommonSpace(4.dp)
            Text(
                text = "Nice to meet you",
                style = MaterialTheme.typography.titleLarge,
                color = Color(0xff222222),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            CommonSpace(4.dp)
            Text(
                text = "Nhập thông tin của bạn để tạo tài khoản ngay",
                fontFamily = ibm_plex_sans,
                fontWeight = FontWeight.Normal,
                fontSize = 15.sp,
                color = Color(0xff888888),
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )

            CommonSpace()

            AuthTitle("Tên đăng nhập")
            CommonSpace(8.dp)
            UsernameTextField(
                username = viewModel.username,
                onValueChange = { viewModel.onUsernameChange(it) },
                onAction = { focusManager.moveFocus(FocusDirection.Down) },
                imeAction = ImeAction.Next
            )

            CommonSpace()

            AuthTitle("Email")
            CommonSpace(8.dp)
            UsernameTextField(
                username = viewModel.email,
                text = "example123@gmail.com",
                onValueChange = { viewModel.onEmailChange(it) },
                onAction = { focusManager.moveFocus(FocusDirection.Down) },
                imeAction = ImeAction.Next
            )

            CommonSpace()

            AuthTitle("Mật khẩu")
            CommonSpace(8.dp)
            PasswordTextField(
                isShowPassword = viewModel.isShowPassword,
                password = viewModel.password,
                onPassWordChange = { viewModel.onPasswordChange(it) },
                onShowPasswordChange = { viewModel.onShowPasswordChange() },
                onAction = { focusManager.moveFocus(FocusDirection.Down) },
                imeAction = ImeAction.Next
            )

            CommonSpace()

            AuthTitle("Xác nhận mật khẩu")
            CommonSpace(8.dp)
            PasswordTextField(
                isShowPassword = viewModel.isShowConfirmPassword,
                password = viewModel.confirmPassword,
                onPassWordChange = { viewModel.onConfirmPasswordChange(it) },
                onShowPasswordChange = { viewModel.onShowConfirmPasswordChange() },
                onAction = {
                    focusManager.clearFocus(true)
                    handleClickNext()
               },
                imeAction = ImeAction.Done
            )

            CommonSpace(24.dp)

            errorMessage?.let { message ->
                Text(
                    text = message,
                    color = Color(0xFFB00020),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            }

            Column(modifier = Modifier.fillMaxSize()) {
                Spacer(modifier = Modifier.weight(3.5f))
                Button(
                    onClick = { handleClickNext() },
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xff283FB1)),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text(
                        text = "Tiếp tục",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelMedium
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text(
                        text = "Đã có tài khoản?",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xff888888)
                    )

                    Text(
                        text = "Đăng nhập ngay",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xff283FB1),
                        modifier = Modifier.clickable(onClick = openLoginScreen)
                    )
                }
            }
        }
    }
}

@Composable
@Preview(showSystemUi = true, device = Devices.PIXEL_7)
fun RegisterScreenPreview() {
    SecAppTheme {
        RegisterScreen()
    }
}

@Composable
@Preview(showSystemUi = true, device = Devices.TABLET)
fun RegisterScreenPreview2() {
    SecAppTheme {
        RegisterScreen()
    }
}

@Composable
@Preview(showSystemUi = true, device = Devices.PIXEL_9_PRO)
fun RegisterScreenPreview3() {
    SecAppTheme {
        RegisterScreen()
    }
}
