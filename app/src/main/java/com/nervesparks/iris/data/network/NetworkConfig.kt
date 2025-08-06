package com.nervesparks.iris.data.network

/**
 * Configuration values for network caching behaviour.
 * @param cacheSizeBytes size of the on-disk HTTP cache.
 * @param maxAgeSeconds max age for cached responses before they are considered stale.
 * @param maxStaleSeconds how long to keep stale responses before eviction.
 */
data class NetworkConfig(
    val cacheSizeBytes: Long = 10L * 1024 * 1024, // 10MB
    val maxAgeSeconds: Int = 60,
    val maxStaleSeconds: Int = 60 * 60 * 24 // 1 day
)
