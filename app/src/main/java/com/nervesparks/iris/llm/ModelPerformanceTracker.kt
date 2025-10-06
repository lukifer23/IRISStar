package com.nervesparks.iris.llm

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Advanced model performance tracking and comparison system.
 *
 * This service tracks detailed performance metrics for loaded models, enabling
 * users to compare performance across different models, configurations, and devices.
 */
@Singleton
class ModelPerformanceTracker @Inject constructor() {

    private val _performanceMetrics = MutableStateFlow<Map<String, ModelMetrics>>(emptyMap())
    val performanceMetrics: StateFlow<Map<String, ModelMetrics>> = _performanceMetrics.asStateFlow()

    private val _currentSessionMetrics = MutableStateFlow<ModelSessionMetrics?>(null)
    val currentSessionMetrics: StateFlow<ModelSessionMetrics?> = _currentSessionMetrics.asStateFlow()

    /**
     * Performance metrics for a specific model
     */
    data class ModelMetrics(
        val modelName: String,
        val modelPath: String,
        val totalSessions: Int,
        val averageLoadTime: Long,
        val averageInferenceTime: Long,
        val averageTokensPerSecond: Double,
        val averageMemoryUsage: Long,
        val bestTokensPerSecond: Double,
        val worstTokensPerSecond: Double,
        val totalTokensGenerated: Long,
        val lastUsed: Long,
        val backendUsed: String,
        val configuration: ModelConfiguration,
        val deviceInfo: DeviceInfo
    ) {

        /**
         * Calculate performance score (0-100) based on multiple factors
         */
        fun calculatePerformanceScore(): Double {
            val tpsScore = (averageTokensPerSecond / 100.0).coerceAtMost(1.0) * 40
            val memoryScore = (1.0 - (averageMemoryUsage / 1000.0).coerceAtMost(1.0)) * 30
            val loadTimeScore = (1.0 - (averageLoadTime / 10000.0).coerceAtMost(1.0)) * 20
            val consistencyScore = (1.0 - ((bestTokensPerSecond - worstTokensPerSecond) / bestTokensPerSecond).coerceAtMost(1.0)) * 10

            return (tpsScore + memoryScore + loadTimeScore + consistencyScore).coerceIn(0.0, 100.0)
        }

        /**
         * Get recommendation based on performance
         */
        fun getRecommendation(): String {
            val score = calculatePerformanceScore()
            return when {
                score >= 80 -> "Excellent performance"
                score >= 60 -> "Good performance"
                score >= 40 -> "Average performance"
                else -> "Poor performance - consider different model"
            }
        }
    }

    /**
     * Current session performance metrics
     */
    data class ModelSessionMetrics(
        val sessionId: String,
        val modelName: String,
        val modelPath: String,
        val startTime: Long,
        val tokensGenerated: Int,
        val inferenceCalls: Int,
        val totalInferenceTime: Long,
        val memoryUsage: Long,
        val backendUsed: String,
        val configuration: ModelConfiguration,
        val deviceInfo: DeviceInfo
    ) {

        /**
         * Calculate current session performance
         */
        fun getCurrentTokensPerSecond(): Double {
            return if (totalInferenceTime > 0) {
                (tokensGenerated.toDouble() / totalInferenceTime.toDouble()) * 1000.0
            } else {
                0.0
            }
        }

        /**
         * Calculate average inference time per token
         */
        fun getAverageInferenceTimePerToken(): Double {
            return if (tokensGenerated > 0) {
                totalInferenceTime.toDouble() / tokensGenerated.toDouble()
            } else {
                0.0
            }
        }
    }

    /**
     * Model configuration snapshot
     */
    data class ModelConfiguration(
        val temperature: Float,
        val topP: Float,
        val topK: Int,
        val threadCount: Int,
        val gpuLayers: Int,
        val contextLength: Int,
        val chatFormat: String
    )

    /**
     * Device information
     */
    data class DeviceInfo(
        val deviceModel: String,
        val androidVersion: String,
        val availableMemory: Long,
        val cpuCores: Int,
        val hasGpu: Boolean
    )

    /**
     * Start tracking a new model session
     */
    fun startSession(
        modelName: String,
        modelPath: String,
        configuration: ModelConfiguration,
        deviceInfo: DeviceInfo,
        backendUsed: String
    ): String {
        val sessionId = generateSessionId()
        val sessionMetrics = ModelSessionMetrics(
            sessionId = sessionId,
            modelName = modelName,
            modelPath = modelPath,
            startTime = System.currentTimeMillis(),
            tokensGenerated = 0,
            inferenceCalls = 0,
            totalInferenceTime = 0,
            memoryUsage = 0,
            backendUsed = backendUsed,
            configuration = configuration,
            deviceInfo = deviceInfo
        )

        _currentSessionMetrics.value = sessionMetrics

        Timber.tag("ModelPerformanceTracker").d(
            "Started performance tracking session: $sessionId for model: $modelName"
        )

        return sessionId
    }

    /**
     * Record an inference call
     */
    fun recordInference(
        sessionId: String,
        tokensGenerated: Int,
        inferenceTime: Long,
        memoryUsage: Long
    ) {
        _currentSessionMetrics.value?.let { current ->
            if (current.sessionId == sessionId) {
                val updatedMetrics = current.copy(
                    tokensGenerated = current.tokensGenerated + tokensGenerated,
                    inferenceCalls = current.inferenceCalls + 1,
                    totalInferenceTime = current.totalInferenceTime + inferenceTime,
                    memoryUsage = memoryUsage
                )

                _currentSessionMetrics.value = updatedMetrics
            }
        }
    }

