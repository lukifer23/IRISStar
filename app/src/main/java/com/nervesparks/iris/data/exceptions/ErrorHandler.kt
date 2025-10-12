package com.nervesparks.iris.data.exceptions

import timber.log.Timber
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Centralized error handling and reporting system for the IRIS Star application.
 *
 * This singleton provides a unified approach to error handling across all components,
 * ensuring consistent logging, user feedback, and error reporting. All errors are
 * logged with appropriate context and severity levels, and critical errors are
 * reported to the UI for user notification.
 */
object ErrorHandler {

    private val _errorFlow = MutableSharedFlow<ErrorEvent>(replay = 0)
    val errorFlow: SharedFlow<ErrorEvent> = _errorFlow.asSharedFlow()

    /**
     * Represents an error event with context and severity information.
     *
     * @property error The original exception or throwable
     * @property context The context where the error occurred (e.g., "Model Loading", "Network Request")
     * @property severity The severity level of the error for prioritization
     * @property userMessage Optional user-friendly message to display instead of the technical error
     */
    data class ErrorEvent(
        val error: Throwable,
        val context: String,
        val severity: ErrorSeverity = ErrorSeverity.MEDIUM,
        val userMessage: String? = null
    )

    /**
     * Severity levels for error classification and handling priority.
     */
    enum class ErrorSeverity {
        /** Minor issues that don't affect functionality */
        LOW,
        /** Issues that affect some features but app still works */
        MEDIUM,
        /** Critical issues that prevent core functionality */
        HIGH,
        /** App-breaking issues that require immediate attention */
        CRITICAL
    }

    /**
     * Report an error with context and severity
     */
    fun reportError(
        error: Throwable,
        context: String,
        severity: ErrorSeverity = ErrorSeverity.MEDIUM,
        userMessage: String? = null
    ) {
        val errorEvent = ErrorEvent(error, context, severity, userMessage)
        Timber.tag("ErrorHandler").e(error, "Error in $context: ${error.message}")

        // Emit to flow for UI handling synchronously to avoid orphaned jobs
        _errorFlow.tryEmit(errorEvent)
    }

    /**
     * Report a model loading error
     */
    fun reportModelError(error: Throwable, modelName: String) {
        reportError(
            error = error,
            context = "Model Loading - $modelName",
            severity = ErrorSeverity.HIGH,
            userMessage = "Failed to load model $modelName. Please try again or select a different model."
        )
    }

    /**
     * Report a network error
     */
    fun reportNetworkError(error: Throwable, operation: String) {
        reportError(
            error = error,
            context = "Network Operation - $operation",
            severity = ErrorSeverity.MEDIUM,
            userMessage = "Network error during $operation. Please check your connection and try again."
        )
    }

    /**
     * Report a storage error
     */
    fun reportStorageError(error: Throwable, operation: String) {
        reportError(
            error = error,
            context = "Storage Operation - $operation",
            severity = ErrorSeverity.HIGH,
            userMessage = "Storage error during $operation. Please ensure you have sufficient storage space."
        )
    }

    /**
     * Report a configuration error
     */
    fun reportConfigurationError(error: Throwable, setting: String) {
        reportError(
            error = error,
            context = "Configuration - $setting",
            severity = ErrorSeverity.LOW,
            userMessage = "Configuration error for $setting. Using default values."
        )
    }
}
