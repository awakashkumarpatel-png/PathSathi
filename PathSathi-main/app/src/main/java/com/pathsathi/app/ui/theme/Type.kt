package com.pathsathi.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val PsTypography = Typography(
    headlineLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 30.sp, color = PsTextPrimary),
    headlineMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 24.sp, color = PsTextPrimary),
    titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 20.sp, color = PsTextPrimary),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = PsTextPrimary),
    bodyLarge = TextStyle(fontWeight = FontWeight.Normal, fontSize = 16.sp, color = PsTextPrimary),
    bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 14.sp, color = PsTextSecondary),
    labelLarge = TextStyle(fontWeight = FontWeight.Medium, fontSize = 14.sp, color = PsTextPrimary),
    labelSmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = 11.sp, color = PsTextSecondary),
)
