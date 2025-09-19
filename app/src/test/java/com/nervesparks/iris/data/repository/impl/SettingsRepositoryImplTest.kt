package com.nervesparks.iris.data.repository.impl

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.nervesparks.iris.data.FakeUserPreferencesRepository
import com.nervesparks.iris.data.repository.PerformanceSettings
import com.nervesparks.iris.data.repository.ThinkingTokenSettings
import com.nervesparks.iris.data.repository.UISettings
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SettingsRepositoryImplTest {

    private lateinit var context: Context
    private lateinit var userPrefs: FakeUserPreferencesRepository
    private lateinit var repository: SettingsRepositoryImpl

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        userPrefs = FakeUserPreferencesRepository(context)
        runTest { userPrefs.clearAll() }
        repository = SettingsRepositoryImpl(userPrefs)
    }

    @After
    fun tearDown() {
        runTest { userPrefs.clearAll() }
    }

    @Test
    fun performanceSettingsRoundTrip() = runTest {
        val settings = PerformanceSettings(
            threadCount = 6,
            maxContextLength = 2048,
            enableMemoryOptimization = false,
            enableBackgroundProcessing = false
        )
        repository.savePerformanceSettings(settings)
        val loaded = repository.getPerformanceSettings()
        assertEquals(settings, loaded)
    }

    @Test
    fun uiSettingsRoundTrip() = runTest {
        val settings = UISettings(
            theme = "LIGHT",
            fontSize = 1.5f,
            enableAnimations = false,
            enableHapticFeedback = false
        )
        repository.saveUISettings(settings)
        val loaded = repository.getUISettings()
        assertEquals(settings, loaded)
    }

    @Test
    fun exportImportRoundTrip() = runTest {
        val defaultModelName = "modelX"
        val thinking = ThinkingTokenSettings(false, "ALWAYS_VISIBLE")
        val perf = PerformanceSettings(
            threadCount = 6,
            maxContextLength = 2048,
            enableMemoryOptimization = false,
            enableBackgroundProcessing = false
        )
        val ui = UISettings(
            theme = "LIGHT",
            fontSize = 1.5f,
            enableAnimations = false,
            enableHapticFeedback = false
        )

        repository.setDefaultModelName(defaultModelName)
        repository.saveThinkingTokenSettings(thinking)
        repository.savePerformanceSettings(perf)
        repository.saveUISettings(ui)

        val json = repository.exportSettings()
        repository.resetToDefaults()
        repository.importSettings(json)

        assertEquals(defaultModelName, repository.getDefaultModelName())
        assertEquals(thinking, repository.getThinkingTokenSettings())
        assertEquals(perf, repository.getPerformanceSettings())
        assertEquals(ui, repository.getUISettings())
    }

    @Test
    fun resetToDefaultsClearsPreferences() = runTest {
        repository.savePerformanceSettings(
            PerformanceSettings(
                threadCount = 6,
                maxContextLength = 2048,
                enableMemoryOptimization = false,
                enableBackgroundProcessing = false
            )
        )
        repository.saveUISettings(
            UISettings(
                theme = "LIGHT",
                fontSize = 1.5f,
                enableAnimations = false,
                enableHapticFeedback = false
            )
        )
        repository.resetToDefaults()

        val perf = repository.getPerformanceSettings()
        val ui = repository.getUISettings()

        assertEquals(
            PerformanceSettings(
                threadCount = 4,
                maxContextLength = 4096,
                enableMemoryOptimization = true,
                enableBackgroundProcessing = true
            ),
            perf
        )
        assertEquals(
            UISettings(
                theme = "DARK",
                fontSize = 1.0f,
                enableAnimations = true,
                enableHapticFeedback = true
            ),
            ui
        )
    }
}
