package com.tuhoang.pocketmind.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.tuhoang.pocketmind.ui.admin.AdminDashboardScreen
import com.tuhoang.pocketmind.ui.auth.ForgotPasswordScreen
import com.tuhoang.pocketmind.ui.auth.LoginScreen
import com.tuhoang.pocketmind.ui.auth.RegisterScreen
import com.tuhoang.pocketmind.ui.components.BiometricUnlockGate
import com.tuhoang.pocketmind.ui.components.ProvideAppSnackbar
import com.tuhoang.pocketmind.ui.main.MainScreen
import com.tuhoang.pocketmind.ui.onboarding.OnboardingScreen
import com.tuhoang.pocketmind.ui.profile.ProfileEditScreen
import com.tuhoang.pocketmind.ui.settings.AiSettingsScreen
import com.tuhoang.pocketmind.ui.settings.SettingsScreen
import com.tuhoang.pocketmind.utils.PrefsManager

object Routes {
    const val ONBOARDING = "onboarding"
    const val MAIN = "main"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val FORGOT_PASSWORD = "forgot_password"
    const val SETTINGS = "settings"
    const val AI_SETTINGS = "ai_settings"
    const val PROFILE_EDIT = "profile_edit"
    const val ADMIN = "admin"
}

@Composable
fun PocketMindNavHost() {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val prefs = PrefsManager.getInstance()
    val startDestination = if (prefs.isOnboardingComplete()) Routes.MAIN else Routes.ONBOARDING

    ProvideAppSnackbar(snackbarHostState) {
        Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
            NavHost(
                navController = navController,
                startDestination = startDestination,
                modifier = Modifier.padding(padding)
            ) {
                composable(Routes.ONBOARDING) {
                    OnboardingScreen(
                        onFinished = {
                            navController.navigate(Routes.MAIN) {
                                popUpTo(Routes.ONBOARDING) { inclusive = true }
                            }
                        }
                    )
                }
                composable(Routes.MAIN) {
                    BiometricUnlockGate(enabled = prefs.isBiometricEnabled()) {
                        MainScreen(
                            onNavigateLogin = { navController.navigate(Routes.LOGIN) },
                            onNavigateSettings = { navController.navigate(Routes.SETTINGS) },
                            onNavigateProfileEdit = { navController.navigate(Routes.PROFILE_EDIT) },
                            onNavigateAdmin = { navController.navigate(Routes.ADMIN) }
                        )
                    }
                }
                composable(Routes.LOGIN) {
                    LoginScreen(
                        onBack = { navController.popBackStack() },
                        onNavigateRegister = { navController.navigate(Routes.REGISTER) },
                        onNavigateForgotPassword = { navController.navigate(Routes.FORGOT_PASSWORD) }
                    )
                }
                composable(Routes.FORGOT_PASSWORD) {
                    ForgotPasswordScreen(onBack = { navController.popBackStack() })
                }
                composable(Routes.REGISTER) {
                    RegisterScreen(onBack = { navController.popBackStack() })
                }
                composable(Routes.SETTINGS) {
                    SettingsScreen(
                        onBack = { navController.popBackStack() },
                        onNavigateAiSettings = { navController.navigate(Routes.AI_SETTINGS) }
                    )
                }
                composable(Routes.AI_SETTINGS) {
                    AiSettingsScreen(onBack = { navController.popBackStack() })
                }
                composable(Routes.PROFILE_EDIT) {
                    ProfileEditScreen(
                        onBack = { navController.popBackStack() },
                        onDeleted = {
                            navController.navigate(Routes.LOGIN) {
                                popUpTo(Routes.MAIN) { inclusive = false }
                            }
                        }
                    )
                }
                composable(Routes.ADMIN) {
                    AdminDashboardScreen(onBack = { navController.popBackStack() })
                }
            }
        }
    }
}
