package com.example.ui.theme

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

private val DarkColorScheme = darkColorScheme(
    primary = ZincAccent,
    onPrimary = CharcoalDark,
    primaryContainer = ZincAccentContainer,
    onPrimaryContainer = ZincTextPrimary,
    secondary = ZincTextSecondary,
    onSecondary = CharcoalDark,
    background = CharcoalDark,
    onBackground = ZincTextPrimary,
    surface = CharcoalSurface,
    onSurface = ZincTextPrimary,
    surfaceVariant = CharcoalSurfaceVariant,
    onSurfaceVariant = ZincTextSecondary,
    outline = CharcoalOutline,
    outlineVariant = CharcoalDivider
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF27272A),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE4E4E7),
    onPrimaryContainer = Color(0xFF18181B),
    secondary = Color(0xFF71717A),
    background = SlateLight,
    onBackground = SlateLightText,
    surface = SlateLightSurface,
    onSurface = SlateLightText,
    surfaceVariant = Color(0xFFF4F4F5),
    onSurfaceVariant = Color(0xFF52525B),
    outline = Color(0xFFE4E4E7),
    outlineVariant = Color(0xFFE5E7EB)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Default to dark theme per user specification
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}

