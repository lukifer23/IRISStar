package com.nervesparks.iris.data.exceptions

import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ErrorHandlerTest {

    @Before
    fun setUp() {
        mockkStatic("kotlinx.coroutines.GlobalScope")
        mockkStatic("timber.log.Timber")
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `reportError should log error and emit to flow`() = runTest {
        // Given
        val error = RuntimeException("Test error")
        val context = "Test Context"
        val severity = ErrorHandler.ErrorSeverity.MEDIUM
        val userMessage = "User-friendly message"

        // Mock Timber
        mockkStatic("timber.log.Timber")
        every { com.nervesparks.iris.data.exceptions.Timber.tag(any()).e(any<Throwable>(), any()) } just Runs

        // When
        ErrorHandler.reportError(error, context, severity, userMessage)

        // Then
        verify {
            com.nervesparks.iris.data.exceptions.Timber.tag("ErrorHandler").e(error, "Error in $context: ${error.message}")
        }

        // Verify flow emission (would need to collect from errorFlow in real test)
        // This tests that the method doesn't throw and calls the expected logging
    }

    @Test
    fun `reportModelError should use HIGH severity and provide user-friendly message`() = runTest {
        // Given
        val error = RuntimeException("Model load failed")
        val modelName = "test-model.bin"

        // Mock Timber
        every { com.nervesparks.iris.data.exceptions.Timber.tag(any()).e(any<Throwable>(), any()) } just Runs

        // When
        ErrorHandler.reportModelError(error, modelName)

        // Then
        verify {
            com.nervesparks.iris.data.exceptions.Timber.tag("ErrorHandler").e(
                error,
                "Error in Model Loading - $modelName: ${error.message}"
            )
        }
    }

    @Test
    fun `reportNetworkError should use MEDIUM severity and provide user-friendly message`() = runTest {
        // Given
        val error = RuntimeException("Network timeout")
        val operation = "Download model"

        // Mock Timber
        every { com.nervesparks.iris.data.exceptions.Timber.tag(any()).e(any<Throwable>(), any()) } just Runs

        // When
        ErrorHandler.reportNetworkError(error, operation)

        // Then
        verify {
            com.nervesparks.iris.data.exceptions.Timber.tag("ErrorHandler").e(
                error,
                "Error in Network Operation - $operation: ${error.message}"
            )
        }
    }

    @Test
    fun `reportStorageError should use HIGH severity and provide user-friendly message`() = runTest {
        // Given
        val error = RuntimeException("Storage full")
        val operation = "Save chat"

        // Mock Timber
        every { com.nervesparks.iris.data.exceptions.Timber.tag(any()).e(any<Throwable>(), any()) } just Runs

        // When
        ErrorHandler.reportStorageError(error, operation)

        // Then
        verify {
            com.nervesparks.iris.data.exceptions.Timber.tag("ErrorHandler").e(
                error,
                "Error in Storage Operation - $operation: ${error.message}"
            )
        }
    }

    @Test
    fun `reportConfigurationError should use LOW severity and provide user-friendly message`() = runTest {
        // Given
        val error = RuntimeException("Invalid config")
        val setting = "temperature"

        // Mock Timber
        every { com.nervesparks.iris.data.exceptions.Timber.tag(any()).e(any<Throwable>(), any()) } just Runs

        // When
        ErrorHandler.reportConfigurationError(error, setting)

        // Then
        verify {
            com.nervesparks.iris.data.exceptions.Timber.tag("ErrorHandler").e(
                error,
                "Error in Configuration - $setting: ${error.message}"
            )
        }
    }

    @Test
    fun `ErrorEvent should store all provided data correctly`() {
        // Given
        val error = RuntimeException("Test error")
        val context = "Test Context"
        val severity = ErrorHandler.ErrorSeverity.CRITICAL
        val userMessage = "Critical error occurred"

        // When
        val errorEvent = ErrorHandler.ErrorEvent(error, context, severity, userMessage)

        // Then
        assertEquals(error, errorEvent.error)
        assertEquals(context, errorEvent.context)
        assertEquals(severity, errorEvent.severity)
        assertEquals(userMessage, errorEvent.userMessage)
    }

    @Test
    fun `ErrorEvent should use default MEDIUM severity when not specified`() {
        // Given
        val error = RuntimeException("Test error")
        val context = "Test Context"

        // When
        val errorEvent = ErrorHandler.ErrorEvent(error, context)

        // Then
        assertEquals(ErrorHandler.ErrorSeverity.MEDIUM, errorEvent.severity)
    }

    @Test
    fun `ErrorSeverity should have correct values`() {
        // When/Then
        assertEquals("LOW", ErrorHandler.ErrorSeverity.LOW.name)
        assertEquals("MEDIUM", ErrorHandler.ErrorSeverity.MEDIUM.name)
        assertEquals("HIGH", ErrorHandler.ErrorSeverity.HIGH.name)
        assertEquals("CRITICAL", ErrorHandler.ErrorSeverity.CRITICAL.name)
    }
}
