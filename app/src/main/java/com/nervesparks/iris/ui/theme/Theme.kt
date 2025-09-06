package com.nervesparks.iris.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.animation.core.AnimationConstants.DefaultDurationMillis
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkSecondary,
    onSecondary = DarkOnSecondary,
    secondaryContainer = DarkSecondaryContainer,
    onSecondaryContainer = DarkOnSecondaryContainer,
    tertiary = DarkTertiary,
    onTertiary = DarkOnTertiary,
    tertiaryContainer = DarkTertiaryContainer,
    onTertiaryContainer = DarkOnTertiaryContainer,
    error = DarkError,
    onError = DarkOnError,
    errorContainer = DarkErrorContainer,
    onErrorContainer = DarkOnErrorContainer,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    // Enhanced Material 3 colors
    surfaceTint = DarkPrimary,
    inverseSurface = LightSurface,
    inverseOnSurface = LightOnSurface,
    inversePrimary = LightPrimary,
    surfaceBright = Color(0xFF36343B),
    surfaceDim = Color(0xFF141218),
    surfaceContainer = Color(0xFF1F1D23),
    surfaceContainerHigh = Color(0xFF2A292E),
    surfaceContainerHighest = Color(0xFF353439),
    surfaceContainerLow = Color(0xFF1C1B1F),
    surfaceContainerLowest = Color(0xFF0F0E11),
)

private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    secondary = LightSecondary,
    onSecondary = LightOnSecondary,
    secondaryContainer = LightSecondaryContainer,
    onSecondaryContainer = LightOnSecondaryContainer,
    tertiary = LightTertiary,
    onTertiary = LightOnTertiary,
    tertiaryContainer = LightTertiaryContainer,
    onTertiaryContainer = LightOnTertiaryContainer,
    error = LightError,
    onError = LightOnError,
    errorContainer = LightErrorContainer,
    onErrorContainer = LightOnErrorContainer,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
    // Enhanced Material 3 colors
    surfaceTint = LightPrimary,
    inverseSurface = DarkSurface,
    inverseOnSurface = DarkOnSurface,
    inversePrimary = DarkPrimary,
    surfaceBright = Color(0xFFFEF7FF),
    surfaceDim = Color(0xFFDED8E1),
    surfaceContainer = Color(0xFFF3EDF7),
    surfaceContainerHigh = Color(0xFFECE6F0),
    surfaceContainerHighest = Color(0xFFE6E0EA),
    surfaceContainerLow = Color(0xFFF7F2FA),
    surfaceContainerLowest = Color(0xFFFFFFFF),
)

@Composable
fun IrisStarTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    // Enhanced theming options
    highContrast: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            // Use dynamic colors when available
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
            val insetsController = WindowCompat.getInsetsController(window, view)

            // Enhanced status bar theming
            window.statusBarColor = colorScheme.surface.toArgb()

            // Set navigation bar color for better integration
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                window.navigationBarColor = colorScheme.surface.toArgb()
            }

            // Configure status bar icon colors based on theme
            insetsController.isAppearanceLightStatusBars = !darkTheme

            // Configure navigation bar icon colors
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                insetsController.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}

// Enhanced animation specifications for consistent motion design
object IrisAnimations {
    // Standard transition durations
    const val INSTANT_DURATION = 0
    const val FAST_DURATION = 150
    const val NORMAL_DURATION = 300
    const val SLOW_DURATION = 500
    const val EXTRA_SLOW_DURATION = 700

    // Easing curves
    val StandardEasing = FastOutSlowInEasing
    val EmphasizedEasing = androidx.compose.animation.core.EaseInOutCubic
    val DecelerateEasing = androidx.compose.animation.core.EaseOutCubic
    val AccelerateEasing = androidx.compose.animation.core.EaseInCubic

    // Common transitions
    val FadeInTransition = fadeIn(
        animationSpec = tween(NORMAL_DURATION, easing = StandardEasing)
    )

