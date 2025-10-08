package com.nervesparks.iris.viewmodel

import timber.log.Timber
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nervesparks.iris.Template
import com.nervesparks.iris.data.UserPreferencesRepository
import com.nervesparks.iris.data.repository.SettingsRepository
import com.nervesparks.iris.data.repository.ThinkingTokenSettings
import com.nervesparks.iris.data.repository.UISettings
import com.nervesparks.iris.security.InputValidator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * PHASE 1.5: SettingsViewModel - Extracted from MainViewModel
 * Handles configuration, preferences, and settings management
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val tag = "SettingsViewModel"

    // Model settings
    var defaultModelName by mutableStateOf("")
    var huggingFaceToken by mutableStateOf("")
    var huggingFaceUsername by mutableStateOf("")

    // Performance settings
    var perfEnableMemoryOptimization by mutableStateOf(true)
    var perfEnableBackgroundProcessing by mutableStateOf(true)

    // UI settings
    var uiTheme by mutableStateOf("DARK")
    var uiFontSize by mutableStateOf(1.0f)
    var uiEnableAnimations by mutableStateOf(true)
    var uiEnableHapticFeedback by mutableStateOf(true)

    // Security settings
    var securityBiometricEnabled by mutableStateOf(false)

    // Thinking tokens settings
    var showThinkingTokens by mutableStateOf(true)
    var thinkingTokenStyle by mutableStateOf("COLLAPSIBLE")

    init {
        loadSettings()
    }

    // Load all settings
    private fun loadSettings() {
        viewModelScope.launch {
            try {
                loadModelSettings()
                loadPerformanceSettings()
                loadUISettings()
                loadSecuritySettings()
                loadThinkingTokenSettings()
                Timber.tag(tag).d("All settings loaded")
            } catch (e: Exception) {
                Timber.tag(tag).e(e, "Error loading settings")
            }
        }
    }

    private suspend fun loadModelSettings() {
        try {
            defaultModelName = userPreferencesRepository.getDefaultModelName()
            huggingFaceToken = userPreferencesRepository.huggingFaceToken
            huggingFaceUsername = userPreferencesRepository.huggingFaceUsername
        } catch (e: Exception) {
            Timber.tag(tag).e(e, "Error loading model settings")
        }
    }

    private suspend fun loadPerformanceSettings() {
        try {
            val performanceSettings = settingsRepository.getPerformanceSettings()
            perfEnableMemoryOptimization = performanceSettings.enableMemoryOptimization
            perfEnableBackgroundProcessing = performanceSettings.enableBackgroundProcessing
            Timber.tag(tag).d("Performance settings loaded")
        } catch (e: Exception) {
            Timber.tag(tag).e(e, "Error loading performance settings")
        }
    }

    private suspend fun loadUISettings() {
        try {
            val uiSettings = settingsRepository.getUISettings()
            uiTheme = uiSettings.theme
            uiFontSize = uiSettings.fontSize
            uiEnableAnimations = uiSettings.enableAnimations
            uiEnableHapticFeedback = uiSettings.enableHapticFeedback
            Timber.tag(tag).d("UI settings loaded")
        } catch (e: Exception) {
            Timber.tag(tag).e(e, "Error loading UI settings")
        }
    }

    private suspend fun loadSecuritySettings() {
        try {
            securityBiometricEnabled = userPreferencesRepository.getSecurityBiometricEnabled()
        } catch (e: Exception) {
            Timber.tag(tag).e(e, "Error loading security settings")
        }
    }

    private suspend fun loadThinkingTokenSettings() {
        try {
            val thinkingTokenSettings = settingsRepository.getThinkingTokenSettings()
            showThinkingTokens = thinkingTokenSettings.showThinkingTokens
            thinkingTokenStyle = thinkingTokenSettings.thinkingTokenStyle
            Timber.tag(tag).d("Thinking token settings loaded")
        } catch (e: Exception) {
            Timber.tag(tag).e(e, "Error loading thinking token settings")
        }
    }

    // Update functions
    fun updateDefaultModelName(modelName: String) {
        defaultModelName = modelName
        viewModelScope.launch {
            userPreferencesRepository.setDefaultModelName(modelName)
        }
        Timber.tag(tag).d("Default model name updated: $modelName")
    }

    fun updateHuggingFaceToken(token: String) {
        val validation = InputValidator.validateHuggingFaceToken(token)
        if (validation.isValid) {
            huggingFaceToken = validation.sanitizedValue ?: token
            userPreferencesRepository.huggingFaceToken = huggingFaceToken
            Timber.tag(tag).d("HuggingFace token updated and validated")
        } else {
            Timber.tag(tag).w("Invalid HuggingFace token: ${validation.errorMessage}")
        }
    }

    fun updateHuggingFaceUsername(username: String) {
        val validation = InputValidator.validateUsername(username)
        if (validation.isValid) {
            huggingFaceUsername = validation.sanitizedValue ?: username
            userPreferencesRepository.huggingFaceUsername = huggingFaceUsername
            Timber.tag(tag).d("HuggingFace username updated and validated: $huggingFaceUsername")
        } else {
            Timber.tag(tag).w("Invalid username: ${validation.errorMessage}")
        }
    }

    fun updatePerformanceSettings(
        enableMemoryOptimization: Boolean,
        enableBackgroundProcessing: Boolean
    ) {
        perfEnableMemoryOptimization = enableMemoryOptimization
        perfEnableBackgroundProcessing = enableBackgroundProcessing
        viewModelScope.launch {
            try {
                val current = settingsRepository.getPerformanceSettings()
                val updated = current.copy(
                    enableMemoryOptimization = enableMemoryOptimization,
                    enableBackgroundProcessing = enableBackgroundProcessing
                )
                settingsRepository.savePerformanceSettings(updated)
            } catch (e: Exception) {
                Timber.tag(tag).e(e, "Error saving performance settings")
            }
        }
        Timber.tag(tag).d("Performance settings updated")
    }

    fun updateUISettings(
        theme: String,
        fontSize: Float,
        enableAnimations: Boolean,
        enableHapticFeedback: Boolean
    ) {
        uiTheme = theme
        uiFontSize = fontSize
        uiEnableAnimations = enableAnimations
        uiEnableHapticFeedback = enableHapticFeedback
        viewModelScope.launch {
            try {
                val settings = UISettings(
                    theme = theme,
                    fontSize = fontSize,
                    enableAnimations = enableAnimations,
                    enableHapticFeedback = enableHapticFeedback
                )
                settingsRepository.saveUISettings(settings)
            } catch (e: Exception) {
                Timber.tag(tag).e(e, "Error saving UI settings")
            }
        }
        Timber.tag(tag).d("UI settings updated")
    }

    fun updateSecuritySettings(enableBiometric: Boolean) {
        securityBiometricEnabled = enableBiometric
        viewModelScope.launch {
            userPreferencesRepository.setSecurityBiometricEnabled(enableBiometric)
        }
        Timber.tag(tag).d("Security settings updated: biometric=$enableBiometric")
    }

    fun updateThinkingTokenSettings(showThinking: Boolean, style: String) {
        showThinkingTokens = showThinking
        thinkingTokenStyle = style
        viewModelScope.launch {
            try {
                val settings = ThinkingTokenSettings(
                    showThinkingTokens = showThinking,
                    thinkingTokenStyle = style
                )
                settingsRepository.saveThinkingTokenSettings(settings)
            } catch (e: Exception) {
                Timber.tag(tag).e(e, "Error saving thinking token settings")
            }
        }
        Timber.tag(tag).d("Thinking token settings updated")
    }

    // Template management
    suspend fun addTemplate(template: Template): Boolean {
        return try {
            val templates = userPreferencesRepository.getTemplates().toMutableList()
            templates.add(template)
            userPreferencesRepository.saveTemplates(templates)
            Timber.tag(tag).d("Template added: ${template.name}")
            true
        } catch (e: Exception) {
            Timber.tag(tag).e(e, "Error adding template")
            false
        }
    }

    suspend fun editTemplate(updated: Template): Boolean {
        return try {
            val templates = userPreferencesRepository.getTemplates().toMutableList()
            val index = templates.indexOfFirst { it.name == updated.name }
            if (index >= 0) {
                templates[index] = updated
                userPreferencesRepository.saveTemplates(templates)
                Timber.tag(tag).d("Template edited: ${updated.name}")
                true
            } else {
                Timber.tag(tag).e("Template not found for editing: ${updated.name}")
                false
            }
        } catch (e: Exception) {
            Timber.tag(tag).e(e, "Error editing template")
            false
        }
    }

    suspend fun deleteTemplate(template: Template): Boolean {
        return try {
            val templates = userPreferencesRepository.getTemplates().toMutableList()
            templates.removeIf { it.name == template.name }
            userPreferencesRepository.saveTemplates(templates)
            Timber.tag(tag).d("Template deleted: ${template.name}")
            true
        } catch (e: Exception) {
            Timber.tag(tag).e(e, "Error deleting template")
            false
        }
    }

    suspend fun getTemplates(): List<Template> {
        return try {
            userPreferencesRepository.getTemplates()
        } catch (e: Exception) {
            Timber.tag(tag).e(e, "Error getting templates")
            emptyList()
        }
    }

    // Configuration export/import
    suspend fun exportConfiguration(): String {
        return try {
            userPreferencesRepository.exportConfiguration()
        } catch (e: Exception) {
            Timber.tag(tag).e(e, "Error exporting configuration")
            "{}"
        }
    }

    suspend fun importConfiguration(jsonString: String): Boolean {
        return try {
            val success = userPreferencesRepository.importConfiguration(jsonString)
            if (success) {
                loadSettings() // Reload all settings after import
                Timber.tag(tag).d("Configuration imported successfully")
            }
            success
        } catch (e: Exception) {
            Timber.tag(tag).e(e, "Error importing configuration")
            false
        }
    }

    // Utility functions
    fun hasHuggingFaceCredentials(): Boolean {
        return huggingFaceToken.isNotEmpty() || huggingFaceUsername.isNotEmpty()
    }

    fun clearAllSettings() {
        viewModelScope.launch {
            try {
                userPreferencesRepository.clearAll()
                loadSettings() // Reset to defaults
                Timber.tag(tag).d("All settings cleared")
            } catch (e: Exception) {
                Timber.tag(tag).e(e, "Error clearing settings")
            }
        }
    }
}