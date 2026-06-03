package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    secondary = Color(0xFF4F46E5),
    background = DayBackground,
    surface = DaySurface,
    onBackground = DayOnBackground,
    onSurface = DayOnBackground,
    surfaceVariant = DayCard,
    onSurfaceVariant = DayOnBackground
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    secondary = Color(0xFF6366F1),
    background = NightBackground,
    surface = NightSurface,
    onBackground = NightOnBackground,
    onSurface = NightOnBackground,
    surfaceVariant = NightCard,
    onSurfaceVariant = NightOnBackground
)

private val AmoledColorScheme = darkColorScheme(
    primary = DarkPrimary,
    secondary = Color(0xFF4F46E5),
    background = AmoledBackground,
    surface = AmoledSurface,
    onBackground = AmoledOnBackground,
    onSurface = AmoledOnBackground,
    surfaceVariant = AmoledCard,
    onSurfaceVariant = AmoledOnBackground
)

private val SepiaColorScheme = lightColorScheme(
    primary = Color(0xFF8B5A2B),
    secondary = Color(0xFF5C3317),
    background = SepiaBackground,
    surface = SepiaSurface,
    onBackground = SepiaOnBackground,
    onSurface = SepiaOnBackground,
    surfaceVariant = SepiaCard,
    onSurfaceVariant = SepiaOnBackground
)

private val SepiaContrastColorScheme = lightColorScheme(
    primary = Color(0xFF5C3317),
    secondary = Color(0xFF2E1C0C),
    background = SepiaContrastBackground,
    surface = SepiaContrastSurface,
    onBackground = SepiaContrastOnBackground,
    onSurface = SepiaContrastOnBackground,
    surfaceVariant = SepiaContrastCard,
    onSurfaceVariant = SepiaContrastOnBackground
)

private val TwilightColorScheme = darkColorScheme(
    primary = Color(0xFF818CF8),
    secondary = Color(0xFF4F46E5),
    background = TwilightBackground,
    surface = TwilightSurface,
    onBackground = TwilightOnBackground,
    onSurface = TwilightOnBackground,
    surfaceVariant = TwilightCard,
    onSurfaceVariant = TwilightOnBackground
)

@Composable
fun LibrovaTheme(
    themeName: String = "Follow Device",
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val colorScheme = when (themeName) {
        "Day" -> LightColorScheme
        "Night" -> DarkColorScheme
        "Sepia" -> SepiaColorScheme
        "Sepia Contrast" -> SepiaContrastColorScheme
        "Twilight" -> TwilightColorScheme
        "AMOLED Black" -> AmoledColorScheme
        else -> {
            // "Follow Device" defaults to AMOLED true black on dark mode, LightColorScheme on light mode
            if (systemDark) AmoledColorScheme else LightColorScheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
