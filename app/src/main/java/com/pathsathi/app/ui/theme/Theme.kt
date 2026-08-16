package com.pathsathi.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

// Path Sathi always ships in its signature dark + turquoise/green identity,
// regardless of system theme, per the approved brand direction.
private val PathSathiColorScheme = darkColorScheme(
    primary = PsTurquoise,
    onPrimary = PsBackground,
    secondary = PsGreen,
    onSecondary = PsBackground,
    background = PsBackground,
    onBackground = PsTextPrimary,
    surface = PsSurface,
    onSurface = PsTextPrimary,
    surfaceVariant = PsSurfaceAlt,
    onSurfaceVariant = PsTextSecondary,
    error = PsDanger,
)

@Composable
fun PathSathiTheme(
    useSystemDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = PathSathiColorScheme,
        typography = PsTypography,
        content = content
    )
}
