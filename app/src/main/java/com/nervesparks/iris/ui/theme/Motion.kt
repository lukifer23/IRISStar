package com.nervesparks.iris.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing

object Motion {
    val fast = 120
    val normal = 200
    val slow = 300

    val easeOut: Easing = CubicBezierEasing(0.0f, 0.0f, 0.2f, 1.0f)
    val easeInOut: Easing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
}


