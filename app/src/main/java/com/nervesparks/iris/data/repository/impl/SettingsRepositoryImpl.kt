package com.nervesparks.iris.data.repository.impl

import android.util.Log
import com.nervesparks.iris.data.UserPreferencesRepository
import com.nervesparks.iris.data.repository.SettingsRepository
import com.nervesparks.iris.data.repository.ThinkingTokenSettings
import com.nervesparks.iris.data.repository.PerformanceSettings
import com.nervesparks.iris.data.repository.UISettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import javax.inject.Inject

/**
 * Implementation of SettingsRepository using UserPreferencesRepository
 */
class SettingsRepositoryImpl @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository
) : SettingsRepository {
    
    private val tag = "SettingsRepositoryImpl"
    
    override suspend fun getDefaultModelName(): String {
        return withContext(Dispatchers.IO) {
            try {
                userPreferencesRepository.defaultModelName
            } catch (e: Exception) {
                Log.e(tag, "Error getting default model name", e)
                ""
            }
        }
    }
    
    override suspend fun setDefaultModelName(modelName: String) {
        withContext(Dispatchers.IO) {
            try {
                userPreferencesRepository.defaultModelName = modelName
                Log.d(tag, "Default model name set to: $modelName")
            } catch (e: Exception) {
                Log.e(tag, "Error setting default model name", e)
            }
        }
    }
    
    override suspend fun getThinkingTokenSettings(): ThinkingTokenSettings {
        return withContext(Dispatchers.IO) {
            try {
                val showThinkingTokens = userPreferencesRepository.showThinkingTokens
                val thinkingTokenStyle = userPreferencesRepository.thinkingTokenStyle
                ThinkingTokenSettings(
                    showThinkingTokens = showThinkingTokens,
                    thinkingTokenStyle = thinkingTokenStyle
                )
            } catch (e: Exception) {
                Log.e(tag, "Error getting thinking token settings", e)
                ThinkingTokenSettings()
            }
        }
    }
    
    override suspend fun saveThinkingTokenSettings(settings: ThinkingTokenSettings) {
        withContext(Dispatchers.IO) {
            try {
                userPreferencesRepository.showThinkingTokens = settings.showThinkingTokens
                userPreferencesRepository.thinkingTokenStyle = settings.thinkingTokenStyle
                Log.d(tag, "Thinking token settings saved")
            } catch (e: Exception) {
                Log.e(tag, "Error saving thinking token settings", e)
            }
        }
    }
    
    override suspend fun getPerformanceSettings(): PerformanceSettings {
        return withContext(Dispatchers.IO) {
            try {
                PerformanceSettings(
                    threadCount = userPreferencesRepository.modelThreadCount,
                    maxContextLength = userPreferencesRepository.modelContextLength,
                    enableMemoryOptimization = userPreferencesRepository.perfEnableMemoryOptimization,
                    enableBackgroundProcessing = userPreferencesRepository.perfEnableBackgroundProcessing
                )
            } catch (e: Exception) {
                Log.e(tag, "Error getting performance settings", e)
                PerformanceSettings()
            }
        }
    }
    
    override suspend fun savePerformanceSettings(settings: PerformanceSettings) {
        withContext(Dispatchers.IO) {
            try {
                userPreferencesRepository.modelThreadCount = settings.threadCount
                userPreferencesRepository.modelContextLength = settings.maxContextLength
                userPreferencesRepository.perfEnableMemoryOptimization = settings.enableMemoryOptimization
                userPreferencesRepository.perfEnableBackgroundProcessing = settings.enableBackgroundProcessing
                Log.d(tag, "Performance settings saved")
            } catch (e: Exception) {
                Log.e(tag, "Error saving performance settings", e)
            }
        }
    }
    
    override suspend fun getUISettings(): UISettings {
        return withContext(Dispatchers.IO) {
            try {
                UISettings(
                    theme = userPreferencesRepository.uiTheme,
                    fontSize = userPreferencesRepository.uiFontSize,
                    enableAnimations = userPreferencesRepository.uiEnableAnimations,
                    enableHapticFeedback = userPreferencesRepository.uiEnableHapticFeedback
                )
            } catch (e: Exception) {
                Log.e(tag, "Error getting UI settings", e)
                UISettings()
            }
        }
    }
    
    override suspend fun saveUISettings(settings: UISettings) {
        withContext(Dispatchers.IO) {
            try {
                userPreferencesRepository.uiTheme = settings.theme
                userPreferencesRepository.uiFontSize = settings.fontSize
                userPreferencesRepository.uiEnableAnimations = settings.enableAnimations
                userPreferencesRepository.uiEnableHapticFeedback = settings.enableHapticFeedback
                Log.d(tag, "UI settings saved")
            } catch (e: Exception) {
                Log.e(tag, "Error saving UI settings", e)
            }
        }
    }
    
    override suspend fun exportSettings(): String {
        return withContext(Dispatchers.IO) {
            try {
                userPreferencesRepository.exportConfiguration()
            } catch (e: Exception) {
                Log.e(tag, "Error exporting settings", e)
                "{}"
            }
        }
    }
    
    override suspend fun importSettings(json: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val success = userPreferencesRepository.importConfiguration(json)
                if (success) {
                    Log.d(tag, "Settings imported successfully")
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("Failed to import settings"))
                }
            } catch (e: Exception) {
                Log.e(tag, "Error importing settings", e)
                Result.failure(e)
            }
        }
    }
    
    override suspend fun resetToDefaults() {
        withContext(Dispatchers.IO) {
            try {
                userPreferencesRepository.clearAll()
                Log.d(tag, "Settings reset to defaults")
            } catch (e: Exception) {
                Log.e(tag, "Error resetting settings", e)
            }
        }
    }
} 