package com.tuhoang.pocketmind.ui.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.tuhoang.pocketmind.R
import com.tuhoang.pocketmind.ui.chat.ChatScreen
import com.tuhoang.pocketmind.ui.home.HomeScreen
import com.tuhoang.pocketmind.ui.profile.ProfileScreen
import com.tuhoang.pocketmind.ui.report.ReportScreen

@Composable
fun MainScreen(
    onNavigateLogin: () -> Unit,
    onNavigateSettings: () -> Unit,
    onNavigateProfileEdit: () -> Unit,
    onNavigateAdmin: () -> Unit
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    val tabs = listOf(
        Triple(0, stringResource(R.string.title_home), Icons.Default.Home),
        Triple(1, stringResource(R.string.title_add), Icons.Default.Add),
        Triple(2, stringResource(R.string.title_report), Icons.Default.BarChart),
        Triple(3, stringResource(R.string.title_profile), Icons.Default.Person)
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEach { (index, label, icon) ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = { Icon(icon, contentDescription = label) },
                        label = { Text(label) }
                    )
                }
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding)) {
            when (selectedTab) {
                0 -> HomeScreen()
                1 -> ChatScreen()
                2 -> ReportScreen()
                3 -> ProfileScreen(
                    onNavigateLogin = onNavigateLogin,
                    onNavigateSettings = onNavigateSettings,
                    onNavigateProfileEdit = onNavigateProfileEdit,
                    onNavigateAdmin = onNavigateAdmin
                )
            }
        }
    }
}
