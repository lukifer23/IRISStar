package com.nervesparks.iris.data.repository

/**
 * Repository interface for settings and configuration management
 */
interface SettingsRepository {
    
    /**
     * Get default model name
     */
    suspend fun getDefaultModelName(): String
    
    /**
     * Set default model name
     */
    suspend fun setDefaultModelName(modelName: String)
    
    /**
     * Get thinking token settings
     */
    suspend fun getThinkingTokenSettings(): ThinkingTokenSettings
    
    /**
     * Save thinking token settings
     */
    suspend fun saveThinkingTokenSettings(settings: ThinkingTokenSettings)
    
    /**
     * Get performance settings
     */
    suspend fun getPerformanceSettings(): PerformanceSettings
    
    /**
     * Save performance settings
     */
    suspend fun savePerformanceSettings(settings: PerformanceSettings)
    
    /**
     * Get UI settings
     */
    suspend fun getUISettings(): UISettings
    
    /**
     * Save UI settings
     */
    suspend fun saveUISettings(settings: UISettings)
    
    /**
     * Export all settings to JSON
     */
    suspend fun exportSettings(): String
    
    /**
     * Import settings from JSON
     */
    suspend fun importSettings(json: String): Result<Unit>
    
    /**
     * Reset all settings to defaults
     */
    suspend fun resetToDefaults()
}

/**
 * Data class for thinking token settings
 */
data class ThinkingTokenSettings(
    val showThinkingTokens: Boolean = true,
    val thinkingTokenStyle: String = "COLLAPSIBLE" // COLLAPSIBLE, ALWAYS_VISIBLE, HIDDEN
)

/**
 * Data class for performance settings
 */
data class PerformanceSettings(
    val threadCount: Int = 2,
    val maxContextLength: Int = 4096,
    val enableMemoryOptimization: Boolean = true,
    val enableBackgroundProcessing: Boolean = true
)

/**
 * Data class for UI settings
 */
data class UISettings(
    val theme: String = "DARK", // DARK, LIGHT, AUTO
    val fontSize: Float = 1.0f,
    val enableAnimations: Boolean = true,
    val enableHapticFeedback: Boolean = true
) 