package com.example.secapp.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.PersonPin
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.secapp.ui.theme.SecAppTheme
import com.example.secapp.ui.theme.titleColor

@Composable
fun UsernameTextField(
    username: String,
    text: String = "example123",
    onValueChange: (String) -> Unit = {},
    onAction: () -> Unit = {},
    imeAction: ImeAction = ImeAction.Next,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = username,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        enabled = true,
        textStyle = MaterialTheme.typography.labelMedium,
        singleLine = true,

        placeholder = {
            Text(text = text, style = MaterialTheme.typography.labelMedium)
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
            if (text.equals("example123")) Icon(Icons.Default.PersonPin, contentDescription = null)
            else Icon(Icons.Default.MailOutline, contentDescription = null)
        },

        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Email,
            imeAction = imeAction
        ),

        keyboardActions = KeyboardActions(
            onNext = { onAction() },
            onDone = { onAction() }
        )
    )
}

@Composable
@Preview(showSystemUi = true)
private fun preview() {
    SecAppTheme() {
        UsernameTextField("")
    }
}