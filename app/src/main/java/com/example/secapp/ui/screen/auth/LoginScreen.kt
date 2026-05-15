package com.example.secapp.ui.screen.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.secapp.R
import com.example.secapp.ui.theme.SecAppTheme
import com.example.secapp.ui.theme.backgroundColor
import com.example.secapp.ui.theme.space_grotesk

@Composable
fun LoginScreen() {
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
                .padding(18.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Welcome Back",
                fontFamily = space_grotesk,
                fontWeight = FontWeight.Bold,
                fontSize = 32.sp,
                color = Color(0xff222222),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
@Preview(showSystemUi = true)
fun LoginScreenPreview() {
    SecAppTheme() {
        LoginScreen()
    }
}