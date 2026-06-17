package com.tuhoang.pocketmind.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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

private val subScreenTransitions = listOf(
    Routes.LOGIN,
    Routes.REGISTER,
    Routes.FORGOT_PASSWORD,
    Routes.SETTINGS,
    Routes.AI_SETTINGS,
    Routes.PROFILE_EDIT,
    Routes.ADMIN
)

@Composable
fun PocketMindNavHost() {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val prefs = PrefsManager.getInstance()
    val startDestination = if (prefs.isOnboardingComplete()) Routes.MAIN else Routes.ONBOARDING

    ProvideAppSnackbar(snackbarHostState) {
        Box(modifier = Modifier.fillMaxSize()) {
            NavHost(
                navController = navController,
                startDestination = startDestination,
                modifier = Modifier.fillMaxSize()
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
                composable(
                    Routes.LOGIN,
                    enterTransition = { slideInFromEnd() },
                    exitTransition = { slideOutToStart() },
                    popEnterTransition = { slideInFromStart() },
                    popExitTransition = { slideOutToEnd() }
                ) {
                    LoginScreen(
                        onBack = { navController.popBackStack() },
                        onNavigateRegister = { navController.navigate(Routes.REGISTER) },
                        onNavigateForgotPassword = { navController.navigate(Routes.FORGOT_PASSWORD) }
                    )
                }
                composable(
                    Routes.FORGOT_PASSWORD,
                    enterTransition = { slideInFromEnd() },
                    exitTransition = { slideOutToStart() },
                    popEnterTransition = { slideInFromStart() },
                    popExitTransition = { slideOutToEnd() }
                ) {
                    ForgotPasswordScreen(onBack = { navController.popBackStack() })
                }
                composable(
                    Routes.REGISTER,
                    enterTransition = { slideInFromEnd() },
                    exitTransition = { slideOutToStart() },
                    popEnterTransition = { slideInFromStart() },
                    popExitTransition = { slideOutToEnd() }
                ) {
                    RegisterScreen(onBack = { navController.popBackStack() })
                }
                composable(
                    Routes.SETTINGS,
                    enterTransition = { slideInFromEnd() },
                    exitTransition = { slideOutToStart() },
                    popEnterTransition = { slideInFromStart() },
                    popExitTransition = { slideOutToEnd() }
                ) {
                    SettingsScreen(
                        onBack = { navController.popBackStack() },
                        onNavigateAiSettings = { navController.navigate(Routes.AI_SETTINGS) }
                    )
                }
                composable(
                    Routes.AI_SETTINGS,
                    enterTransition = { slideInFromEnd() },
                    exitTransition = { slideOutToStart() },
                    popEnterTransition = { slideInFromStart() },
                    popExitTransition = { slideOutToEnd() }
                ) {
                    AiSettingsScreen(onBack = { navController.popBackStack() })
                }
                composable(
                    Routes.PROFILE_EDIT,
                    enterTransition = { slideInFromEnd() },
                    exitTransition = { slideOutToStart() },
                    popEnterTransition = { slideInFromStart() },
                    popExitTransition = { slideOutToEnd() }
                ) {
                    ProfileEditScreen(
                        onBack = { navController.popBackStack() },
                        onDeleted = {
                            navController.navigate(Routes.LOGIN) {
                                popUpTo(Routes.MAIN) { inclusive = false }
                            }
                        }
                    )
                }
                composable(
                    Routes.ADMIN,
                    enterTransition = { slideInFromEnd() },
                    exitTransition = { slideOutToStart() },
                    popEnterTransition = { slideInFromStart() },
                    popExitTransition = { slideOutToEnd() }
                ) {
                    AdminDashboardScreen(onBack = { navController.popBackStack() })
                }
            }
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 8.dp)
            )
        }
    }
}
