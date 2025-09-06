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
    // Status colors
    val Success = Color(0xFF4CAF50)
    val Warning = Color(0xFFFF9800)
    val Info = Color(0xFF2196F3)
    
    // Interactive states
    val Hover = Color(0xFFE3F2FD)
    val Pressed = Color(0xFFBBDEFB)
    val Disabled = Color(0xFFE0E0E0)
    
    // Special purpose colors (replacing hardcoded values)
    val ModalBackground = Color(0xFF1a1a2e)
    val ModalSurface = Color(0xFF16213e)
    val ModalAccent = Color(0xFF0f3460)
    val LoadingBackground = Color(0xFF01081a)
    val LoadingAccent = Color(0xFF17246a)
    val DownloadSurface = Color(0xFF233340)
    
    // Text variants
    val TextPrimary = Color(0xFF1C1B1F)
    val TextSecondary = Color(0xFF49454F)
    val TextDisabled = Color(0xFF9E9E9E)
    val TextInverse = Color(0xFFFFFFFF)
    
    // Border and divider colors
    val BorderPrimary = Color(0xFF79747E)
    val BorderSecondary = Color(0xFFE7E0EC)
    val Divider = Color(0xFFE0E0E0)
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
    // Status colors with variations
    val SuccessLight = Color(0xFFE8F5E8)
    val SuccessMain = Color(0xFF4CAF50)
    val SuccessDark = Color(0xFF2E7D32)

    val WarningLight = Color(0xFFFFF8E1)
    val WarningMain = Color(0xFFFF9800)
    val WarningDark = Color(0xFFF57C00)

    val ErrorLight = Color(0xFFFFEBEE)
    val ErrorMain = Color(0xFFF44336)
    val ErrorDark = Color(0xFFC62828)

    val InfoLight = Color(0xFFE3F2FD)
    val InfoMain = Color(0xFF2196F3)
    val InfoDark = Color(0xFF1565C0)

    // Interactive states
    val HoverOverlay = Color(0x1F000000) // 12% black overlay
    val PressedOverlay = Color(0x331C1B1F) // 20% surface overlay
    val FocusRing = Color(0xFF6750A4)

    // Glass morphism colors
    val GlassBackground = Color(0x80FFFFFF) // 50% white
    val GlassBorder = Color(0x1FFFFFFF) // 12% white

    // Chat specific colors
    val UserMessage = Color(0xFF6750A4)
    val AssistantMessage = Color(0xFF2A292E)
    val SystemMessage = Color(0xFF4CAF50)
    val ErrorMessage = Color(0xFFF44336)

    // Model status colors
    val ModelReady = Color(0xFF4CAF50)
    val ModelLoading = Color(0xFFFF9800)
    val ModelError = Color(0xFFF44336)
    val ModelOffline = Color(0xFF9E9E9E)
}

// Extension functions for semantic color access
fun androidx.compose.material3.ColorScheme.semanticSuccess() = EnhancedSemanticColors.SuccessMain
fun androidx.compose.material3.ColorScheme.semanticWarning() = EnhancedSemanticColors.WarningMain
fun androidx.compose.material3.ColorScheme.semanticInfo() = EnhancedSemanticColors.InfoMain
fun androidx.compose.material3.ColorScheme.semanticError() = EnhancedSemanticColors.ErrorMain

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
