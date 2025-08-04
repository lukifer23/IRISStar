package com.nervesparks.iris.data.repository.impl

import android.util.Log
import com.nervesparks.iris.data.UserPreferencesRepository
import com.nervesparks.iris.data.repository.SettingsRepository
import com.nervesparks.iris.data.repository.ThinkingTokenSettings
import com.nervesparks.iris.data.repository.PerformanceSettings
import com.nervesparks.iris.data.repository.UISettings
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import javax.inject.Inject

private data class AllSettings(
    val defaultModelName: String,
    val huggingFaceToken: String,
    val huggingFaceUsername: String,
    val modelTemperature: Float,
    val modelTopP: Float,
    val modelTopK: Int,
    val modelMaxTokens: Int,
    val modelContextLength: Int,
    val modelSystemPrompt: String,
    val modelChatFormat: String,
    val modelThreadCount: Int,
    val showThinkingTokens: Boolean,
    val thinkingTokenStyle: String,
    val uiTheme: String,
    val uiFontSize: Float,
    val uiEnableAnimations: Boolean,
    val uiEnableHapticFeedback: Boolean,
    val perfEnableMemoryOptimization: Boolean,
    val perfEnableBackgroundProcessing: Boolean
)

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
                val settings = AllSettings(
                    defaultModelName = userPreferencesRepository.getDefaultModelName(),
                    huggingFaceToken = userPreferencesRepository.getHuggingFaceToken(),
                    huggingFaceUsername = userPreferencesRepository.getHuggingFaceUsername(),
                    modelTemperature = userPreferencesRepository.getModelTemperature(),
                    modelTopP = userPreferencesRepository.getModelTopP(),
                    modelTopK = userPreferencesRepository.getModelTopK(),
                    modelMaxTokens = userPreferencesRepository.getModelMaxTokens(),
                    modelContextLength = userPreferencesRepository.getModelContextLength(),
                    modelSystemPrompt = userPreferencesRepository.getModelSystemPrompt(),
                    modelChatFormat = userPreferencesRepository.getModelChatFormat(),
                    modelThreadCount = userPreferencesRepository.getModelThreadCount(),
                    showThinkingTokens = userPreferencesRepository.getShowThinkingTokens(),
                    thinkingTokenStyle = userPreferencesRepository.getThinkingTokenStyle(),
                    uiTheme = userPreferencesRepository.getUITheme(),
                    uiFontSize = userPreferencesRepository.getUIFontSize(),
                    uiEnableAnimations = userPreferencesRepository.getUIEnableAnimations(),
                    uiEnableHapticFeedback = userPreferencesRepository.getUIEnableHapticFeedback(),
                    perfEnableMemoryOptimization = userPreferencesRepository.getPerfEnableMemoryOptimization(),
                    perfEnableBackgroundProcessing = userPreferencesRepository.getPerfEnableBackgroundProcessing()
                )
                Moshi.Builder().build().adapter(AllSettings::class.java).toJson(settings)
            } catch (e: Exception) {
                Log.e(tag, "Error exporting settings", e)
                "{}"
            }
        }
    }
    
    override suspend fun importSettings(json: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val adapter = Moshi.Builder().build().adapter(AllSettings::class.java)
                val settings = adapter.fromJson(json) ?: return@withContext Result.failure(Exception("Invalid JSON"))
                userPreferencesRepository.setDefaultModelName(settings.defaultModelName)
                userPreferencesRepository.setHuggingFaceToken(settings.huggingFaceToken)
                userPreferencesRepository.setHuggingFaceUsername(settings.huggingFaceUsername)
                userPreferencesRepository.setModelTemperature(settings.modelTemperature)
                userPreferencesRepository.setModelTopP(settings.modelTopP)
                userPreferencesRepository.setModelTopK(settings.modelTopK)
                userPreferencesRepository.setModelMaxTokens(settings.modelMaxTokens)
                userPreferencesRepository.setModelContextLength(settings.modelContextLength)
                userPreferencesRepository.setModelSystemPrompt(settings.modelSystemPrompt)
                userPreferencesRepository.setModelChatFormat(settings.modelChatFormat)
                userPreferencesRepository.setModelThreadCount(settings.modelThreadCount)
                userPreferencesRepository.setShowThinkingTokens(settings.showThinkingTokens)
                userPreferencesRepository.setThinkingTokenStyle(settings.thinkingTokenStyle)
                userPreferencesRepository.setUITheme(settings.uiTheme)
                userPreferencesRepository.setUIFontSize(settings.uiFontSize)
                userPreferencesRepository.setUIEnableAnimations(settings.uiEnableAnimations)
                userPreferencesRepository.setUIEnableHapticFeedback(settings.uiEnableHapticFeedback)
                userPreferencesRepository.setPerfEnableMemoryOptimization(settings.perfEnableMemoryOptimization)
                userPreferencesRepository.setPerfEnableBackgroundProcessing(settings.perfEnableBackgroundProcessing)
                Log.d(tag, "Settings imported successfully")
                Result.success(Unit)
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