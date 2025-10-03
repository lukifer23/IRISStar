package com.nervesparks.iris.platform

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.BackHandler
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import timber.log.Timber

/**
 * Android platform features and modern API integrations
 */
object AndroidPlatformFeatures {

    /**
     * Check if the device supports Android 14+ features
     */
    fun supportsAndroid14Features(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
    }

    /**
     * Check if the device supports Android 13+ features
     */
    fun supportsAndroid13Features(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    }

    /**
     * Check if the device supports Android 12+ features (Material You)
     */
    fun supportsAndroid12Features(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    }

    /**
     * Check if the device has notification permission (Android 13+)
     */
    fun hasNotificationPermission(context: Context): Boolean {
        return if (supportsAndroid13Features()) {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            true // Permission not required on older versions
        }
    }

    /**
     * Request notification permission (Android 13+)
     */
    fun requestNotificationPermission(activity: Activity, requestCode: Int) {
        if (supportsAndroid13Features() && !hasNotificationPermission(activity)) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                requestCode
            )
        }
    }
}

/**
 * Composable for handling predictive back gestures (Android 14+)
 */
@Composable
fun PredictiveBackHandler(
    enabled: Boolean = true,
    onBack: () -> Unit
) {
    if (AndroidPlatformFeatures.supportsAndroid14Features()) {
        val context = LocalContext.current
        var backCallCount by remember { mutableStateOf(0) }

        // For Android 14+, we would use PredictiveBackHandler
        // For now, use regular BackHandler as fallback
        BackHandler(enabled = enabled, onBack = onBack)
    } else {
        // Fallback for older Android versions
        BackHandler(enabled = enabled, onBack = onBack)
    }
}

/**
 * Check if the device supports per-app language preferences (Android 13+)
 */
fun supportsPerAppLanguagePreferences(): Boolean {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
}

/**
 * Get the current per-app language setting (Android 13+)
 */
fun getCurrentAppLanguage(context: Context): String? {
    return if (supportsPerAppLanguagePreferences()) {
        try {
            // This would require proper locale handling
            // For now, return null as placeholder
            null
        } catch (e: Exception) {
            Timber.e(e, "Error getting app language")
            null
        }
    } else {
        null
    }
}

/**
 * Set per-app language preference (Android 13+)
 */
fun setAppLanguage(context: Context, languageTag: String): Boolean {
    return if (supportsPerAppLanguagePreferences()) {
        try {
            // This would require proper locale handling
            // For now, return false as placeholder
            false
        } catch (e: Exception) {
            Timber.e(e, "Error setting app language")
            false
        }
    } else {
        false
    }
}
