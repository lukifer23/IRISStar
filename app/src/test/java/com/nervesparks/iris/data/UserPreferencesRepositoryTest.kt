package com.nervesparks.iris.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.nervesparks.iris.Template
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class UserPreferencesRepositoryTest {

    private lateinit var context: Context
    private lateinit var repository: UserPreferencesRepository

    @Before
    fun setup() = runTest {
        context = ApplicationProvider.getApplicationContext()
        repository = UserPreferencesRepository.getInstance(context)
        repository.clearAll()
    }

    @After
    fun tearDown() = runTest {
        repository.clearAll()
    }

    @Test
    fun defaultModelNameFlowEmitsUpdates() = runTest {
        val emissions = mutableListOf<String>()
        val job = launch {
            repository.defaultModelNameFlow.take(2).toList(emissions)
        }
        repository.setDefaultModelName("model-alpha")
        job.join()
        assertEquals(listOf("", "model-alpha"), emissions)
    }

    @Test
    fun setModelTemperaturePersistsValue() = runTest {
        repository.setModelTemperature(1.1f)
        assertEquals(1.1f, repository.getModelTemperature(), 0.0001f)
    }

    @Test
    fun saveTemplatesRoundTrip() = runTest {
        val templates = listOf(
            Template(id = 1L, name = "Example", content = "Sample content")
        )
        repository.saveTemplates(templates)
        assertEquals(templates, repository.getTemplates())
    }
}
