package com.nervesparks.iris.llm

import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import org.junit.Test

class ModelPerformanceTrackerTest {

    private fun createConfiguration(
        temperature: Float = 0.8f,
        topP: Float = 0.95f,
        topK: Int = 50,
        threadCount: Int = 6,
        gpuLayers: Int = 2,
        contextLength: Int = 4096,
        chatFormat: String = "LLAMA"
    ): ModelPerformanceTracker.ModelConfiguration {
        return ModelPerformanceTracker.ModelConfiguration(
            temperature = temperature,
            topP = topP,
            topK = topK,
            threadCount = threadCount,
            gpuLayers = gpuLayers,
            contextLength = contextLength,
            chatFormat = chatFormat
        )
    }

    private fun createDeviceInfo(
        model: String = "Pixel 8",
        androidVersion: String = "14",
        availableMemory: Long = 8_000,
        cpuCores: Int = 8,
        hasGpu: Boolean = true
    ): ModelPerformanceTracker.DeviceInfo {
        return ModelPerformanceTracker.DeviceInfo(
            deviceModel = model,
            androidVersion = androidVersion,
            availableMemory = availableMemory,
            cpuCores = cpuCores,
            hasGpu = hasGpu
        )
    }

    @Test
    fun `endSession records configuration and device info for new metrics`() {
        val tracker = ModelPerformanceTracker()
        val configuration = createConfiguration()
        val deviceInfo = createDeviceInfo()

        val sessionId = tracker.startSession(
            modelName = "model-A",
            modelPath = "/models/model-A.bin",
            configuration = configuration,
            deviceInfo = deviceInfo,
            backendUsed = "cpu"
        )

        tracker.recordInference(
            sessionId = sessionId,
            tokensGenerated = 120,
            inferenceTime = 600,
            memoryUsage = 2_048
        )

        tracker.endSession(sessionId)

        val metrics = tracker.performanceMetrics.value["model-A"]
        assertNotNull(metrics)
        assertEquals("/models/model-A.bin", metrics.modelPath)
        assertEquals(configuration, metrics.configuration)
        assertEquals(deviceInfo, metrics.deviceInfo)
        assertEquals("cpu", metrics.backendUsed)
        assertNull(tracker.currentSessionMetrics.value)
    }

    @Test
    fun `endSession updates metrics to reflect latest session details`() {
        val tracker = ModelPerformanceTracker()

        val initialConfig = createConfiguration(topK = 32, threadCount = 4, gpuLayers = 0)
        val initialDevice = createDeviceInfo(model = "Pixel 7", hasGpu = false)
        val secondConfig = createConfiguration(temperature = 0.6f, chatFormat = "CHATML")
        val secondDevice = createDeviceInfo(model = "Pixel 8 Pro", availableMemory = 12_000)

        val firstSession = tracker.startSession(
            modelName = "shared-model",
            modelPath = "/models/first.bin",
            configuration = initialConfig,
            deviceInfo = initialDevice,
            backendUsed = "cpu"
        )

        tracker.recordInference(
            sessionId = firstSession,
            tokensGenerated = 60,
            inferenceTime = 300,
            memoryUsage = 1_024
        )

        tracker.endSession(firstSession)

        val secondSession = tracker.startSession(
            modelName = "shared-model",
            modelPath = "/models/second.bin",
            configuration = secondConfig,
            deviceInfo = secondDevice,
            backendUsed = "gpu"
        )

        tracker.recordInference(
            sessionId = secondSession,
            tokensGenerated = 200,
            inferenceTime = 500,
            memoryUsage = 1_536
        )

        tracker.endSession(secondSession)

        val metrics = tracker.performanceMetrics.value["shared-model"]
        assertNotNull(metrics)
        assertEquals(2, metrics.totalSessions)
        assertEquals("/models/second.bin", metrics.modelPath)
        assertEquals(secondConfig, metrics.configuration)
        assertEquals(secondDevice, metrics.deviceInfo)
        assertEquals("gpu", metrics.backendUsed)
    }
}
