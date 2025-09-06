package com.nervesparks.iris.viewmodel

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nervesparks.iris.Template
import com.nervesparks.iris.data.UserPreferencesRepository
import com.nervesparks.iris.data.repository.SettingsRepository
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
                Log.d(tag, "All settings loaded")
            } catch (e: Exception) {
                Log.e(tag, "Error loading settings", e)
            }
        }
    }

    private fun loadModelSettings() {
        try {
            defaultModelName = userPreferencesRepository.defaultModelName
            huggingFaceToken = userPreferencesRepository.huggingFaceToken
            huggingFaceUsername = userPreferencesRepository.huggingFaceUsername
        } catch (e: Exception) {
            Log.e(tag, "Error loading model settings", e)
        }
    }

    private suspend fun loadPerformanceSettings() {
        try {
            // TODO: Load performance settings from repository
            Log.d(tag, "Performance settings loaded")
        } catch (e: Exception) {
            Log.e(tag, "Error loading performance settings", e)
        }
    }

    private suspend fun loadUISettings() {
        try {
            // TODO: Load UI settings from repository
            Log.d(tag, "UI settings loaded")
        } catch (e: Exception) {
            Log.e(tag, "Error loading UI settings", e)
        }
    }

    private fun loadSecuritySettings() {
        try {
            securityBiometricEnabled = userPreferencesRepository.securityBiometricEnabled
        } catch (e: Exception) {
            Log.e(tag, "Error loading security settings", e)
        }
    }

    private suspend fun loadThinkingTokenSettings() {
        try {
            // TODO: Load thinking token settings from repository
            Log.d(tag, "Thinking token settings loaded")
        } catch (e: Exception) {
            Log.e(tag, "Error loading thinking token settings", e)
        }
    }

    // Update functions
    fun updateDefaultModelName(modelName: String) {
        defaultModelName = modelName
        userPreferencesRepository.defaultModelName = modelName
        Log.d(tag, "Default model name updated: $modelName")
    }

    fun updateHuggingFaceToken(token: String) {
        val validation = InputValidator.validateHuggingFaceToken(token)
        if (validation.isValid) {
            huggingFaceToken = validation.sanitizedValue ?: token
            userPreferencesRepository.huggingFaceToken = huggingFaceToken
            Log.d(tag, "HuggingFace token updated and validated")
        } else {
            Log.w(tag, "Invalid HuggingFace token: ${validation.errorMessage}")
        }
    }

    fun updateHuggingFaceUsername(username: String) {
        val validation = InputValidator.validateUsername(username)
        if (validation.isValid) {
            huggingFaceUsername = validation.sanitizedValue ?: username
            userPreferencesRepository.huggingFaceUsername = huggingFaceUsername
            Log.d(tag, "HuggingFace username updated and validated: $huggingFaceUsername")
        } else {
            Log.w(tag, "Invalid username: ${validation.errorMessage}")
        }
    }

    fun updatePerformanceSettings(
        enableMemoryOptimization: Boolean,
        enableBackgroundProcessing: Boolean
    ) {
        perfEnableMemoryOptimization = enableMemoryOptimization
        perfEnableBackgroundProcessing = enableBackgroundProcessing
        // TODO: Save to repository
        Log.d(tag, "Performance settings updated")
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
        // TODO: Save to repository
        Log.d(tag, "UI settings updated")
    }

    fun updateSecuritySettings(enableBiometric: Boolean) {
        securityBiometricEnabled = enableBiometric
        userPreferencesRepository.securityBiometricEnabled = enableBiometric
        Log.d(tag, "Security settings updated: biometric=$enableBiometric")
    }

    fun updateThinkingTokenSettings(showThinking: Boolean, style: String) {
        showThinkingTokens = showThinking
        thinkingTokenStyle = style
        // TODO: Save to repository
        Log.d(tag, "Thinking token settings updated")
    }

    // Template management
    fun addTemplate(template: Template): Boolean {
        return try {
            val templates = userPreferencesRepository.getTemplates().toMutableList()
            templates.add(template)
            userPreferencesRepository.saveTemplates(templates)
            Log.d(tag, "Template added: ${template.name}")
            true
        } catch (e: Exception) {
            Log.e(tag, "Error adding template", e)
            false
        }
    }

    fun editTemplate(updated: Template): Boolean {
        return try {
            val templates = userPreferencesRepository.getTemplates().toMutableList()
            val index = templates.indexOfFirst { it.name == updated.name }
            if (index >= 0) {
                templates[index] = updated
                userPreferencesRepository.saveTemplates(templates)
                Log.d(tag, "Template edited: ${updated.name}")
                true
            } else {
                Log.e(tag, "Template not found for editing: ${updated.name}")
                false
            }
        } catch (e: Exception) {
            Log.e(tag, "Error editing template", e)
            false
        }
    }

    fun deleteTemplate(template: Template): Boolean {
        return try {
            val templates = userPreferencesRepository.getTemplates().toMutableList()
            templates.removeIf { it.name == template.name }
            userPreferencesRepository.saveTemplates(templates)
            Log.d(tag, "Template deleted: ${template.name}")
            true
        } catch (e: Exception) {
            Log.e(tag, "Error deleting template", e)
            false
        }
    }

    fun getTemplates(): List<Template> {
        return try {
            userPreferencesRepository.getTemplates()
        } catch (e: Exception) {
            Log.e(tag, "Error getting templates", e)
            emptyList()
        }
    }

    // Configuration export/import
    fun exportConfiguration(): String {
        return try {
            userPreferencesRepository.exportConfiguration()
        } catch (e: Exception) {
            Log.e(tag, "Error exporting configuration", e)
            "{}"
        }
    }

    fun importConfiguration(jsonString: String): Boolean {
        return try {
            val success = userPreferencesRepository.importConfiguration(jsonString)
            if (success) {
                loadSettings() // Reload all settings after import
                Log.d(tag, "Configuration imported successfully")
            }
            success
        } catch (e: Exception) {
            Log.e(tag, "Error importing configuration", e)
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
                Log.d(tag, "All settings cleared")
            } catch (e: Exception) {
                Log.e(tag, "Error clearing settings", e)
            }
        }
    }
}