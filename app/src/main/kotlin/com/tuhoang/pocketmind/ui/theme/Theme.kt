package com.tuhoang.pocketmind.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val GreenPrimary = Color(0xFF4CAF50)
val YellowSecondary = Color(0xFFFFEB3B)
val RedExpense = Color(0xFFF44336)
val BlueAccent = Color(0xFF2196F3)

private val LightColorScheme = lightColorScheme(
    primary = GreenPrimary,
    secondary = YellowSecondary,
    tertiary = BlueAccent
)

private val DarkColorScheme = darkColorScheme(
    primary = GreenPrimary,
    secondary = YellowSecondary,
    tertiary = BlueAccent
)

@Composable
fun PocketMindTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
