package com.nervesparks.iris.data.repository.impl

import timber.log.Timber
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
                Timber.tag(tag).e(e, "Error getting default model name")
                ""
            }
        }
    }
    
    override suspend fun setDefaultModelName(modelName: String) {
        withContext(Dispatchers.IO) {
            try {
                userPreferencesRepository.defaultModelName = modelName
                Timber.tag(tag).d("Default model name set to: $modelName")
            } catch (e: Exception) {
                Timber.tag(tag).e(e, "Error setting default model name")
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
                Timber.tag(tag).e(e, "Error getting thinking token settings")
                ThinkingTokenSettings()
            }
        }
    }
    
    override suspend fun saveThinkingTokenSettings(settings: ThinkingTokenSettings) {
        withContext(Dispatchers.IO) {
            try {
                userPreferencesRepository.showThinkingTokens = settings.showThinkingTokens
                userPreferencesRepository.thinkingTokenStyle = settings.thinkingTokenStyle
                Timber.tag(tag).d("Thinking token settings saved")
            } catch (e: Exception) {
                Timber.tag(tag).e(e, "Error saving thinking token settings")
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
                Timber.tag(tag).e(e, "Error getting performance settings")
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
                Timber.tag(tag).d("Performance settings saved")
            } catch (e: Exception) {
                Timber.tag(tag).e(e, "Error saving performance settings")
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
                Timber.tag(tag).e(e, "Error getting UI settings")
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
                Timber.tag(tag).d("UI settings saved")
            } catch (e: Exception) {
                Timber.tag(tag).e(e, "Error saving UI settings")
            }
        }
    }
    
    override suspend fun exportSettings(): String {
        return withContext(Dispatchers.IO) {
            try {
                userPreferencesRepository.exportConfiguration()
            } catch (e: Exception) {
                Timber.tag(tag).e(e, "Error exporting settings")
                "{}"
            }
        }
    }
    
    override suspend fun importSettings(json: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val success = userPreferencesRepository.importConfiguration(json)
                if (success) {
                    Timber.tag(tag).d("Settings imported successfully")
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("Failed to import settings"))
                }
            } catch (e: Exception) {
                Timber.tag(tag).e(e, "Error importing settings")
                Result.failure(e)
            }
        }
    }
    
    override suspend fun resetToDefaults() {
        withContext(Dispatchers.IO) {
            try {
                userPreferencesRepository.clearAll()
                Timber.tag(tag).d("Settings reset to defaults")
            } catch (e: Exception) {
                Timber.tag(tag).e(e, "Error resetting settings")
            }
        }
    }
} 