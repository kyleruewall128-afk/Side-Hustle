package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val AdPulseDarkColorScheme = darkColorScheme(
    primary = Emerald80,
    onPrimary = Emerald10,
    primaryContainer = Emerald15,
    onPrimaryContainer = Emerald90,
    secondary = Teal80,
    onSecondary = Emerald10,
    secondaryContainer = Teal20,
    onSecondaryContainer = Teal90,
    tertiary = Gold80,
    onTertiary = Gold20,
    tertiaryContainer = Color(0xFF451A03),
    onTertiaryContainer = Gold90,
    background = DarkBackground,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceMuted,
    surfaceContainerLow = DarkSurface,
    surfaceContainer = DarkSurfaceVariant,
    surfaceContainerHigh = DarkSurfaceElevated,
    outline = DarkOutline,
    error = ErrorRedDark,
    onError = ErrorContainerDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = Color(0xFFFCA5A5),
)

private val AdPulseLightColorScheme = lightColorScheme(
    primary = Emerald40,
    onPrimary = Color.White,
    primaryContainer = Emerald90,
    onPrimaryContainer = Emerald10,
    secondary = Teal40,
    onSecondary = Color.White,
    secondaryContainer = Teal90,
    onSecondaryContainer = Teal20,
    tertiary = Gold40,
    onTertiary = Color.White,
    tertiaryContainer = Gold90,
    onTertiaryContainer = Gold20,
    background = LightBackground,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceMuted,
    surfaceContainerLow = LightSurface,
    surfaceContainer = LightSurfaceVariant,
    surfaceContainerHigh = Color(0xFFDFE8E3),
    outline = LightOutline,
    error = ErrorRedLight,
    onError = Color.White,
    errorContainer = ErrorContainerLight,
    onErrorContainer = Color(0xFF7F1D1D),
)

val AdPulseShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun AdPulseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) AdPulseDarkColorScheme else AdPulseLightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !darkTheme
                controller.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AdPulseTypography,
        shapes = AdPulseShapes,
        content = content,
    )
}
