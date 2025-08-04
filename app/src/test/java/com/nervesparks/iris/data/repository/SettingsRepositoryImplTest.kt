package com.nervesparks.iris.data.repository

import com.nervesparks.iris.data.UserPreferencesRepository
import com.nervesparks.iris.data.repository.impl.SettingsRepositoryImpl
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsRepositoryImplTest {
    @Test
    fun getDefaultModelName_success() = runBlocking {
        val prefs = mockk<UserPreferencesRepository>()
        every { prefs.getDefaultModelName() } returns "ModelA"
        val repo = SettingsRepositoryImpl(prefs)
        assertEquals("ModelA", repo.getDefaultModelName())
    }

    @Test
    fun getDefaultModelName_failureReturnsEmpty() = runBlocking {
        val prefs = mockk<UserPreferencesRepository>()
        every { prefs.getDefaultModelName() } throws RuntimeException("error")
        val repo = SettingsRepositoryImpl(prefs)
        assertEquals("", repo.getDefaultModelName())
    }

    @Test
    fun setDefaultModelName_success() = runBlocking {
        val prefs = mockk<UserPreferencesRepository>()
        justRun { prefs.setDefaultModelName(any()) }
        val repo = SettingsRepositoryImpl(prefs)
        repo.setDefaultModelName("B")
    }
}
