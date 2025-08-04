package com.nervesparks.iris.data.repository.impl

import android.util.Log
import com.nervesparks.iris.data.UserPreferencesRepository
import com.nervesparks.iris.data.repository.SettingsRepository
import com.nervesparks.iris.data.repository.ThinkingTokenSettings
import com.nervesparks.iris.data.repository.PerformanceSettings
import com.nervesparks.iris.data.repository.UISettings
import com.nervesparks.iris.data.repository.MetricsSnapshot
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
    private val metricsHistory = mutableListOf<MetricsSnapshot>()
    
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
                // TODO: Implement actual performance settings loading
                PerformanceSettings()
            } catch (e: Exception) {
                Log.e(tag, "Error getting performance settings", e)
                PerformanceSettings()
            }
        }
    }
    
    override suspend fun savePerformanceSettings(settings: PerformanceSettings) {
        withContext(Dispatchers.IO) {
            try {
                // TODO: Implement actual performance settings saving
                Log.d(tag, "Performance settings saved")
            } catch (e: Exception) {
                Log.e(tag, "Error saving performance settings", e)
            }
        }
    }
    
    override suspend fun getUISettings(): UISettings {
        return withContext(Dispatchers.IO) {
            try {
                // TODO: Implement actual UI settings loading
                UISettings()
            } catch (e: Exception) {
                Log.e(tag, "Error getting UI settings", e)
                UISettings()
            }
        }
    }
    
    override suspend fun saveUISettings(settings: UISettings) {
        withContext(Dispatchers.IO) {
            try {
                // TODO: Implement actual UI settings saving
                Log.d(tag, "UI settings saved")
            } catch (e: Exception) {
                Log.e(tag, "Error saving UI settings", e)
            }
        }
    }

    override suspend fun saveMetricsSnapshot(snapshot: MetricsSnapshot) {
        withContext(Dispatchers.IO) {
            metricsHistory.add(snapshot)
            Log.d(tag, "Saved metrics snapshot: $snapshot")
        }
    }

    override suspend fun getMetricsSnapshots(): List<MetricsSnapshot> {
        return withContext(Dispatchers.IO) { metricsHistory.toList() }
    }
    
    override suspend fun exportSettings(): String {
        return withContext(Dispatchers.IO) {
            try {
                // TODO: Implement actual settings export to JSON
                "{}"
            } catch (e: Exception) {
                Log.e(tag, "Error exporting settings", e)
                "{}"
            }
        }
    }
    
    override suspend fun importSettings(json: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                // TODO: Implement actual settings import from JSON
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
                // TODO: Implement actual settings reset
                Log.d(tag, "Settings reset to defaults")
            } catch (e: Exception) {
                Log.e(tag, "Error resetting settings", e)
            }
        }
    }
} 