    val FadeOutTransition = fadeOut(
        animationSpec = tween(NORMAL_DURATION, easing = StandardEasing)
    )

    val ScaleInTransition = scaleIn(
        initialScale = 0.8f,
        animationSpec = tween(NORMAL_DURATION, easing = EmphasizedEasing)
    )

    val ScaleOutTransition = scaleOut(
        targetScale = 0.8f,
        animationSpec = tween(NORMAL_DURATION, easing = EmphasizedEasing)
    )

    val SlideInFromBottom = fadeIn(
        animationSpec = tween(SLOW_DURATION, easing = DecelerateEasing)
    ) + scaleIn(
        initialScale = 0.9f,
        animationSpec = tween(SLOW_DURATION, easing = EmphasizedEasing)
    )

    val SlideOutToBottom = fadeOut(
        animationSpec = tween(FAST_DURATION, easing = AccelerateEasing)
    ) + scaleOut(
        targetScale = 0.9f,
        animationSpec = tween(FAST_DURATION, easing = EmphasizedEasing)
    )

    val SlideInFromRight = fadeIn(
        animationSpec = tween(NORMAL_DURATION, easing = StandardEasing)
    ) + slideInHorizontally(
        initialOffsetX = { it / 4 },
        animationSpec = tween(NORMAL_DURATION, easing = StandardEasing)
    )

    val SlideOutToLeft = fadeOut(
        animationSpec = tween(NORMAL_DURATION, easing = StandardEasing)
    ) + slideOutHorizontally(
        targetOffsetX = { -it / 4 },
        animationSpec = tween(NORMAL_DURATION, easing = StandardEasing)
    )

    // Enhanced transitions for different UI elements
    val CardEnterTransition = fadeIn(
        animationSpec = tween(FAST_DURATION, easing = StandardEasing)
    ) + scaleIn(
        initialScale = 0.95f,
        animationSpec = tween(FAST_DURATION, easing = StandardEasing)
    )

    val CardExitTransition = fadeOut(
        animationSpec = tween(FAST_DURATION, easing = StandardEasing)
    ) + scaleOut(
        targetScale = 0.95f,
        animationSpec = tween(FAST_DURATION, easing = StandardEasing)
    )

    val DialogEnterTransition = fadeIn(
        animationSpec = tween(NORMAL_DURATION, easing = EmphasizedEasing)
    ) + scaleIn(
        initialScale = 0.85f,
        animationSpec = tween(NORMAL_DURATION, easing = EmphasizedEasing)
    )

    val DialogExitTransition = fadeOut(
        animationSpec = tween(FAST_DURATION, easing = EmphasizedEasing)
    ) + scaleOut(
        targetScale = 0.85f,
        animationSpec = tween(FAST_DURATION, easing = EmphasizedEasing)
    )

    // Loading and progress animations
    val LoadingPulse = androidx.compose.animation.core.infiniteRepeatable<Float>(
        animation = tween(1000, easing = StandardEasing),
        repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
    )

    val ShimmerAnimation = androidx.compose.animation.core.infiniteRepeatable<Float>(
        animation = tween(1500, easing = StandardEasing),
        repeatMode = androidx.compose.animation.core.RepeatMode.Restart
    )

    // Button and interactive element animations
    val ButtonPressScale = scaleIn(
        initialScale = 0.95f,
        animationSpec = tween(FAST_DURATION, easing = StandardEasing)
    )

    val RippleEffect = scaleIn(
        initialScale = 0.1f,
        animationSpec = tween(FAST_DURATION, easing = EmphasizedEasing)
    )
}

// Enhanced composable for animated theme transitions
@Composable
fun AnimatedIrisStarTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    highContrast: Boolean = false,
    content: @Composable () -> Unit
) {
    // For now, just use the regular theme - animations can be added later
    IrisStarTheme(
        darkTheme = darkTheme,
        dynamicColor = dynamicColor,
        highContrast = highContrast,
        content = content
    )
}