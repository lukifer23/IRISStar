package com.nervesparks.iris.viewmodel

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.nervesparks.iris.data.FakeUserPreferencesRepository
import com.nervesparks.iris.data.repository.PerformanceSettings
import com.nervesparks.iris.data.repository.SettingsRepository
import com.nervesparks.iris.data.repository.ThinkingTokenSettings
import com.nervesparks.iris.data.repository.UISettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun updatePerformanceSettingsSavesToRepository() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val repository = FakeSettingsRepository().apply {
                performanceSettings = PerformanceSettings(
                    threadCount = 8,
                    maxContextLength = 8192,
                    enableMemoryOptimization = true,
                    enableBackgroundProcessing = true
                )
            }

            val viewModel = createViewModel(repository)
            advanceUntilIdle()

            viewModel.updatePerformanceSettings(enableMemoryOptimization = false, enableBackgroundProcessing = false)
            advanceUntilIdle()

            assertFalse(viewModel.perfEnableMemoryOptimization)
            assertFalse(viewModel.perfEnableBackgroundProcessing)
            assertEquals(1, repository.savedPerformanceSettings.size)
            assertEquals(
                PerformanceSettings(
                    threadCount = 8,
                    maxContextLength = 8192,
                    enableMemoryOptimization = false,
                    enableBackgroundProcessing = false
                ),
                repository.savedPerformanceSettings.single()
            )
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun updateUISettingsSavesToRepository() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val repository = FakeSettingsRepository()
            val viewModel = createViewModel(repository)
            advanceUntilIdle()

            viewModel.updateUISettings(
                theme = "LIGHT",
                fontSize = 1.5f,
                enableAnimations = false,
                enableHapticFeedback = false
            )
            advanceUntilIdle()

            assertEquals("LIGHT", viewModel.uiTheme)
            assertEquals(1.5f, viewModel.uiFontSize, 0.0f)
            assertFalse(viewModel.uiEnableAnimations)
            assertFalse(viewModel.uiEnableHapticFeedback)
            assertEquals(1, repository.savedUISettings.size)
            assertEquals(
                UISettings(
                    theme = "LIGHT",
                    fontSize = 1.5f,
                    enableAnimations = false,
                    enableHapticFeedback = false
                ),
                repository.savedUISettings.single()
            )
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun updateThinkingTokenSettingsSavesToRepository() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val repository = FakeSettingsRepository()
            val viewModel = createViewModel(repository)
            advanceUntilIdle()

            viewModel.updateThinkingTokenSettings(showThinking = false, style = "ALWAYS_VISIBLE")
            advanceUntilIdle()

            assertFalse(viewModel.showThinkingTokens)
            assertEquals("ALWAYS_VISIBLE", viewModel.thinkingTokenStyle)
            assertEquals(1, repository.savedThinkingTokenSettings.size)
            assertEquals(
                ThinkingTokenSettings(
                    showThinkingTokens = false,
                    thinkingTokenStyle = "ALWAYS_VISIBLE"
                ),
                repository.savedThinkingTokenSettings.single()
            )
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun savedSettingsAreRestoredOnReload() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val repository = FakeSettingsRepository()

            val initialViewModel = SettingsViewModel(newUserPreferences(), repository)
            advanceUntilIdle()

            initialViewModel.updatePerformanceSettings(enableMemoryOptimization = false, enableBackgroundProcessing = false)
            initialViewModel.updateUISettings(
                theme = "LIGHT",
                fontSize = 1.25f,
                enableAnimations = false,
                enableHapticFeedback = false
            )
            initialViewModel.updateThinkingTokenSettings(showThinking = false, style = "ALWAYS_VISIBLE")
            advanceUntilIdle()

            val reloadedViewModel = SettingsViewModel(newUserPreferences(), repository)
            advanceUntilIdle()

            assertFalse(reloadedViewModel.perfEnableMemoryOptimization)
            assertFalse(reloadedViewModel.perfEnableBackgroundProcessing)
            assertEquals("LIGHT", reloadedViewModel.uiTheme)
            assertEquals(1.25f, reloadedViewModel.uiFontSize, 0.0f)
            assertFalse(reloadedViewModel.uiEnableAnimations)
            assertFalse(reloadedViewModel.uiEnableHapticFeedback)
            assertFalse(reloadedViewModel.showThinkingTokens)
            assertEquals("ALWAYS_VISIBLE", reloadedViewModel.thinkingTokenStyle)

            assertEquals(1, repository.savedPerformanceSettings.size)
            assertEquals(1, repository.savedUISettings.size)
            assertEquals(1, repository.savedThinkingTokenSettings.size)
        } finally {
            Dispatchers.resetMain()
        }
    }

    private fun createViewModel(repository: FakeSettingsRepository): SettingsViewModel {
        return SettingsViewModel(newUserPreferences(), repository)
    }

    private fun newUserPreferences(): FakeUserPreferencesRepository {
        return FakeUserPreferencesRepository(context).apply {
            runBlocking { clearAll() }
        }
    }

    private class FakeSettingsRepository : SettingsRepository {
        var performanceSettings: PerformanceSettings = PerformanceSettings()
        var uiSettings: UISettings = UISettings()
        var thinkingTokenSettings: ThinkingTokenSettings = ThinkingTokenSettings()

        val savedPerformanceSettings = mutableListOf<PerformanceSettings>()
        val savedUISettings = mutableListOf<UISettings>()
        val savedThinkingTokenSettings = mutableListOf<ThinkingTokenSettings>()

        override suspend fun getDefaultModelName(): String = ""

        override suspend fun setDefaultModelName(modelName: String) = Unit

        override suspend fun getThinkingTokenSettings(): ThinkingTokenSettings {
            return thinkingTokenSettings
        }

        override suspend fun saveThinkingTokenSettings(settings: ThinkingTokenSettings) {
            thinkingTokenSettings = settings
            savedThinkingTokenSettings += settings
        }

        override suspend fun getPerformanceSettings(): PerformanceSettings {
            return performanceSettings
        }

        override suspend fun savePerformanceSettings(settings: PerformanceSettings) {
            performanceSettings = settings
            savedPerformanceSettings += settings
        }

        override suspend fun getUISettings(): UISettings {
            return uiSettings
        }

        override suspend fun saveUISettings(settings: UISettings) {
            uiSettings = settings
            savedUISettings += settings
        }

        override suspend fun exportSettings(): String = "{}"

        override suspend fun importSettings(json: String): Result<Unit> = Result.success(Unit)

        override suspend fun resetToDefaults() = Unit
    }
}
