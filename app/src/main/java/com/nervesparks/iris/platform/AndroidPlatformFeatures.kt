package com.nervesparks.iris.platform

import android.app.Activity
import android.app.LocaleManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat
import com.nervesparks.iris.data.UserPreferencesRepository
import kotlinx.coroutines.runBlocking
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
    return try {
        if (supportsPerAppLanguagePreferences()) {
            val localeFromManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val localeManager = context.getSystemService(LocaleManager::class.java)
                val locales = localeManager?.applicationLocales
                if (locales != null && !locales.isEmpty) {
                    locales[0]?.toLanguageTag()
                } else {
                    null
                }
            } else {
                null
            }

            localeFromManager
                ?: AppCompatDelegate.getApplicationLocales()
                    .takeIf { !it.isEmpty }
                    ?.get(0)
                    ?.toLanguageTag()
                ?: getDefaultLocaleFromResources(context)
        } else {
            AppCompatDelegate.getApplicationLocales()
                .takeIf { !it.isEmpty }
                ?.get(0)
                ?.toLanguageTag()
                ?: getDefaultLocaleFromResources(context)
        }
    } catch (e: Exception) {
        Timber.e(e, "Error getting app language")
        null
    }
}

/**
 * Set per-app language preference (Android 13+)
 */
fun setAppLanguage(context: Context, languageTag: String): Boolean {
    return try {
        val sanitizedTag = languageTag.trim()
        val localeListCompat = if (sanitizedTag.isEmpty()) {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(sanitizedTag)
        }

        if (supportsPerAppLanguagePreferences()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val localeManager = context.getSystemService(LocaleManager::class.java)
                val localeList = if (sanitizedTag.isEmpty()) {
                    android.os.LocaleList.getEmptyLocaleList()
                } else {
                    android.os.LocaleList.forLanguageTags(sanitizedTag)
                }
                localeManager?.setApplicationLocales(localeList)
            } else {
                AppCompatDelegate.setApplicationLocales(localeListCompat)
            }
        } else {
            AppCompatDelegate.setApplicationLocales(localeListCompat)
        }

        val preferences = UserPreferencesRepository.getInstance(context)
        runBlocking {
            preferences.setAppLanguage(sanitizedTag.ifEmpty { null })
        }
        true
    } catch (e: Exception) {
        Timber.e(e, "Error setting app language")
        false
    }
}

private fun getDefaultLocaleFromResources(context: Context): String? {
    val configuration = context.resources.configuration
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        configuration.locales.get(0)?.toLanguageTag()
    } else {
        @Suppress("DEPRECATION")
        configuration.locale?.toLanguageTag()
    }
}
