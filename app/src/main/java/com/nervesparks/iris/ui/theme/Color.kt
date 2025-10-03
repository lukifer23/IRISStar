package com.nervesparks.iris.ui.theme

import androidx.compose.ui.graphics.Color

// Light Theme Colors
val LightPrimary = Color(0xFF6750A4)
val LightOnPrimary = Color(0xFFFFFFFF)
val LightPrimaryContainer = Color(0xFFEADDFF)
val LightOnPrimaryContainer = Color(0xFF21005D)
val LightSecondary = Color(0xFF625B71)
val LightOnSecondary = Color(0xFFFFFFFF)
val LightSecondaryContainer = Color(0xFFE8DEF8)
val LightOnSecondaryContainer = Color(0xFF1D1B20)
val LightTertiary = Color(0xFF7D5260)
val LightOnTertiary = Color(0xFFFFFFFF)
val LightTertiaryContainer = Color(0xFFFFD8E4)
val LightOnTertiaryContainer = Color(0xFF31111D)
val LightError = Color(0xFFB3261E)
val LightOnError = Color(0xFFFFFFFF)
val LightErrorContainer = Color(0xFFF9DEDC)
val LightOnErrorContainer = Color(0xFF410E0B)
val LightBackground = Color(0xFFFFFBFE)
val LightOnBackground = Color(0xFF1C1B1F)
val LightSurface = Color(0xFFFFFBFE)
val LightOnSurface = Color(0xFF1C1B1F)
val LightSurfaceVariant = Color(0xFFE7E0EC)
val LightOnSurfaceVariant = Color(0xFF49454F)
val LightOutline = Color(0xFF79747E)

// Dark Theme Colors
val DarkPrimary = Color(0xFFD0BCFF)
val DarkOnPrimary = Color(0xFF381E72)
val DarkPrimaryContainer = Color(0xFF4F378B)
val DarkOnPrimaryContainer = Color(0xFFEADDFF)
val DarkSecondary = Color(0xFFCCC2DC)
val DarkOnSecondary = Color(0xFF332D41)
val DarkSecondaryContainer = Color(0xFF4A4458)
val DarkOnSecondaryContainer = Color(0xFFE8DEF8)
val DarkTertiary = Color(0xFFEFB8C8)
val DarkOnTertiary = Color(0xFF492532)
val DarkTertiaryContainer = Color(0xFF633B48)
val DarkOnTertiaryContainer = Color(0xFFFFD8E4)
val DarkError = Color(0xFFF2B8B5)
val DarkOnError = Color(0xFF601410)
val DarkErrorContainer = Color(0xFF8C1D18)
val DarkOnErrorContainer = Color(0xFFF9DEDC)
val DarkBackground = Color(0xFF1C1B1F)
val DarkOnBackground = Color(0xFFE6E1E5)
val DarkSurface = Color(0xFF1C1B1F)
val DarkOnSurface = Color(0xFFE6E1E5)
val DarkSurfaceVariant = Color(0xFF49454F)
val DarkOnSurfaceVariant = Color(0xFFCAC4D0)
val DarkOutline = Color(0xFF938F99)

// Semantic Color Tokens for consistent usage across components
object SemanticColors {
    // Status colors using Material 3 theme colors
    val Success get() = androidx.compose.material3.MaterialTheme.colorScheme.primary
    val Warning get() = androidx.compose.material3.MaterialTheme.colorScheme.tertiary
    val Info get() = androidx.compose.material3.MaterialTheme.colorScheme.secondary

    // Interactive states using theme colors
    val Hover get() = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    val Pressed get() = androidx.compose.material3.MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
    val Disabled get() = androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)

    // Special purpose colors using Material 3 containers
    val ModalBackground get() = androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainerHighest
    val ModalSurface get() = androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainerHigh
    val ModalAccent get() = androidx.compose.material3.MaterialTheme.colorScheme.primaryContainer
    val LoadingBackground get() = androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainerLowest
    val LoadingAccent get() = androidx.compose.material3.MaterialTheme.colorScheme.primary
    val DownloadSurface get() = androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainer

    // Text variants using Material 3 theme
    val TextPrimary get() = androidx.compose.material3.MaterialTheme.colorScheme.onSurface
    val TextSecondary get() = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
    val TextDisabled get() = androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    val TextInverse get() = androidx.compose.material3.MaterialTheme.colorScheme.inverseOnSurface

    // Border and divider colors using Material 3
    val BorderPrimary get() = androidx.compose.material3.MaterialTheme.colorScheme.outline
    val BorderSecondary get() = androidx.compose.material3.MaterialTheme.colorScheme.outlineVariant
    val Divider get() = androidx.compose.material3.MaterialTheme.colorScheme.outlineVariant
}

// Gradient definitions for modern UI elements
object Gradients {
    // Primary gradients
    val Primary = listOf(Color(0xFF6750A4), Color(0xFF9C73D7))
    val Secondary = listOf(Color(0xFF625B71), Color(0xFF8B8494))
    val Surface = listOf(Color(0xFF1C1B1F), Color(0xFF2A292E))

