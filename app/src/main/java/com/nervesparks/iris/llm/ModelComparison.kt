package com.nervesparks.iris.llm

/**
 * Data class representing a model performance comparison
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
