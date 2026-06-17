package com.tuhoang.pocketmind.ui.main

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tuhoang.pocketmind.R
import com.tuhoang.pocketmind.ui.chat.ChatScreen
import com.tuhoang.pocketmind.ui.home.HomeScreen
import com.tuhoang.pocketmind.ui.profile.ProfileScreen
import com.tuhoang.pocketmind.ui.report.ReportScreen
import kotlinx.coroutines.launch

private const val EXIT_INTERVAL_MS = 2000L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onNavigateLogin: () -> Unit,
    onNavigateSettings: () -> Unit,
    onNavigateProfileEdit: () -> Unit,
    onNavigateAdmin: () -> Unit
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var lastBackPress by remember { mutableLongStateOf(0L) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val exitMessage = stringResource(R.string.exit_press_back_again)

    BackHandler {
        if (selectedTab != 0) {
            selectedTab = 0
            return@BackHandler
        }

        val now = System.currentTimeMillis()
        if (now - lastBackPress < EXIT_INTERVAL_MS) {
            (context as? Activity)?.finish()
        } else {
            lastBackPress = now
            scope.launch {
                snackbarHostState.showSnackbar(exitMessage)
            }
        }
    }

    val tabs = listOf(
        Triple(0, stringResource(R.string.title_home), Icons.Default.Home),
        Triple(1, stringResource(R.string.title_add), Icons.Default.Add),
        Triple(2, stringResource(R.string.title_report), Icons.Default.BarChart),
        Triple(3, stringResource(R.string.title_profile), Icons.Default.Person)
    )
    val currentTitle = tabs.first { it.first == selectedTab }.second

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets.statusBars,
                title = {
                    Column {
                        Text(currentTitle, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateSettings) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = stringResource(R.string.title_settings)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                windowInsets = WindowInsets.navigationBars,
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                tabs.forEach { (index, label, icon) ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = { Icon(icon, contentDescription = label) },
                        label = { Text(label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
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
