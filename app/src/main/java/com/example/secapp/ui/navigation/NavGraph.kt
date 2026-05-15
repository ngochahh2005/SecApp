package com.example.secapp.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.secapp.ui.screen.auth.LoginScreen
import com.example.secapp.ui.screen.auth.PinUnlockScreen
import com.example.secapp.ui.screen.auth.RegisterScreen

@Composable
fun NavGraph(innerPadding: PaddingValues, navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Login.route,
        modifier = Modifier.fillMaxSize().padding(innerPadding),

        enterTransition = {
            fadeIn(
                animationSpec = tween(
                    durationMillis = 280,
                    easing = FastOutSlowInEasing
                )
            ) + slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Left,
                initialOffset = {it / 12},
                animationSpec = tween(
                    durationMillis = 500,
                    easing = FastOutSlowInEasing
                )
            ) + scaleIn(
                initialScale = 0.98f,
                animationSpec = tween(280)
            )
        },
        exitTransition = {
            fadeOut(
                animationSpec = tween(220)
            ) + scaleOut(
                targetScale = 1.02f,
                animationSpec = tween(220)
            )
        },
        popEnterTransition = {
            fadeIn(
                animationSpec = tween(280)
            ) + slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Right,
                initialOffset = {it / 12},
                animationSpec = tween(
                    durationMillis = 500,
                    easing = FastOutSlowInEasing
                )
            ) + scaleIn(
                initialScale = 0.98f,
                animationSpec = tween(280)
            )
        },
        popExitTransition = {
            fadeOut(
                animationSpec = tween(220)
            ) + scaleOut(
                targetScale = 1.02f,
                animationSpec = tween(220)
            )
        }
    ) {
        // login
        composable(route = Screen.Login.route) {
            LoginScreen(
                openRegisterScreen = {
                    navController.navigate(Screen.Register.route)
                }
            )
        }

        // register
        composable(route = Screen.Register.route) {
            RegisterScreen(
                openLoginScreen = {
                    navController.navigate(Screen.Login.route)
                }
            )
        }

        // pin unlock
        composable(route = Screen.PinUnlock.route) {
            PinUnlockScreen()
        }
    }
}