    /**
     * End the current session and update model metrics
     */
    fun endSession(sessionId: String) {
        _currentSessionMetrics.value?.let { current ->
            if (current.sessionId == sessionId) {
                val sessionDuration = System.currentTimeMillis() - current.startTime

                // Update or create model metrics
                val currentMetrics = _performanceMetrics.value.toMutableMap()
                val modelKey = current.modelName

                val existingMetrics = currentMetrics[modelKey]
                val updatedMetrics = if (existingMetrics != null) {
                    // Update existing metrics
                    existingMetrics.copy(
                        modelPath = current.modelPath,
                        totalSessions = existingMetrics.totalSessions + 1,
                        averageLoadTime = (existingMetrics.averageLoadTime + sessionDuration) / 2,
                        averageInferenceTime = calculateAverageInferenceTime(existingMetrics, current),
                        averageTokensPerSecond = calculateAverageTokensPerSecond(existingMetrics, current),
                        averageMemoryUsage = (existingMetrics.averageMemoryUsage + current.memoryUsage) / 2,
                        bestTokensPerSecond = maxOf(existingMetrics.bestTokensPerSecond, current.getCurrentTokensPerSecond()),
                        worstTokensPerSecond = minOf(existingMetrics.worstTokensPerSecond, current.getCurrentTokensPerSecond()),
                        totalTokensGenerated = existingMetrics.totalTokensGenerated + current.tokensGenerated,
                        lastUsed = System.currentTimeMillis(),
                        backendUsed = current.backendUsed,
                        configuration = current.configuration,
                        deviceInfo = current.deviceInfo
                    )
                } else {
                    // Create new metrics
                    ModelMetrics(
                        modelName = current.modelName,
                        modelPath = current.modelPath,
                        totalSessions = 1,
                        averageLoadTime = sessionDuration,
                        averageInferenceTime = current.getAverageInferenceTimePerToken(),
                        averageTokensPerSecond = current.getCurrentTokensPerSecond(),
                        averageMemoryUsage = current.memoryUsage,
                        bestTokensPerSecond = current.getCurrentTokensPerSecond(),
                        worstTokensPerSecond = current.getCurrentTokensPerSecond(),
                        totalTokensGenerated = current.tokensGenerated,
                        lastUsed = System.currentTimeMillis(),
                        backendUsed = current.backendUsed,
                        configuration = current.configuration,
                        deviceInfo = current.deviceInfo
                    )
                }

                currentMetrics[modelKey] = updatedMetrics
                _performanceMetrics.value = currentMetrics

                // Clear current session
                _currentSessionMetrics.value = null

                Timber.tag("ModelPerformanceTracker").d(
                    "Ended performance tracking session: $sessionId. " +
                    "Generated ${current.tokensGenerated} tokens at ${current.getCurrentTokensPerSecond()} TPS"
                )
            }
        }
    }

    /**
     * Get performance comparison between models
     */
    fun getPerformanceComparison(): List<ModelComparison> {
        val metrics = _performanceMetrics.value.values.toList()

        return metrics.map { modelMetrics ->
            ModelComparison(
                modelName = modelMetrics.modelName,
                performanceScore = modelMetrics.calculatePerformanceScore(),
                recommendation = modelMetrics.getRecommendation(),
                averageTokensPerSecond = modelMetrics.averageTokensPerSecond,
                averageMemoryUsage = modelMetrics.averageMemoryUsage,
                totalSessions = modelMetrics.totalSessions,
                lastUsed = modelMetrics.lastUsed
            )
        }.sortedByDescending { it.performanceScore }
    }

    /**
     * Get the best performing model
     */
    fun getBestPerformingModel(): ModelMetrics? {
        return _performanceMetrics.value.values.maxByOrNull { it.calculatePerformanceScore() }
    }

    /**
     * Clear all performance data
     */
    fun clearAllData() {
        _performanceMetrics.value = emptyMap()
        _currentSessionMetrics.value = null
        Timber.tag("ModelPerformanceTracker").d("Cleared all performance tracking data")
    }

    private fun calculateAverageInferenceTime(existing: ModelMetrics, current: ModelSessionMetrics): Long {
        val existingTotal = existing.averageInferenceTime * existing.totalSessions
        val currentTotal = current.getAverageInferenceTimePerToken()
        return ((existingTotal + currentTotal) / (existing.totalSessions + 1)).toLong()
    }

    private fun calculateAverageTokensPerSecond(existing: ModelMetrics, current: ModelSessionMetrics): Double {
        val existingTotal = existing.averageTokensPerSecond * existing.totalSessions
        val currentTotal = current.getCurrentTokensPerSecond()
        return (existingTotal + currentTotal) / (existing.totalSessions + 1)
    }

    private fun generateSessionId(): String {
        return "session_${System.currentTimeMillis()}_${(Math.random() * 1000).toInt()}"
    }
}

/**
 * Model performance comparison data
 */
data class ModelComparison(
    val modelName: String,
    val performanceScore: Double,
    val recommendation: String,
    val averageTokensPerSecond: Double,
    val averageMemoryUsage: Long,
    val totalSessions: Int,
    val lastUsed: Long
)
