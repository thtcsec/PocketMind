package com.tuhoang.pocketmind.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.tuhoang.pocketmind.utils.PrefsManager

val GreenPrimary = Color(0xFF2E7D32)
val GreenPrimaryDark = Color(0xFF66BB6A)
val GreenContainer = Color(0xFFE8F5E9)
val GreenContainerDark = Color(0xFF1B3A1E)
val RedExpense = Color(0xFFE53935)
val BlueAccent = Color(0xFF1565C0)

private val LightColorScheme = lightColorScheme(
    primary = GreenPrimary,
    onPrimary = Color.White,
    primaryContainer = GreenContainer,
    onPrimaryContainer = Color(0xFF1B5E20),
    secondary = Color(0xFF558B2F),
    onSecondary = Color.White,
    tertiary = BlueAccent,
    background = Color(0xFFF7FAF7),
    onBackground = Color(0xFF1A1C19),
    surface = Color(0xFFFCFDFB),
    onSurface = Color(0xFF1A1C19),
    surfaceVariant = Color(0xFFEEF2EE),
    onSurfaceVariant = Color(0xFF434843),
    outline = Color(0xFFBFC9BF)
)

private val DarkColorScheme = darkColorScheme(
    primary = GreenPrimaryDark,
    onPrimary = Color(0xFF003910),
    primaryContainer = GreenContainerDark,
    onPrimaryContainer = Color(0xFFC8E6C9),
    secondary = Color(0xFFA5D6A7),
    onSecondary = Color(0xFF1B3A1E),
    tertiary = Color(0xFF90CAF9),
    background = Color(0xFF101410),
    onBackground = Color(0xFFE2E3DE),
    surface = Color(0xFF161A16),
    onSurface = Color(0xFFE2E3DE),
    surfaceVariant = Color(0xFF252A25),
    onSurfaceVariant = Color(0xFFC3C8C0),
    outline = Color(0xFF8D938B)
)

@Composable
fun PocketMindTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = PrefsManager.getInstance().isDynamicColorEnabled(),
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
