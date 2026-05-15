package com.example.secapp.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.secapp.ui.theme.SecAppTheme

@Composable
fun UsernameTextField(
    username: String,
    text: String = "example123",
    onValueChange: (String) -> Unit = {},
    onAction: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    TextField(
        value = username,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = true,
        textStyle = MaterialTheme.typography.labelMedium,

        placeholder = {
            Text(text = text, style = MaterialTheme.typography.labelMedium)
        },
        
        
    )
}

@Composable
@Preview(showSystemUi = true)
private fun preview() {
    SecAppTheme() {
        UsernameTextField("")
    }
}