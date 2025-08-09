package com.nervesparks.iris.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape

object Shapes {
    val radiusXs = RoundedCornerShape(8)
    val radiusSm = RoundedCornerShape(12)
    val radiusMd = RoundedCornerShape(16)
    val radiusLg = RoundedCornerShape(20)
    val pill = RoundedCornerShape(28)
}

object ChatShapes {
    // Symmetric bubble with tighter inward corner for the tail side (dp integers are interpreted as dp)
    val incoming = RoundedCornerShape(16, 16, 16, 8)
    val outgoing = RoundedCornerShape(16, 16, 8, 16)
}


