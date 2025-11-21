package com.example.myapplication.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = Color.White,
    primaryContainer = LightBlue,
    onPrimaryContainer = DarkBlue,

    secondary = PrimaryPurple,
    onSecondary = Color.White,
    secondaryContainer = LightPurple,
    onSecondaryContainer = DarkPurple,

    tertiary = PrimaryPurple,
    onTertiary = Color.White,
    tertiaryContainer = LightPurple,
    onTertiaryContainer = DarkPurple,

    error = ErrorRed,
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),

    background = BackgroundLight,
    onBackground = Color(0xFF1C1B1F),

    surface = SurfaceLight,
    onSurface = Color(0xFF1C1B1F),

    surfaceVariant = Color(0xFFE7E0EC),
    onSurfaceVariant = Color(0xFF49454F),

    outline = Color(0xFF79747E)
)

@Composable
fun AcneDetectionTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}