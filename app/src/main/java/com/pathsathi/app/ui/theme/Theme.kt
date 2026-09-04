package com.pathsathi.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Colors sampled from the PathSathi logo / design mockup
val TealPrimary = Color(0xFF13C2A6)
val TealDark = Color(0xFF0E9A85)
val GreenAccent = Color(0xFF8BD44C)
val OrangeAccent = Color(0xFFFF9F45)
val BlueAccent = Color(0xFF4E8FF7)
val SurfaceWhite = Color(0xFFFFFFFF)
val BackgroundLight = Color(0xFFF5F8F7)
val TextDark = Color(0xFF1B1F1E)
val MutedGray = Color(0xFF8A928F)
val CardBorder = Color(0xFFE7EEEC)

// Kept for compatibility with any code still referencing the old dark palette
val DeepBlack = Color(0xFF0B0F0E)
val SurfaceDark = Color(0xFF161B1A)
val OnSurfaceLight = Color(0xFFF2F2F2)

private val PathSathiLightColorScheme = lightColorScheme(
    primary = TealPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDFF7F2),
    onPrimaryContainer = TealDark,
    secondary = GreenAccent,
    onSecondary = Color.Black,
    background = BackgroundLight,
    onBackground = TextDark,
    surface = SurfaceWhite,
    onSurface = TextDark,
    surfaceVariant = Color(0xFFF0F4F3),
    onSurfaceVariant = MutedGray,
    outline = CardBorder,
    error = Color(0xFFE0463D)
)

private val PathSathiDarkColorScheme = darkColorScheme(
    primary = TealPrimary,
    onPrimary = Color.Black,
    primaryContainer = TealDark,
    onPrimaryContainer = Color(0xFFDFF7F2),
    secondary = GreenAccent,
    onSecondary = Color.Black,
    background = DeepBlack,
    onBackground = OnSurfaceLight,
    surface = SurfaceDark,
    onSurface = OnSurfaceLight,
    surfaceVariant = Color(0xFF20302D),
    onSurfaceVariant = Color(0xFFB6C2BF),
    outline = Color(0xFF3A4441),
    error = Color(0xFFFF6B60)
)

private val PathSathiShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun PathSathiTheme(darkTheme: Boolean = false, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) PathSathiDarkColorScheme else PathSathiLightColorScheme,
        typography = MaterialTheme.typography,
        shapes = PathSathiShapes,
        content = content
    )
}
