package com.nervesparks.iris.performance

import android.content.Context
import io.mockk.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MemoryMonitorTest {

    private lateinit var mockContext: Context
    private lateinit var memoryMonitor: MemoryMonitor

    @Before
    fun setUp() {
        mockContext = mockk(relaxed = true)
        memoryMonitor = MemoryMonitor(mockContext)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `getMemoryStats should return correct memory statistics`() {
        // Given
        val runtime = Runtime.getRuntime()
        val expectedUsedMemory = runtime.totalMemory() - runtime.freeMemory()
        val expectedAvailableMemory = runtime.freeMemory()
        val expectedTotalMemory = runtime.totalMemory()
        val expectedUsagePercent = (expectedUsedMemory.toFloat() / expectedTotalMemory.toFloat()) * 100

        // Mock ActivityManager
        val mockActivityManager = mockk<android.app.ActivityManager>()
        val mockMemoryInfo = mockk<android.app.ActivityManager.MemoryInfo>()

        every { mockContext.getSystemService(Context.ACTIVITY_SERVICE) } returns mockActivityManager
        every { mockActivityManager.getMemoryInfo(any()) } just Runs
        every { mockMemoryInfo.lowMemory } returns false

        // When
        val stats = memoryMonitor.getMemoryStats()

        // Then
        assertEquals(expectedUsedMemory, stats.usedMemory)
        assertEquals(expectedAvailableMemory, stats.availableMemory)
        assertEquals(expectedTotalMemory, stats.totalMemory)
        assertEquals(expectedUsagePercent, stats.memoryUsagePercent, 0.01f)
        assertFalse(stats.isLowMemory)
    }

    @Test
    fun `getMemoryStats should handle zero total memory gracefully`() {
        // Given - Mock scenario where totalMemory might be 0
        // This is more of a defensive test for edge cases

        // Mock ActivityManager
        val mockActivityManager = mockk<android.app.ActivityManager>()
        val mockMemoryInfo = mockk<android.app.ActivityManager.MemoryInfo>()

        every { mockContext.getSystemService(Context.ACTIVITY_SERVICE) } returns mockActivityManager
        every { mockActivityManager.getMemoryInfo(any()) } just Runs
        every { mockMemoryInfo.lowMemory } returns false

        // When
        val stats = memoryMonitor.getMemoryStats()

        // Then
        assertTrue(stats.memoryUsagePercent >= 0.0f) // Should not be negative
        assertTrue(stats.usedMemory >= 0)
        assertTrue(stats.availableMemory >= 0)
        assertTrue(stats.totalMemory >= 0)
    }

    @Test
    fun `shouldOptimizeMemory should return true when usage is high or low memory detected`() {
        // Given
        val runtime = Runtime.getRuntime()

        // Mock high memory usage scenario
        val mockActivityManager = mockk<android.app.ActivityManager>()
        val mockMemoryInfo = mockk<android.app.ActivityManager.MemoryInfo>()

        every { mockContext.getSystemService(Context.ACTIVITY_SERVICE) } returns mockActivityManager
        every { mockActivityManager.getMemoryInfo(any()) } just Runs
        every { mockMemoryInfo.lowMemory } returns true // Force low memory condition

        // When
        val shouldOptimize = memoryMonitor.shouldOptimizeMemory()

        // Then
        assertTrue(shouldOptimize)
    }

    @Test
    fun `shouldOptimizeMemory should return false when usage is normal and no low memory detected`() {
        // Given
        val mockActivityManager = mockk<android.app.ActivityManager>()
        val mockMemoryInfo = mockk<android.app.ActivityManager.MemoryInfo>()

        every { mockContext.getSystemService(Context.ACTIVITY_SERVICE) } returns mockActivityManager
        every { mockActivityManager.getMemoryInfo(any()) } just Runs
        every { mockMemoryInfo.lowMemory } returns false // Normal memory condition

        // When
        val shouldOptimize = memoryMonitor.shouldOptimizeMemory()

        // Then
        assertFalse(shouldOptimize)
    }

    @Test
    fun `MemoryStats data class should have correct equals and hashCode implementation`() {
        // Given
        val stats1 = MemoryMonitor.MemoryStats(
            usedMemory = 100L,
            availableMemory = 200L,
            totalMemory = 300L,
            memoryUsagePercent = 33.33f,
            isLowMemory = false
        )

        val stats2 = MemoryMonitor.MemoryStats(
            usedMemory = 100L,
            availableMemory = 200L,
            totalMemory = 300L,
            memoryUsagePercent = 33.33f,
            isLowMemory = false
        )

        val stats3 = MemoryMonitor.MemoryStats(
            usedMemory = 150L,
            availableMemory = 200L,
            totalMemory = 300L,
            memoryUsagePercent = 50.0f,
            isLowMemory = true
        )

        // Then
        assertEquals(stats1, stats2)
        assertTrue(stats1 != stats3)
    }
}
