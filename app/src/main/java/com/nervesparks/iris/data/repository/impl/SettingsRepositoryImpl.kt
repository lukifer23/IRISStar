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
                userPreferencesRepository.getDefaultModelName()
            } catch (e: Exception) {
                Log.e(tag, "Error getting default model name", e)
                ""
            }
        }
    }
    
    override suspend fun setDefaultModelName(modelName: String) {
        withContext(Dispatchers.IO) {
            try {
                userPreferencesRepository.setDefaultModelName(modelName)
                Log.d(tag, "Default model name set to: $modelName")
            } catch (e: Exception) {
                Log.e(tag, "Error setting default model name", e)
            }
        }
    }
    
    override suspend fun getThinkingTokenSettings(): ThinkingTokenSettings {
        return withContext(Dispatchers.IO) {
            try {
                val showThinkingTokens = userPreferencesRepository.getShowThinkingTokens()
                val thinkingTokenStyle = userPreferencesRepository.getThinkingTokenStyle()
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
                userPreferencesRepository.setShowThinkingTokens(settings.showThinkingTokens)
                userPreferencesRepository.setThinkingTokenStyle(settings.thinkingTokenStyle)
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
                    threadCount = userPreferencesRepository.getModelThreadCount(),
                    maxContextLength = userPreferencesRepository.getModelContextLength(),
                    enableMemoryOptimization = userPreferencesRepository.getPerfEnableMemoryOptimization(),
                    enableBackgroundProcessing = userPreferencesRepository.getPerfEnableBackgroundProcessing()
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
                userPreferencesRepository.setModelThreadCount(settings.threadCount)
                userPreferencesRepository.setModelContextLength(settings.maxContextLength)
                userPreferencesRepository.setPerfEnableMemoryOptimization(settings.enableMemoryOptimization)
                userPreferencesRepository.setPerfEnableBackgroundProcessing(settings.enableBackgroundProcessing)
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
                    theme = userPreferencesRepository.getUITheme(),
                    fontSize = userPreferencesRepository.getUIFontSize(),
                    enableAnimations = userPreferencesRepository.getUIEnableAnimations(),
                    enableHapticFeedback = userPreferencesRepository.getUIEnableHapticFeedback()
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
                userPreferencesRepository.setUITheme(settings.theme)
                userPreferencesRepository.setUIFontSize(settings.fontSize)
                userPreferencesRepository.setUIEnableAnimations(settings.enableAnimations)
                userPreferencesRepository.setUIEnableHapticFeedback(settings.enableHapticFeedback)
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