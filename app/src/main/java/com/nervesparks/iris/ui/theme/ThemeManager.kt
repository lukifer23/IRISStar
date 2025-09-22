package com.nervesparks.iris.ui.theme

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nervesparks.iris.data.UserPreferencesRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * PHASE 3.2: Theme Manager - Comprehensive dark theme system
 * Manages theme preferences, dynamic switching, and system integration
 */

private val Context.themeDataStore: DataStore<Preferences> by preferencesDataStore(name = "theme_preferences")

private object ThemeKeys {
    val THEME_MODE = stringPreferencesKey("theme_mode") // SYSTEM, LIGHT, DARK
    val DYNAMIC_COLORS = booleanPreferencesKey("dynamic_colors")
    val HIGH_CONTRAST = booleanPreferencesKey("high_contrast")
    val MATERIAL_YOU = booleanPreferencesKey("material_you")
}

enum class ThemeMode {
    SYSTEM, LIGHT, DARK;

    fun toBoolean(): Boolean? = when (this) {
        SYSTEM -> null
        LIGHT -> false
        DARK -> true
    }

    companion object {
        fun fromBoolean(isDark: Boolean?): ThemeMode = when (isDark) {
            true -> DARK
            false -> LIGHT
            null -> SYSTEM
        }
    }
}

class ThemeViewModel(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val appContext: Context
) : ViewModel() {

    private val context = appContext

    // Theme state
    private val _themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _dynamicColors = MutableStateFlow(true)
    val dynamicColors: StateFlow<Boolean> = _dynamicColors.asStateFlow()

    private val _highContrast = MutableStateFlow(false)
    val highContrast: StateFlow<Boolean> = _highContrast.asStateFlow()

    private val _materialYou = MutableStateFlow(true)
    val materialYou: StateFlow<Boolean> = _materialYou.asStateFlow()

    // Computed theme state
    val isDarkTheme: StateFlow<Boolean?> = _themeMode
        .map { mode -> mode.toBoolean() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    init {
        loadThemePreferences()
    }

    private fun loadThemePreferences() {
        viewModelScope.launch {
            try {
                val preferences = context.themeDataStore.data.first()
                _themeMode.value = ThemeMode.valueOf(
                    preferences[ThemeKeys.THEME_MODE] ?: ThemeMode.SYSTEM.name
                )
                _dynamicColors.value = preferences[ThemeKeys.DYNAMIC_COLORS] ?: true
                _highContrast.value = preferences[ThemeKeys.HIGH_CONTRAST] ?: false
                _materialYou.value = preferences[ThemeKeys.MATERIAL_YOU] ?: true
            } catch (e: Exception) {
                // Use defaults on error
                _themeMode.value = ThemeMode.SYSTEM
                _dynamicColors.value = true
                _highContrast.value = false
                _materialYou.value = true
            }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            _themeMode.value = mode
            context.themeDataStore.edit { preferences ->
                preferences[ThemeKeys.THEME_MODE] = mode.name
            }
        }
    }

    fun setDynamicColors(enabled: Boolean) {
        viewModelScope.launch {
            _dynamicColors.value = enabled
            context.themeDataStore.edit { preferences ->
                preferences[ThemeKeys.DYNAMIC_COLORS] = enabled
            }
        }
    }

    fun setHighContrast(enabled: Boolean) {
        viewModelScope.launch {
            _highContrast.value = enabled
            context.themeDataStore.edit { preferences ->
                preferences[ThemeKeys.HIGH_CONTRAST] = enabled
            }
        }
    }

    fun setMaterialYou(enabled: Boolean) {
        viewModelScope.launch {
            _materialYou.value = enabled
            context.themeDataStore.edit { preferences ->
                preferences[ThemeKeys.MATERIAL_YOU] = enabled
            }
        }
    }

    fun toggleTheme() {
        val currentMode = _themeMode.value
        val newMode = when (currentMode) {
            ThemeMode.SYSTEM -> ThemeMode.DARK
            ThemeMode.DARK -> ThemeMode.LIGHT
            ThemeMode.LIGHT -> ThemeMode.SYSTEM
        }
        setThemeMode(newMode)
    }

    fun resetToDefaults() {
        setThemeMode(ThemeMode.SYSTEM)
        setDynamicColors(true)
        setHighContrast(false)
        setMaterialYou(true)
    }

    // Get current theme display name
    fun getCurrentThemeDisplayName(): String {
        return when (_themeMode.value) {
            ThemeMode.SYSTEM -> "System"
            ThemeMode.LIGHT -> "Light"
            ThemeMode.DARK -> "Dark"
        }
    }
}

// Composable for accessing theme state
@Composable
fun rememberThemeState(viewModel: ThemeViewModel): ThemeState {
    val themeMode by viewModel.themeMode.collectAsState()
    val dynamicColors by viewModel.dynamicColors.collectAsState()
    val highContrast by viewModel.highContrast.collectAsState()
    val materialYou by viewModel.materialYou.collectAsState()
    val isDarkTheme by viewModel.isDarkTheme.collectAsState()

    return ThemeState(
        themeMode = themeMode,
        dynamicColors = dynamicColors,
        highContrast = highContrast,
        materialYou = materialYou,
        isDarkTheme = isDarkTheme
    )
}

data class ThemeState(
    val themeMode: ThemeMode,
    val dynamicColors: Boolean,
    val highContrast: Boolean,
    val materialYou: Boolean,
    val isDarkTheme: Boolean?
)

// Enhanced theme composable with full theme management
@Composable
fun ManagedIrisStarTheme(
    themeViewModel: ThemeViewModel,
    content: @Composable () -> Unit
) {
    val themeState = rememberThemeState(themeViewModel)

    IrisStarTheme(
        darkTheme = themeState.isDarkTheme ?: isSystemInDarkTheme(),
        dynamicColor = themeState.dynamicColors && themeState.materialYou,
        highContrast = themeState.highContrast,
        content = content
    )
}

// Utility functions for theme-aware components
@Composable
fun isDarkTheme(): Boolean {
    return isSystemInDarkTheme()
}

@Composable
fun isLightTheme(): Boolean {
    return !isSystemInDarkTheme()
}

@Composable
fun isHighContrastMode(): Boolean {
    // This could be expanded to check theme preferences
    return false
}

// Theme-aware color selection
@Composable
fun themeAwareColor(
    lightColor: androidx.compose.ui.graphics.Color,
    darkColor: androidx.compose.ui.graphics.Color
): androidx.compose.ui.graphics.Color {
    return if (isDarkTheme()) darkColor else lightColor
}

// Theme-aware surface color with elevation
@Composable
fun surfaceColorForElevation(elevation: Int): androidx.compose.ui.graphics.Color {
    val colorScheme = MaterialTheme.colorScheme
    return when (elevation) {
        0 -> colorScheme.surface
        1 -> colorScheme.surfaceVariant
        2 -> colorScheme.surfaceContainerLow
        3 -> colorScheme.surfaceContainer
        4 -> colorScheme.surfaceContainerHigh
        else -> colorScheme.surfaceContainerHighest
    }
}

// Factory for creating ThemeViewModel instances
class ThemeViewModelFactory(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ThemeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ThemeViewModel(userPreferencesRepository, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
