package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// Every colour role is set on purpose so dialogs, sheets, chips and menus match the bento tiles:
// indigo for the main colour, neutral greys for every surface, and no tint on raised surfaces.
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFA5B4FC),
    onPrimary = Color(0xFF1E1B4B),
    primaryContainer = Color(0xFF3730A3),
    onPrimaryContainer = Color(0xFFE0E7FF),
    secondary = Color(0xFF86EFAC),
    onSecondary = Color(0xFF052E16),
    secondaryContainer = Color(0xFF2E2A5C),
    onSecondaryContainer = Color(0xFFE0E7FF),
    tertiary = Color(0xFFFCA5A5),
    onTertiary = Color(0xFF450A0A),
    tertiaryContainer = Color(0xFF3D3110),
    onTertiaryContainer = Color(0xFFFDE68A),
    error = Color(0xFFFCA5A5),
    onError = Color(0xFF450A0A),
    errorContainer = Color(0xFF451818),
    onErrorContainer = Color(0xFFFECACA),
    background = Color(0xFF111318),
    onBackground = Color(0xFFE6E6EA),
    surface = Color(0xFF1A1C21),
    onSurface = Color(0xFFE6E6EA),
    surfaceVariant = Color(0xFF2A2D34),
    onSurfaceVariant = Color(0xFFA9ADB8),
    surfaceTint = Color.Transparent,
    surfaceContainerLowest = Color(0xFF0E0F12),
    surfaceContainerLow = Color(0xFF16181D),
    surfaceContainer = Color(0xFF1A1C21),
    surfaceContainerHigh = Color(0xFF1F2127),
    surfaceContainerHighest = Color(0xFF2A2D34),
    surfaceBright = Color(0xFF2A2D34),
    surfaceDim = Color(0xFF111318),
    inverseSurface = Color(0xFFE6E6EA),
    inverseOnSurface = Color(0xFF1A1C21),
    inversePrimary = Color(0xFF3730A3),
    outline = Color(0xFF4A4E58),
    outlineVariant = Color(0xFF2C2F37)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF3730A3),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0E7FF),
    onPrimaryContainer = Color(0xFF1E1B4B),
    secondary = Color(0xFF166534),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE8E6FB),
    onSecondaryContainer = Color(0xFF1E1B4B),
    tertiary = Color(0xFF991B1B),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFBEFC5),
    onTertiaryContainer = Color(0xFF854D0E),
    error = Color(0xFFB91C1C),
    onError = Color.White,
    errorContainer = Color(0xFFFFE0E0),
    onErrorContainer = Color(0xFF7F1D1D),
    background = Color(0xFFF5F6FA),
    onBackground = LightOnSurface,
    surface = Color.White,
    onSurface = LightOnSurface,
    surfaceVariant = Color(0xFFEFF0F4),
    onSurfaceVariant = Color(0xFF5B5F6B),
    surfaceTint = Color.Transparent,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF8F9FC),
    surfaceContainer = Color(0xFFF2F3F7),
    surfaceContainerHigh = Color.White,
    surfaceContainerHighest = Color(0xFFEFF0F4),
    surfaceBright = Color.White,
    surfaceDim = Color(0xFFE3E4EA),
    inverseSurface = Color(0xFF1A1C21),
    inverseOnSurface = Color(0xFFE6E6EA),
    inversePrimary = Color(0xFFA5B4FC),
    outline = Color(0xFFC5C6D0),
    outlineVariant = Color(0xFFE3E4EA)
)

@Composable
fun SmartBudgetTheme(
    themeMode: String = "SYSTEM",
    dynamicColor: Boolean = false, // Use our financial theme colors by default
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        "LIGHT" -> false
        "DARK" -> true
        else -> isSystemInDarkTheme()
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    CompositionLocalProvider(LocalIsDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
