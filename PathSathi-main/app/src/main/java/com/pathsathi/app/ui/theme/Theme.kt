package com.pathsathi.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ColorWhite = Color(0xFFFFFFFF)

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
