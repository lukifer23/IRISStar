package com.nervesparks.iris.security

import android.util.Patterns
import java.util.regex.Pattern

/**
 * PHASE 2.4: Input Validation - Sanitize and validate user inputs
 * Provides comprehensive input validation and sanitization
 */
object InputValidator {

    // Regular expressions for validation
    private val alphaNumericPattern = Pattern.compile("^[a-zA-Z0-9_\\-\\.]+$")
    private val safeTextPattern = Pattern.compile("^[a-zA-Z0-9\\s_\\-\\.\\,\\!\\?\\:\\;\\'\\\"\\(\\)\\[\\]\\{\\}]+$")
    private val apiKeyPattern = Pattern.compile("^[a-zA-Z0-9_\\-]{20,}$")
    private val urlPattern = Pattern.compile("^(https?://)?[a-zA-Z0-9\\-\\.]+\\.[a-zA-Z]{2,}(/.*)?$")

    /**
     * Validates API keys/tokens
     */
    fun isValidApiKey(apiKey: String): Boolean {
        return apiKey.isNotBlank() &&
               apiKey.length >= 20 &&
               apiKeyPattern.matcher(apiKey).matches()
    }

    /**
     * Validates HuggingFace tokens (typically start with 'hf_')
     */
    fun isValidHuggingFaceToken(token: String): Boolean {
        return token.isNotBlank() &&
               token.startsWith("hf_") &&
               token.length >= 40 &&
               alphaNumericPattern.matcher(token.substring(3)).matches()
    }

    /**
     * Validates usernames
     */
    fun isValidUsername(username: String): Boolean {
        return username.isNotBlank() &&
               username.length in 3..50 &&
               alphaNumericPattern.matcher(username).matches()
    }

    /**
     * Validates URLs
     */
    fun isValidUrl(url: String): Boolean {
        return url.isNotBlank() && urlPattern.matcher(url).matches()
    }

    /**
     * Validates email addresses
     */
    fun isValidEmail(email: String): Boolean {
        return email.isNotBlank() && Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    /**
     * Sanitizes text input to prevent injection attacks
     */
    fun sanitizeTextInput(text: String): String {
        return text
            .replace(Regex("[<>\"'&]"), "") // Remove HTML/XML injection chars
            .replace(Regex("[\\x00-\\x1F\\x7F-\\x9F]"), "") // Remove control characters
            .trim()
            .take(10000) // Limit length to prevent DoS
    }

    /**
     * Sanitizes model names and file paths
     */
    fun sanitizeModelName(name: String): String {
        return name
            .replace(Regex("[^a-zA-Z0-9_\\-\\.\\s]"), "") // Only allow safe characters
            .trim()
            .take(255) // Reasonable length limit
    }

    /**
     * Validates model configuration values
     */
    fun isValidTemperature(temp: Float): Boolean {
        return temp in 0.0f..2.0f
    }

    fun isValidTopP(topP: Float): Boolean {
        return topP in 0.0f..1.0f
    }

    fun isValidTopK(topK: Int): Boolean {
        return topK in 1..1000
    }

    fun isValidMaxTokens(tokens: Int): Boolean {
        return tokens in 1..32768
    }

    fun isValidContextLength(length: Int): Boolean {
        return length in 512..131072
    }

    /**
     * Validates file paths to prevent directory traversal
     */
    fun isValidFilePath(path: String): Boolean {
        return !path.contains("..") &&
               !path.contains("/") &&
               !path.contains("\\") &&
               path.length <= 255
    }

    /**
     * Comprehensive input validation result
     */
    data class ValidationResult(
        val isValid: Boolean,
        val errorMessage: String? = null,
        val sanitizedValue: String? = null
    )

    /**
     * Validates and sanitizes API key input
     */
    fun validateApiKey(apiKey: String): ValidationResult {
        val sanitized = sanitizeTextInput(apiKey)
        return when {
            sanitized.isBlank() ->
                ValidationResult(false, "API key cannot be empty")
            !isValidApiKey(sanitized) ->
                ValidationResult(false, "Invalid API key format")
            else ->
                ValidationResult(true, sanitizedValue = sanitized)
        }
    }

    /**
     * Validates and sanitizes HuggingFace token input
     */
    fun validateHuggingFaceToken(token: String): ValidationResult {
        val sanitized = sanitizeTextInput(token)
        return when {
            sanitized.isBlank() ->
                ValidationResult(false, "Token cannot be empty")
            !isValidHuggingFaceToken(sanitized) ->
                ValidationResult(false, "Invalid HuggingFace token format")
            else ->
                ValidationResult(true, sanitizedValue = sanitized)
        }
    }

    /**
     * Validates and sanitizes username input
     */
    fun validateUsername(username: String): ValidationResult {
        val sanitized = sanitizeTextInput(username)
        return when {
            sanitized.isBlank() ->
                ValidationResult(false, "Username cannot be empty")
            !isValidUsername(sanitized) ->
                ValidationResult(false, "Username must be 3-50 alphanumeric characters")
            else ->
                ValidationResult(true, sanitizedValue = sanitized)
        }
    }
}
