package com.nervesparks.iris.viewmodel

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import android.llama.cpp.LLamaAndroid
import com.nervesparks.iris.data.UserPreferencesRepository
import com.nervesparks.iris.data.repository.ModelRepository

@RunWith(AndroidJUnit4::class)
class ModelLoadingInstrumentedTest {

    @Test
    fun loadAndUnloadModelUpdatesState() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val llama = mockk<LLamaAndroid>(relaxed = true)
        val userPrefs = mockk<UserPreferencesRepository>(relaxed = true)
        val modelRepo = mockk<ModelRepository>(relaxed = true)
        val viewModel = ModelViewModel(llama, userPrefs, modelRepo)

        val dir = context.cacheDir
        val file = File(dir, "test.gguf")
        file.writeText("model")

        instrumentation.runOnMainSync {
            viewModel.loadModelByName(file.name, dir)
        }
        instrumentation.waitForIdleSync()

        assertTrue(viewModel.isModelLoaded)
        assertEquals(file.name, viewModel.currentModelName)
        verify { llama.load(file.absolutePath, any(), any(), any(), any(), any()) }

        instrumentation.runOnMainSync { viewModel.unloadModel() }
        instrumentation.waitForIdleSync()

        assertFalse(viewModel.isModelLoaded)
        assertEquals("", viewModel.currentModelName)
        verify { llama.unload() }
    }
}

