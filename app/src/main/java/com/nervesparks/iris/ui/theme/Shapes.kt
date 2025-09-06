package com.nervesparks.iris.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Enhanced Material 3 Shapes system
val Shapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

// Legacy shapes for backward compatibility
object LegacyShapes {
    val radiusXs = RoundedCornerShape(8.dp)
    val radiusSm = RoundedCornerShape(12.dp)
    val radiusMd = RoundedCornerShape(16.dp)
    val radiusLg = RoundedCornerShape(20.dp)
    val pill = RoundedCornerShape(28.dp)
}

// Chat-specific shapes
object ChatShapes {
    // Symmetric bubble with tighter inward corner for the tail side
    val incoming = RoundedCornerShape(16.dp, 16.dp, 16.dp, 8.dp)
    val outgoing = RoundedCornerShape(16.dp, 16.dp, 8.dp, 16.dp)

    // Enhanced chat bubble shapes for better visual hierarchy
    val system = RoundedCornerShape(12.dp)
    val code = RoundedCornerShape(8.dp)
    val quote = RoundedCornerShape(4.dp, 12.dp, 12.dp, 12.dp)
}

// Card and surface shapes
object SurfaceShapes {
    val card = RoundedCornerShape(12.dp)
    val modal = RoundedCornerShape(16.dp)
    val bottomSheet = RoundedCornerShape(20.dp, 20.dp, 0.dp, 0.dp)
    val tooltip = RoundedCornerShape(8.dp)
    val menu = RoundedCornerShape(12.dp)
}

// Button and interactive element shapes
object InteractiveShapes {
    val button = RoundedCornerShape(20.dp)
    val fab = RoundedCornerShape(16.dp)
    val chip = RoundedCornerShape(8.dp)
    val textField = RoundedCornerShape(12.dp)
    val switch = RoundedCornerShape(16.dp)
}


