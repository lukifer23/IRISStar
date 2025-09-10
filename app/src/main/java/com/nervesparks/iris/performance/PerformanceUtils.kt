package com.nervesparks.iris.performance

import android.content.Context
import android.os.Build
import android.os.Debug
import android.util.LruCache
import android.content.ComponentCallbacks2
import android.content.res.Configuration
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nervesparks.iris.data.UserPreferencesRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/**
 * PHASE 3.4: Performance Optimization Utilities
 * Memory management, caching, and performance monitoring
 */

// Memory usage monitor
class MemoryMonitor(private val context: Context) {

    data class MemoryStats(
        val usedMemory: Long,
        val availableMemory: Long,
        val totalMemory: Long,
        val memoryUsagePercent: Float,
        val isLowMemory: Boolean
    )

    fun getMemoryStats(): MemoryStats {
        val runtime = Runtime.getRuntime()
        val usedMemory = runtime.totalMemory() - runtime.freeMemory()
        val availableMemory = runtime.freeMemory()
        val totalMemory = runtime.totalMemory()
        val memoryUsagePercent = (usedMemory.toFloat() / totalMemory.toFloat()) * 100

        // Check if device is in low memory state
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        val memoryInfo = android.app.ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        val isLowMemory = memoryInfo.lowMemory

        return MemoryStats(
            usedMemory = usedMemory,
            availableMemory = availableMemory,
            totalMemory = totalMemory,
            memoryUsagePercent = memoryUsagePercent,
            isLowMemory = isLowMemory
        )
    }

    fun shouldOptimizeMemory(): Boolean {
        val stats = getMemoryStats()
        return stats.memoryUsagePercent > 75f || stats.isLowMemory
    }
}

// Smart memory manager backed by an LruCache
class SmartMemoryManager<K, V>(
    maxSize: Int = (Runtime.getRuntime().maxMemory() / 1024L).toInt() / 8
) : ComponentCallbacks2 {

    private val cache = object : LruCache<K, V>(maxSize) {}

    fun put(key: K, value: V) {
        cache.put(key, value)
    }

    fun get(key: K): V? = cache.get(key)

    fun size(): Int = cache.size()

    fun clear() {
        cache.evictAll()
    }

    override fun onTrimMemory(level: Int) {
        if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) {
            clear()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        // no-op
    }

    override fun onLowMemory() {
        clear()
    }
}

// Performance monitoring utilities
fun logPerformanceWarning(context: Context) {
    val memoryMonitor = MemoryMonitor(context)
    val stats = memoryMonitor.getMemoryStats()
    if (stats.memoryUsagePercent > 80f) {
        android.util.Log.w("PerformanceMonitor", "High memory usage: ${stats.memoryUsagePercent}%")
    }
}

// Cache management utilities
class CacheManager(private val context: Context) {

    private val memoryCache = mutableMapOf<String, Any>()
    private val cacheExpiry = mutableMapOf<String, Long>()

    fun <T> getOrPut(key: String, expiryMillis: Long = 5 * 60 * 1000, factory: () -> T): T {
        val currentTime = System.currentTimeMillis()

        // Check if cached item exists and is not expired
        val cachedExpiry = cacheExpiry[key]
        if (cachedExpiry != null && currentTime < cachedExpiry) {
            @Suppress("UNCHECKED_CAST")
            return memoryCache[key] as T
        }

        // Create new item and cache it
        val item = factory()
        memoryCache[key] = item as Any
        cacheExpiry[key] = currentTime + expiryMillis

        return item
    }

    fun clearExpiredCache() {
        val currentTime = System.currentTimeMillis()
        val expiredKeys = cacheExpiry.filter { currentTime >= it.value }.keys

        expiredKeys.forEach { key ->
            memoryCache.remove(key)
            cacheExpiry.remove(key)
        }
    }

    fun clearAllCache() {
        memoryCache.clear()
        cacheExpiry.clear()
    }

    fun getCacheSize(): Int = memoryCache.size
}

// Performance-optimized ViewModel base class
abstract class OptimizedViewModel(private val context: Context) : ViewModel() {

    protected val cacheManager = CacheManager(context)

    // Automatic cleanup on clear
    override fun onCleared() {
        super.onCleared()
        cacheManager.clearAllCache()
    }

    // Memory optimization utilities
    protected fun <T> cached(key: String, factory: () -> T): T {
        return cacheManager.getOrPut(key, factory = factory) as T
    }

    protected fun optimizeForLowMemory(): Boolean {
        val runtime = Runtime.getRuntime()
        val usedMemory = runtime.totalMemory() - runtime.freeMemory()
        val maxMemory = runtime.maxMemory()
        val usagePercent = (usedMemory.toFloat() / maxMemory.toFloat()) * 100

        return if (usagePercent > 80f) {
            // Trigger cleanup
            cacheManager.clearExpiredCache()
            true
        } else {
            false
        }
    }
}
