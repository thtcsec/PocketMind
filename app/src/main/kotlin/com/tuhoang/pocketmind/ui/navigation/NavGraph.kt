package com.tuhoang.pocketmind.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.tuhoang.pocketmind.ui.admin.AdminDashboardScreen
import com.tuhoang.pocketmind.ui.auth.LoginScreen
import com.tuhoang.pocketmind.ui.auth.RegisterScreen
import com.tuhoang.pocketmind.ui.main.MainScreen
import com.tuhoang.pocketmind.ui.profile.ProfileEditScreen
import com.tuhoang.pocketmind.ui.settings.AiSettingsScreen
import com.tuhoang.pocketmind.ui.settings.SettingsScreen

object Routes {
    const val MAIN = "main"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val SETTINGS = "settings"
    const val AI_SETTINGS = "ai_settings"
    const val PROFILE_EDIT = "profile_edit"
    const val ADMIN = "admin"
}

@Composable
fun PocketMindNavHost() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.MAIN) {
        composable(Routes.MAIN) {
            MainScreen(
                onNavigateLogin = { navController.navigate(Routes.LOGIN) },
                onNavigateSettings = { navController.navigate(Routes.SETTINGS) },
                onNavigateProfileEdit = { navController.navigate(Routes.PROFILE_EDIT) },
                onNavigateAdmin = { navController.navigate(Routes.ADMIN) }
            )
        }
        composable(Routes.LOGIN) {
            LoginScreen(
                onBack = { navController.popBackStack() },
                onNavigateRegister = { navController.navigate(Routes.REGISTER) }
            )
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
