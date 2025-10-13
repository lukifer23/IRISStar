package com.nervesparks.iris.performance

import android.content.Context
import android.os.Build
import android.os.Debug
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nervesparks.iris.data.UserPreferencesRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.lang.ref.WeakReference
import timber.log.Timber

/**
 * PHASE 3.4: Performance Optimization Utilities
 * Memory management, caching, and performance monitoring
 */

// Memory usage monitor with caching for performance
class MemoryMonitor(private val context: Context) {

    data class MemoryStats(
        val usedMemory: Long,
        val availableMemory: Long,
        val totalMemory: Long,
        val memoryUsagePercent: Float,
        val isLowMemory: Boolean
    )

    private var cachedStats: MemoryStats? = null
    private var lastUpdateTime: Long = 0
    private val cacheValidityDuration = 500L // 500ms cache

    fun getMemoryStats(): MemoryStats {
        val currentTime = System.currentTimeMillis()

        // Return cached stats if they're still valid
        cachedStats?.let { cached ->
            if (currentTime - lastUpdateTime < cacheValidityDuration) {
                return cached
            }
        }

        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        val memoryInfo = android.app.ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)

        val totalMemory = memoryInfo.totalMem
        val availableMemory = memoryInfo.availMem
        val baseUsedMemory = (totalMemory - availableMemory).coerceAtLeast(0L)
        val nativeHeapUsage = Debug.getNativeHeapAllocatedSize().coerceAtLeast(0L)
        val usedMemory = (baseUsedMemory + nativeHeapUsage).coerceAtMost(totalMemory)

        val memoryUsagePercent = if (totalMemory > 0) {
            (usedMemory.toFloat() / totalMemory.toFloat()) * 100
        } else {
            0f
        }

        // Check if device is in low memory state (cached for performance)
        val isLowMemory = if (currentTime - lastUpdateTime < 2000L) {
            cachedStats?.isLowMemory ?: memoryInfo.lowMemory
        } else {
            memoryInfo.lowMemory
        }

        val stats = MemoryStats(
            usedMemory = usedMemory,
            availableMemory = availableMemory,
            totalMemory = totalMemory,
            memoryUsagePercent = memoryUsagePercent,
            isLowMemory = isLowMemory
        )

        // Cache the results
        cachedStats = stats
        lastUpdateTime = currentTime

        return stats
    }

    fun shouldOptimizeMemory(): Boolean {
        val stats = getMemoryStats()
        return stats.memoryUsagePercent > 75f || stats.isLowMemory
    }
}

// Smart memory manager with automatic cleanup
class SmartMemoryManager(private val context: Context) {

    private val weakReferences = mutableListOf<WeakReference<Any>>()
    private val coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    fun registerForCleanup(obj: Any) {
        weakReferences.add(WeakReference(obj))
    }

    fun triggerCleanup() {
        coroutineScope.launch {
            // Clean up weak references
            weakReferences.removeAll { it.get() == null }

            // Force garbage collection if memory is low
            val memoryMonitor = MemoryMonitor(context)
            if (memoryMonitor.shouldOptimizeMemory()) {
                System.gc()
                delay(100) // Allow GC to complete
                System.runFinalization()
            }
        }
    }

    fun cleanup() {
        coroutineScope.cancel()
        weakReferences.clear()
    }
}

// Performance monitoring utilities
fun logPerformanceWarning(context: Context) {
    val memoryMonitor = MemoryMonitor(context)
    val stats = memoryMonitor.getMemoryStats()
    if (stats.memoryUsagePercent > 80f) {
        Timber.tag("PerformanceMonitor").w("High memory usage: ${stats.memoryUsagePercent}%")
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
        val totalMemory = runtime.totalMemory()
        val usagePercent = (usedMemory.toFloat() / totalMemory.toFloat()) * 100

        return if (usagePercent > 80f) {
            // Trigger cleanup
            cacheManager.clearExpiredCache()
            System.gc()
            true
        } else {
            false
        }
    }
}