    // Accent gradients
    val Success = listOf(Color(0xFF4CAF50), Color(0xFF81C784))
    val Warning = listOf(Color(0xFFFF9800), Color(0xFFFFB74D))
    val Error = listOf(Color(0xFFF44336), Color(0xFFE57373))

    // Special purpose gradients
    val ChatBubble = listOf(Color(0xFFD0BCFF), Color(0xFFEADDFF))
    val Loading = listOf(Color(0xFF6750A4), Color(0xFF9C73D7), Color(0xFFD0BCFF))
}

// Enhanced semantic colors for modern UI
object EnhancedSemanticColors {
    // Status colors with variations using Material 3 theme
    val SuccessLight get() = androidx.compose.material3.MaterialTheme.colorScheme.primaryContainer
    val SuccessMain get() = androidx.compose.material3.MaterialTheme.colorScheme.primary
    val SuccessDark get() = androidx.compose.material3.MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)

    val WarningLight get() = androidx.compose.material3.MaterialTheme.colorScheme.tertiaryContainer
    val WarningMain get() = androidx.compose.material3.MaterialTheme.colorScheme.tertiary
    val WarningDark get() = androidx.compose.material3.MaterialTheme.colorScheme.tertiary.copy(alpha = 0.8f)

    val ErrorLight get() = androidx.compose.material3.MaterialTheme.colorScheme.errorContainer
    val ErrorMain get() = androidx.compose.material3.MaterialTheme.colorScheme.error
    val ErrorDark get() = androidx.compose.material3.MaterialTheme.colorScheme.error.copy(alpha = 0.8f)

    val InfoLight get() = androidx.compose.material3.MaterialTheme.colorScheme.secondaryContainer
    val InfoMain get() = androidx.compose.material3.MaterialTheme.colorScheme.secondary
    val InfoDark get() = androidx.compose.material3.MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f)

    // Interactive states using Material 3
    val HoverOverlay get() = androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    val PressedOverlay get() = androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    val FocusRing get() = androidx.compose.material3.MaterialTheme.colorScheme.primary

    // Glass morphism colors using Material 3
    val GlassBackground get() = androidx.compose.material3.MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
    val GlassBorder get() = androidx.compose.material3.MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)

    // Chat specific colors using Material 3
    val UserMessage get() = androidx.compose.material3.MaterialTheme.colorScheme.primaryContainer
    val AssistantMessage get() = androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainerHigh
    val SystemMessage get() = androidx.compose.material3.MaterialTheme.colorScheme.tertiaryContainer
    val ErrorMessage get() = androidx.compose.material3.MaterialTheme.colorScheme.errorContainer

    // Model status colors using Material 3
    val ModelReady get() = androidx.compose.material3.MaterialTheme.colorScheme.primary
    val ModelLoading get() = androidx.compose.material3.MaterialTheme.colorScheme.tertiary
    val ModelError get() = androidx.compose.material3.MaterialTheme.colorScheme.error
    val ModelOffline get() = androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
}

// Extension functions for semantic color access
fun androidx.compose.material3.ColorScheme.semanticSuccess() = EnhancedSemanticColors.SuccessMain
fun androidx.compose.material3.ColorScheme.semanticWarning() = EnhancedSemanticColors.WarningMain
fun androidx.compose.material3.ColorScheme.semanticInfo() = EnhancedSemanticColors.InfoMain
fun androidx.compose.material3.ColorScheme.semanticError() = EnhancedSemanticColors.ErrorMain

// Convenience functions for common UI patterns
fun androidx.compose.material3.ColorScheme.cardBackground() = surfaceContainerHigh
fun androidx.compose.material3.ColorScheme.modalBackground() = SemanticColors.ModalBackground
fun androidx.compose.material3.ColorScheme.userMessageColor() = EnhancedSemanticColors.UserMessage
fun androidx.compose.material3.ColorScheme.assistantMessageColor() = EnhancedSemanticColors.AssistantMessage

// Enhanced interactive state colors
fun androidx.compose.material3.ColorScheme.hoverOverlay() = EnhancedSemanticColors.HoverOverlay
fun androidx.compose.material3.ColorScheme.pressedOverlay() = EnhancedSemanticColors.PressedOverlay
fun androidx.compose.material3.ColorScheme.focusRing() = EnhancedSemanticColors.FocusRing

// Chat-specific colors
fun androidx.compose.material3.ColorScheme.userMessage() = EnhancedSemanticColors.UserMessage
fun androidx.compose.material3.ColorScheme.assistantMessage() = EnhancedSemanticColors.AssistantMessage
fun androidx.compose.material3.ColorScheme.systemMessage() = EnhancedSemanticColors.SystemMessage

// Model status colors
fun androidx.compose.material3.ColorScheme.modelReady() = EnhancedSemanticColors.ModelReady
fun androidx.compose.material3.ColorScheme.modelLoading() = EnhancedSemanticColors.ModelLoading
fun androidx.compose.material3.ColorScheme.modelError() = EnhancedSemanticColors.ModelError
