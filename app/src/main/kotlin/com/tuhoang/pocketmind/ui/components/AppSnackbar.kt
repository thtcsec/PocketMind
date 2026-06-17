package com.tuhoang.pocketmind.ui.components

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

val LocalAppSnackbar = compositionLocalOf<(String) -> Unit> { { } }

@Composable
fun ProvideAppSnackbar(
    snackbarHostState: SnackbarHostState,
    content: @Composable () -> Unit
) {
    val scope = rememberCoroutineScope()
    val showSnackbar: (String) -> Unit = { message ->
        scope.launch { snackbarHostState.showSnackbar(message) }
    }
    CompositionLocalProvider(LocalAppSnackbar provides showSnackbar, content = content)
}

@Composable
fun rememberShowSnackbar(): (String) -> Unit = LocalAppSnackbar.current
