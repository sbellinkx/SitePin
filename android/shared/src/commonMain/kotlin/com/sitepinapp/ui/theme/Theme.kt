package com.sitepinapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = SitePinBlue,
    onPrimary = Color.White,
    primaryContainer = SitePinLightBlue,
    onPrimaryContainer = SitePinDarkBlue,
    secondary = SitePinDarkBlue,
    onSecondary = Color.White,
    secondaryContainer = SitePinLightBlue,
    onSecondaryContainer = SitePinDarkBlue,
    tertiary = Color(0xFF625B71),
    onTertiary = Color.White,
    error = Color(0xFFB3261E),
    onError = Color.White,
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
    background = Color.White,
    onBackground = Color(0xFF1C1B1F),
    surface = SurfaceLight,
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFE7E0EC),
    onSurfaceVariant = Color(0xFF49454F),
    outline = Color(0xFF79747E),
    outlineVariant = Color(0xFFCAC4D0)
)

private val DarkColorScheme = darkColorScheme(
    primary = SitePinLightBlue,
    onPrimary = SitePinDarkBlue,
    primaryContainer = SitePinBlue,
    onPrimaryContainer = Color.White,
    secondary = SitePinLightBlue,
    onSecondary = SitePinDarkBlue,
    secondaryContainer = Color(0xFF1A3A5C),
    onSecondaryContainer = SitePinLightBlue,
    tertiary = Color(0xFFCCC2DC),
    onTertiary = Color(0xFF332D41),
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC),
    background = Color(0xFF121212),
    onBackground = Color(0xFFE6E1E5),
    surface = SurfaceDark,
    onSurface = Color(0xFFE6E1E5),
    surfaceVariant = Color(0xFF49454F),
    onSurfaceVariant = Color(0xFFCAC4D0),
    outline = Color(0xFF938F99),
    outlineVariant = Color(0xFF49454F)
)

@Composable
fun SitePinTheme(
    themeMode: String = "system",
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = SitePinTypography,
        content = content
    )
}
