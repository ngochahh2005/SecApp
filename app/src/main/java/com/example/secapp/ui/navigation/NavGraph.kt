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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.secapp.data.repository.AuthRepository
import com.example.secapp.ui.screen.auth.CreatePinScreen
import com.example.secapp.ui.screen.auth.DashboardScreen
import com.example.secapp.ui.screen.auth.LoginScreen
import com.example.secapp.ui.screen.auth.PinUnlockScreen
import com.example.secapp.ui.screen.auth.RegisterScreen

@Composable
fun NavGraph(innerPadding: PaddingValues, navController: NavHostController) {
    var registrationDraft by remember { mutableStateOf<RegistrationDraft?>(null) }

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
                },
                openPinUnlockScreen = {
                    navController.navigate(Screen.PinUnlock.route)
                }
            )
        }

        // register
        composable(route = Screen.Register.route) {
            RegisterScreen(
                openLoginScreen = {
                    navController.navigate(Screen.Login.route)
                },
                openCreatePinScreen = { username, email, password, confirmPassword ->
                    registrationDraft = RegistrationDraft(username, email, password, confirmPassword)
                    navController.navigate(Screen.CreatePin.route)
                }
            )
        }

        // create pin
        composable(route = Screen.CreatePin.route) {
            val draft = registrationDraft
            if (draft == null) {
                LaunchedEffect(Unit) {
                    navController.navigate(Screen.Register.route) {
                        popUpTo(Screen.Login.route) { inclusive = false }
                        launchSingleTop = true
                    }
                }
            } else {
                CreatePinScreen(
                    username = draft.username,
                    email = draft.email,
                    password = draft.password,
                    confirmPassword = draft.confirmPassword,
                    openLoginScreen = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.Login.route) { inclusive = false }
                            launchSingleTop = true
                        }
                    },
                    onMissingRegistrationData = {
                        navController.navigate(Screen.Register.route) {
                            popUpTo(Screen.Login.route) { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                )
            }
        }

        // pin unlock
        composable(route = Screen.PinUnlock.route) {
            val context = LocalContext.current
            val authRepository = AuthRepository(context)
            PinUnlockScreen(
                openDashboard = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onLogout = {
                    authRepository.clearSession()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.PinUnlock.route) { inclusive = true }
                    }
                }
            )
        }

        // dashboard
        composable(route = Screen.Dashboard.route) {
            val context = LocalContext.current
            val authRepository = AuthRepository(context)
            val displayName = authRepository.getCurrentUserDisplayName() ?: "Người dùng"
            DashboardScreen(
                displayName = displayName,
                onLogout = {
                    authRepository.clearSession()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Dashboard.route) { inclusive = true }
                    }
                }
            )
        }
    }
}

private data class RegistrationDraft(
    val username: String,
    val email: String,
    val password: String,
    val confirmPassword: String
)
