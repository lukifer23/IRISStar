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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.ui.graphics.Color

// Modern dark color scheme matching reference images
private val DarkColorScheme = darkColorScheme(
    // Primary colors - deep blacks and dark grays
    primary = Color(0xFF050a14),           // Deep navy blue (main background)
    onPrimary = Color(0xFFFFFFFF),         // White text on primary
    primaryContainer = Color(0xFF051633),   // Slightly lighter navy
    onPrimaryContainer = Color(0xFFFFFFFF), // White text on primary container
    
    // Secondary colors - UI elements
    secondary = Color(0xFF16213e),         // Card backgrounds
    onSecondary = Color(0xFFFFFFFF),       // White text on secondary
    secondaryContainer = Color(0xFF171E2C), // Button backgrounds
    onSecondaryContainer = Color(0xFFA0A0A5), // Muted text on buttons
    
    // Tertiary colors - accents
    tertiary = Color(0xFF00BCD4),          // Cyan accent (buttons, highlights)
    onTertiary = Color(0xFFFFFFFF),        // White text on tertiary
    tertiaryContainer = Color(0xFF2196F3),  // Blue accent
    onTertiaryContainer = Color(0xFFFFFFFF), // White text on tertiary container
    
    // Background colors
    background = Color(0xFF000000),        // Pure black background
    onBackground = Color(0xFFFFFFFF),      // White text on background
    surface = Color(0xFF010825),           // Surface color for cards
    onSurface = Color(0xFFFFFFFF),         // White text on surface
    
    // Error colors
    error = Color(0xFFCF6679),             // Error red
    onError = Color(0xFFFFFFFF),           // White text on error
    errorContainer = Color(0xFFB00020),    // Error background
    onErrorContainer = Color(0xFFFFFFFF),  // White text on error container
    
    // Outline colors
    outline = Color(0xFF666666),           // Border color
    outlineVariant = Color(0xFFcfcfd1),    // Focused border color
    
    // Surface variants
    surfaceVariant = Color(0xFF01081a),    // Variant surface color
    onSurfaceVariant = Color(0xFFf5f5f5),  // Text on surface variant
    
    // Inverse colors
    inversePrimary = Color(0xFF00BCD4),    // Inverse primary
    inverseSurface = Color(0xFF16213e),    // Inverse surface
    
    // Scrim
    scrim = Color(0x80000000),            // Semi-transparent overlay
)

// Light color scheme (for completeness, but we're focusing on dark)
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF6650a4),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFEADDFF),
    onPrimaryContainer = Color(0xFF21005D),
    secondary = Color(0xFF625b71),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE8DEF8),
    onSecondaryContainer = Color(0xFF1D192B),
    tertiary = Color(0xFF7D5260),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFD8E4),
    onTertiaryContainer = Color(0xFF31111D),
    background = Color(0xFFFFFBFE),
    onBackground = Color(0xFF1C1B1F),
    surface = Color(0xFFFFFBFE),
    onSurface = Color(0xFF1C1B1F),
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
    outline = Color(0xFF79747E),
    outlineVariant = Color(0xFFCAC4D0),
    surfaceVariant = Color(0xFFE7E0EC),
    onSurfaceVariant = Color(0xFF49454F),
    inversePrimary = Color(0xFFD0BCFF),
    inverseSurface = Color(0xFF313033),
    scrim = Color(0x80000000),
)

@Composable
fun IRISTheme(
    darkTheme: Boolean = true, // Force dark theme for now
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false, // Disable dynamic color to maintain our custom theme
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
            // Set status bar color to match our theme
            window.statusBarColor = colorScheme.primary.toArgb()
            // Ensure status bar icons are light (white) on our dark background
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
