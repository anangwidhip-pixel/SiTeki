package com.example.sitekiver01.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val ForgeColorScheme = darkColorScheme(
    primary = ForgePrimary,
    onPrimary = Color(0xFF032018),
    primaryContainer = Color(0xFF123A2D),
    onPrimaryContainer = Color(0xFFC9F8E3),
    secondary = ForgeBlue,
    onSecondary = Color(0xFF071A24),
    secondaryContainer = Color(0xFF173342),
    onSecondaryContainer = Color(0xFFD0EEFC),
    tertiary = ForgeAmber,
    onTertiary = Color(0xFF271904),
    error = ForgeRed,
    background = ForgeBase,
    onBackground = ForgeText,
    surface = ForgeSurface,
    onSurface = ForgeText,
    surfaceVariant = ForgeSurfaceHigh,
    onSurfaceVariant = ForgeTextMuted,
    outline = ForgeLine,
    outlineVariant = ForgeLine.copy(alpha = 0.55f),
    scrim = Color.Black
)

private val AtelierLightColorScheme = lightColorScheme(
    primary = AtelierPrimary,
    onPrimary = Color.White,
    primaryContainer = AtelierPrimarySoft,
    onPrimaryContainer = Color(0xFF003828),
    secondary = AtelierBlue,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD5EFF8),
    onSecondaryContainer = Color(0xFF072F3E),
    tertiary = AtelierAmber,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFDDB1),
    onTertiaryContainer = Color(0xFF432B00),
    error = Color(0xFFBA1A1A),
    background = AtelierPaper,
    onBackground = AtelierInk,
    surface = AtelierPaperRaised,
    onSurface = AtelierInk,
    surfaceVariant = Color(0xFFE8EEE9),
    onSurfaceVariant = AtelierInkMuted,
    outline = AtelierLine,
    outlineVariant = AtelierLine.copy(alpha = 0.7f),
    scrim = Color.Black
)

@Composable
fun SiTekiVer01Theme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) ForgeColorScheme else AtelierLightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = ForgeShapes,
        content = content
    )
}
