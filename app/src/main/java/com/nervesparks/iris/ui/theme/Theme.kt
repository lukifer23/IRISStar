package com.nervesparks.iris.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = BrandBlue,
    onPrimary = White,
    primaryContainer = BrandBlue80,
    onPrimaryContainer = BrandBlue10,
    secondary = DarkBlue,
    onSecondary = White,
    secondaryContainer = LightBlue,
    onSecondaryContainer = DarkBlue,
    tertiary = Pink80,
    onTertiary = Black,
    tertiaryContainer = Pink40,
    onTertiaryContainer = White,
    error = Color(0xFFB00020),
    onError = White,
    background = Black,
    onBackground = White,
    surface = DarkBlue,
    onSurface = White,
)

private val LightColorScheme = lightColorScheme(
    primary = BrandBlue,
    onPrimary = White,
    primaryContainer = BrandBlue10,
    onPrimaryContainer = BrandBlue,
    secondary = LightBlue,
    onSecondary = Black,
    secondaryContainer = BrandBlue10,
    onSecondaryContainer = BrandBlue,
    tertiary = Pink40,
    onTertiary = White,
    tertiaryContainer = Pink80,
    onTertiaryContainer = Black,
    error = Color(0xFFB00020),
    onError = White,
    background = White,
    onBackground = Black,
    surface = Grey10,
    onSurface = Black,
)

@Composable
fun IrisStarTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}