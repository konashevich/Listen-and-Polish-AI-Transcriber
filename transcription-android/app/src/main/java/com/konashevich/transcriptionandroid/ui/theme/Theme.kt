package com.konashevich.pressscribe.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.konashevich.pressscribe.data.ThemeMode

private val DarkColors = darkColorScheme(
    primary = Color(0xFF0078D7),
    secondary = Color(0xFF00C084),
    tertiary = Color(0xFF8BD3FF),
    background = Color(0xFF2B2B2B),
    surface = Color(0xFF252526),
    surfaceVariant = Color(0xFF3C3C3C),
    onPrimary = Color.White,
    onSecondary = Color(0xFF092016),
    onBackground = Color(0xFFF0F0F0),
    onSurface = Color(0xFFF0F0F0),
    onSurfaceVariant = Color(0xFFE0E0E0),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF0078D7),
    secondary = Color(0xFF008E61),
    tertiary = Color(0xFF245E7A),
    background = Color(0xFFF0F0F0),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFE8E8E8),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color.Black,
    onSurface = Color.Black,
    onSurfaceVariant = Color(0xFF333333),
)

@Composable
fun PressScribeTheme(
    themeMode: ThemeMode,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        ThemeMode.AUTO -> isSystemInDarkTheme()
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }

    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
