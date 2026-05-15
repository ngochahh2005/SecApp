package com.example.secapp.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.PersonPin
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.secapp.ui.theme.SecAppTheme
import com.example.secapp.ui.theme.titleColor

@Composable
fun PasswordTextField(
    isShowPassword: Boolean,
    password: String,
    onPassWordChange: (String) -> Unit = {},
    onShowPasswordChange: () -> Unit = {},
    onAction: () -> Unit = {},
    imeAction: ImeAction = ImeAction.Done,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = password,
        onValueChange = onPassWordChange,
        modifier = modifier.fillMaxWidth(),
        enabled = true,
        textStyle = MaterialTheme.typography.labelMedium,
        singleLine = true,

        placeholder = {
            Text(text = "* * * * * * * *", style = MaterialTheme.typography.labelMedium)
        },

        shape = RoundedCornerShape(24.dp),

        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = titleColor,
            unfocusedTextColor = titleColor,

            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,

            focusedBorderColor = Color(0xFF656565),
            unfocusedBorderColor = Color(0xff888888)
        ),

        leadingIcon = {
            Icon(Icons.Default.Lock, contentDescription = null)
        },

        trailingIcon = {
            IconButton(
                onClick = onShowPasswordChange
            ) {
                if (isShowPassword) Icon(Icons.Default.VisibilityOff, contentDescription = null)
                else Icon(Icons.Default.Visibility, contentDescription = null)
            }
        },

        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = imeAction
        ),

        keyboardActions = KeyboardActions(
            onDone = { onAction() },
            onNext = { onAction() }
        ),

        visualTransformation = if (isShowPassword) VisualTransformation.None else PasswordVisualTransformation()
    )
}

@Composable
@Preview(showSystemUi = true)
private fun preview() {
    SecAppTheme() {
        PasswordTextField(false, "")
    }
}