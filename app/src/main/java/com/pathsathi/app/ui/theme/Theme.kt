package com.pathsathi.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val PathSathiColorScheme = lightColorScheme(
    primary = PsTurquoise,
    onPrimary = ColorWhite,
    secondary = PsGreen,
    onSecondary = ColorWhite,
    background = PsBackground,
    onBackground = PsTextPrimary,
    surface = PsSurface,
    onSurface = PsTextPrimary,
    surfaceVariant = PsSurfaceAlt,
    onSurfaceVariant = PsTextSecondary,
    error = PsDanger,
)

private val ColorWhite = androidx.compose.ui.graphics.Color(0xFFFFFFFF)

@Composable
fun PathSathiTheme(
    useSystemDarkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = PathSathiColorScheme,
        typography = PsTypography,
        content = content
    )
}
