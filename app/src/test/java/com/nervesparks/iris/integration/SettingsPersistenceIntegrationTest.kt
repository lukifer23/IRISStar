package com.nervesparks.iris.integration

import androidx.test.core.app.ApplicationProvider
import com.nervesparks.iris.data.UserPreferencesRepository
import com.nervesparks.iris.data.repository.impl.SettingsRepositoryImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class SettingsPersistenceIntegrationTest {
    private lateinit var repo: SettingsRepositoryImpl

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val prefs = UserPreferencesRepository.getInstance(context)
        repo = SettingsRepositoryImpl(prefs)
    }

    @Test
    fun defaultModelName_persists() = runBlocking {
        repo.setDefaultModelName("ModelX")
        val name = repo.getDefaultModelName()
        assertEquals("ModelX", name)
    }
}
