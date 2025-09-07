package com.nervesparks.iris.ui.util

import android.app.Activity
import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * Simple wrapper around [WindowSizeClass] providing the current width and height
 * classifications for the host activity.
 */
 data class WindowClass(
     val width: WindowWidthSizeClass,
     val height: WindowHeightSizeClass
 )

@Composable
fun rememberWindowClass(): WindowClass {
    val activity = LocalContext.current as? Activity
    return if (activity != null) {
        val sizeClass = calculateWindowSizeClass(activity)
        WindowClass(sizeClass.widthSizeClass, sizeClass.heightSizeClass)
    } else {
        WindowClass(WindowWidthSizeClass.Compact, WindowHeightSizeClass.Compact)
    }
}
